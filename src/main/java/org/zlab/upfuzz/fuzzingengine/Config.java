package org.zlab.upfuzz.fuzzingengine;

import com.google.gson.GsonBuilder;

public class Config {
    public static Configuration instance;

    public static Configuration getConf() {
        return instance;
    }

    public Config() {
        instance = new Configuration();
    }

    public static class Configuration {
        public String serverHost = "localhost";
        public Integer serverPort = 6299;
        public String clientHost = "localhost";
        public Integer clientPort = 6300;
        public String cassandraPath = null;
        public String jacocoAgentPath = null;
        public String cqlshDaemonScript = null;
        public String cassandraOutputFile = null;

        @Override
        public String toString() {
            return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(this,
                    Configuration.class);
            // return "Configuration [cassandraPath=" + cassandraPath + ", clientHost=" + clientHost + ", clientPort="
            //         + clientPort + ", jacocoAgentPath=" + jacocoAgentPath + ", serverHost=" + serverHost
            //         + ", serverPort=" + serverPort + "]";
        }
    }

    public static void setInstance(Configuration config) {
        instance = config;
    }
}
