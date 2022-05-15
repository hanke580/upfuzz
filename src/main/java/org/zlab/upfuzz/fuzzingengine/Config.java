package org.zlab.upfuzz.fuzzingengine;


import java.lang.reflect.Field;
import java.util.Arrays;

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
        public String upgradeCassandraPath = null;
        public String jacocoAgentPath = null;
        public String cassandraOutputFile = null;
        public String initSeedDir = null;
        public String crashDir = null;
        public String corpusDir = null;

        @Override
        public String toString() {
            return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(this,
                    Configuration.class);
        }

        public Boolean checkNull() {
            Field[] fields = this.getClass().getDeclaredFields();
            for(Field field : fields){
                try {
                    Object fieldObject = field.get(this);
                    if( fieldObject == null ){
                        System.err.println("Configuration failed to find: " + field);
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }

            // assertTrue(Arrays.stream(fields).anyMatch(
            //         field -> field.getName().equals(LAST_NAME_FIELD) && field.getType().equals(String.class)));
            return true;
        }
    }

    public static void setInstance(Configuration config) {
        instance = config;
    }
}
