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

        /**
         * Type1: warn_threshold -> fail_threshold
         * Type2: tombstone_warn_threshold -> tombstone_failure_threshold
         * Type3: _warn_timeout -> _
         *      user_defined_functions_fail_timeout -> user_defined_functions_warn_timeout
         */
        Set<String> configs = new HashSet<>();
        for (String config : this.configs) {
            if (config.contains("fail_threshold")
                    || config.contains("failure_threshold")
                    || config.contains("_fail_timeout"))
                continue;

            if (config.contains("warn_threshold")) {
                // Type1
                String pConfig = config.replace("warn_threshold",
                        "fail_threshold");
                if (this.configs.contains(pConfig)) {
                    pairConfigs.put(config, pConfig);
                    continue;
                } else {
                    // Try Type2
                    pConfig = config.replace("warn_threshold",
                            "failure_threshold");
                    if (this.configs.contains(pConfig)) {
                        pairConfigs.put(config, pConfig);
                    }
                    continue;
                }
            } else if (config.contains("_warn_timeout")) {
                // Type3
                String pConfig = config.replace("_warn_timeout",
                        "_fail_timeout");
                if (this.configs.contains(pConfig)) {
                    pairConfigs.put(config, pConfig);
                }
                continue;
            }
            configs.add(config);
        }
        this.configs = configs;
    }
}
