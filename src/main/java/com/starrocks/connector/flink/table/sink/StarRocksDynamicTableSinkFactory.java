/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.starrocks.connector.flink.table.sink;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.factories.DynamicTableSinkFactory;
import org.apache.flink.table.factories.FactoryUtil;
import org.apache.flink.table.utils.TableSchemaUtils;

import java.util.HashSet;
import java.util.Set;

import static com.starrocks.connector.flink.table.StarRocksOptions.DATABASE_NAME;
import static com.starrocks.connector.flink.table.StarRocksOptions.FE_NODES;
import static com.starrocks.connector.flink.table.StarRocksOptions.IDENTIFIER;
import static com.starrocks.connector.flink.table.StarRocksOptions.JDBC_URL;
import static com.starrocks.connector.flink.table.StarRocksOptions.PASSWORD;
import static com.starrocks.connector.flink.table.StarRocksOptions.TABLE_NAME;
import static com.starrocks.connector.flink.table.StarRocksOptions.USERNAME;

public class StarRocksDynamicTableSinkFactory implements DynamicTableSinkFactory {

    @Override
    public DynamicTableSink createDynamicTableSink(Context context) {
        final FactoryUtil.TableFactoryHelper helper = FactoryUtil.createTableFactoryHelper(this, context);
        helper.validateExcept(StarRocksSinkOptions.SINK_PROPERTIES_PREFIX);
        ReadableConfig options = helper.getOptions();
        // validate some special properties
        StarRocksSinkOptions sinkOptions = new StarRocksSinkOptions(options, context.getCatalogTable().getOptions());
        TableSchema physicalSchema = TableSchemaUtils.getPhysicalSchema(context.getCatalogTable().getSchema());
        return new StarRocksDynamicTableSink(sinkOptions, physicalSchema);
    }

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        Set<ConfigOption<?>> requiredOptions = new HashSet<>();
        requiredOptions.add(JDBC_URL);
        requiredOptions.add(FE_NODES);
        requiredOptions.add(DATABASE_NAME);
        requiredOptions.add(TABLE_NAME);
        requiredOptions.add(USERNAME);
        requiredOptions.add(PASSWORD);
        return requiredOptions;
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        Set<ConfigOption<?>> optionalOptions = new HashSet<>();
        optionalOptions.add(StarRocksSinkOptions.SINK_VERSION);
        optionalOptions.add(StarRocksSinkOptions.SINK_BATCH_MAX_SIZE);
        optionalOptions.add(StarRocksSinkOptions.SINK_BATCH_MAX_ROWS);
        optionalOptions.add(StarRocksSinkOptions.SINK_BATCH_FLUSH_INTERVAL);
        optionalOptions.add(StarRocksSinkOptions.SINK_MAX_RETRIES);
        optionalOptions.add(StarRocksSinkOptions.SINK_SEMANTIC);
        optionalOptions.add(StarRocksSinkOptions.SINK_BATCH_OFFER_TIMEOUT);
        optionalOptions.add(StarRocksSinkOptions.SINK_PARALLELISM);
        optionalOptions.add(StarRocksSinkOptions.SINK_LABEL_PREFIX);
        optionalOptions.add(StarRocksSinkOptions.SINK_CONNECT_TIMEOUT);
        optionalOptions.add(StarRocksSinkOptions.SINK_IO_THREAD_COUNT);
        optionalOptions.add(StarRocksSinkOptions.SINK_CHUNK_LIMIT);
        optionalOptions.add(StarRocksSinkOptions.SINK_SCAN_FREQUENCY);
        return optionalOptions;
    }
}
