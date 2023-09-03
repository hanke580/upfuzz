package org.zlab.upfuzz.cassandra;

import org.zlab.upfuzz.fuzzingengine.configgen.ConfigGen;
import org.zlab.upfuzz.fuzzingengine.configgen.PlainTextGenerator;
import org.zlab.upfuzz.fuzzingengine.configgen.yaml.YamlGenerator;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CassandraConfigGen extends ConfigGen {

    @Override
    public void updateConfigBlackList() {
        configBlackList
                .add("minimum_replication_factor_warn_threshold");
        configBlackList
                .add("minimum_replication_factor_fail_threshold");
        configBlackList
                .add("user_defined_functions_threads_enabled");
        configBlackList
                .add("concurrent_validations");
    }

    @Override
    public void initFileGenerator() {
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
    public void initValGenerator() {
        // specialize ConfigValGenerator for Cassandra
        commonConfigValGenerator = new CassandraConfigValGenerator(commonConfig,
                oriConfigInfo);
        addedConfigValGenerator = new CassandraConfigValGenerator(addedConfig,
                upConfigInfo);
        deletedConfigValGenerator = new CassandraConfigValGenerator(
                deletedConfig,
                oriConfigInfo);
    }
}
