package org.zlab.upfuzz.cassandra;

import org.zlab.upfuzz.fuzzingengine.configgen.ConfigGen;
import org.zlab.upfuzz.fuzzingengine.configgen.PlainTextGenerator;
import org.zlab.upfuzz.fuzzingengine.configgen.yaml.YamlGenerator;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class CassandraConfigGen extends ConfigGen {

    public static final String[] cassandraConfigBlacklist = {
            "minimum_replication_factor_warn_threshold",
            "minimum_replication_factor_fail_threshold",
            "user_defined_functions_threads_enabled",
            "concurrent_validations",
            "enable_user_defined_functions_threads",
            // timeout related, better not mutate be mutated
            "request_timeout_in_ms",
            "read_request_timeout_in_ms",
            "range_request_timeout_in_ms",
            "write_request_timeout_in_ms",
            "counter_write_request_timeout_in_ms",
            "cas_contention_timeout_in_ms",
            // setting this to 1 prevents system from starting up
            "truncate_request_timeout_in_ms",
            // new feature: usually not enabled during upgrade
            "partition_denylist_enabled",
            // Encryption must be enabled in client_encryption_options for
            // native_transport_port_ssl
            "native_transport_port_ssl",
            "native_transport_max_negotiable_version"
    };

    @Override
    public void updateConfigBlackList() {
        // add all configs in cassandraConfigBlacklist to configBlackList
        configBlackList.addAll(Arrays.asList(cassandraConfigBlacklist));
    }

    @Override
    public void initUpgradeFileGenerator() {
        Path defaultConfigPath = Paths.get(oldVersionPath.toString(),
                "conf/cassandra.yaml");
        Path defaultNewConfigPath = Paths.get(newVersionPath.toString(),
                "conf/cassandra.yaml");
        configFileGenerator = new YamlGenerator[1];
        configFileGenerator[0] = new YamlGenerator(defaultConfigPath,
                defaultNewConfigPath, generateFolderPath);
        extraGenerator = new PlainTextGenerator[0];
    }

    @Override
    public void initSingleFileGenerator() {
        Path defaultConfigPath = Paths.get(oldVersionPath.toString(),
                "conf/cassandra.yaml");
        configFileGenerator = new YamlGenerator[1];
        configFileGenerator[0] = new YamlGenerator(defaultConfigPath,
                generateFolderPath);
        extraGenerator = new PlainTextGenerator[0];
    }

    @Override
    public void initUpgradeValGenerator() {
        // specialize ConfigValGenerator for Cassandra
        boundaryConfigValGenerator = new CassandraConfigValGenerator(
                boundaryConfig, oriConfigInfo);
        commonConfigValGenerator = new CassandraConfigValGenerator(commonConfig,
                oriConfigInfo);
        addedConfigValGenerator = new CassandraConfigValGenerator(addedConfig,
                upConfigInfo);
        deletedConfigValGenerator = new CassandraConfigValGenerator(
                deletedConfig,
                oriConfigInfo);
        remainOriConfigValGenerator = new CassandraConfigValGenerator(
                remainOriConfig, oriConfigInfo);
        remainUpConfigValGenerator = new CassandraConfigValGenerator(
                remainUpConfig, upConfigInfo);
    }

    @Override
    public void initSingleValGenerator() {
        // specialize ConfigValGenerator for Cassandra
        singleConfigValGenerator = new CassandraConfigValGenerator(singleConfig,
                oriConfigInfo);
    }
}
