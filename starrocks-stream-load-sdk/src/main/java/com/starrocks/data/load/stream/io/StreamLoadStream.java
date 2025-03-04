package com.starrocks.data.load.stream.io;

import com.starrocks.data.load.stream.StreamLoadDataFormat;
import com.starrocks.data.load.stream.TableRegion;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class StreamLoadStream extends InputStream {

    private static final int DEFAULT_BUFFER_SIZE = 2048;

    private final TableRegion region;
    private final StreamLoadDataFormat dataFormat;

    private ByteBuffer buffer;
    private byte[] cache;
    private int pos;
    private boolean endStream = false;

    public StreamLoadStream(TableRegion region,
                            StreamLoadDataFormat dataFormat) {
        this.region = region;
        this.dataFormat = dataFormat;

        buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
        buffer.position(buffer.capacity());
    }


    @Override
    public int read() throws IOException {
        byte[] bytes = new byte[1];
        int ws = read(bytes);
        if (ws == -1) {
            return -1;
        }
        return bytes[0];
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        }

        if (!buffer.hasRemaining()) {
            if (cache == null && endStream) {
                return -1;
            }
            fillBuffer();
        }

        int size = len - off;
        int ws = Math.min(size, buffer.remaining());

        for (int pos = off; pos < off + ws; pos++) {
            b[pos] = buffer.get();
        }

        return ws;
    }

    @Override
    public void close() throws IOException {
        buffer = null;
        cache = null;
        pos = 0;
        endStream = true;
    }

    private void fillBuffer() {
        buffer.clear();
        if (cache != null) {
            writeBuffer(cache, pos);
        }

        if (endStream || !buffer.hasRemaining()) {
            buffer.flip();
            return;
        }

        byte[] bytes;
        while ((bytes = readRegion()) != null) {
            writeBuffer(bytes, 0);
            bytes = null;
            if (!buffer.hasRemaining()) {
                break;
            }
        }
        if (buffer.position() == 0) {
            buffer.position(buffer.limit());
        } else {
            buffer.flip();
        }
    }

    private void writeBuffer(byte[] bytes, int pos) {
        int size = bytes.length - pos;

        int remain = buffer.remaining();

        int ws = Math.min(size, remain);
        buffer.put(bytes, pos, ws);
        if (size > remain) {
            this.cache = bytes;
            this.pos = pos + ws;
        } else {
            this.cache = null;
            this.pos = 0;
        }
    }

    private static final int DATA_FIRST = 1;
    private static final int DATA_BODY = 2;
    private static final int DATA_END = 3;

    private int state = DATA_FIRST;

    private byte[] next;
    private boolean first = true;

    private byte[] readRegion() {
        switch (state) {
            case DATA_FIRST:
                state = DATA_BODY;
                if (dataFormat.first() != null && dataFormat.first().length > 0) {
                    return dataFormat.first();
                } else {
                    return readRegion();
                }
            case DATA_BODY:
                byte[] body;
                if (next != null) {
                    body = next;
                    next = null;
                    return body;
                }

                body = region.read();
                if (body == null) {
                    state = DATA_END;
                    return null;
                }
                if (!first) {
                    next = body;
                    body = dataFormat.delimiter();
                } else {
                    first = false;
                }
                return body;
            case DATA_END:
                if (endStream) {
                    return null;
                }
                endStream = true;
                return dataFormat.end();
            default:
                return null;
        }
    }
}
