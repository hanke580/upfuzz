package org.zlab.upfuzz.cassandra;

import org.zlab.upfuzz.fuzzingengine.configgen.ConfigInfo;
import org.zlab.upfuzz.fuzzingengine.configgen.ConfigValGenerator;

import java.util.HashSet;
import java.util.Set;

public class CassandraConfigValGenerator extends ConfigValGenerator {
    public CassandraConfigValGenerator(Set<String> configs,
            ConfigInfo configInfo) {
        super(configs, configInfo);
    }

    @Override
    public void constructPairs() {
        // support int type configuration with smaller relation
        // only for cassandra
        Set<String> configs = new HashSet<>();
        for (String config : this.configs) {
            if (config.contains("warn_threshold")) {
                String pConfig = config.replace("warn_threshold",
                        "fail_threshold");
                if (this.configs.contains(pConfig)) {
                    pairConfigs.put(config, pConfig);
                } else {
                    configs.add(config);
                }
            } else {
                configs.add(config);
            }
        }
        this.configs = configs;
    }
}
