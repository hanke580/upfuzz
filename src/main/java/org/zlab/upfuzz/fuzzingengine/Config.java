/* (C)2022 */
package org.zlab.upfuzz.fuzzingengine;

import com.google.gson.GsonBuilder;
import java.lang.reflect.Field;

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
        public String oldSystemPath = null;
        public String newSystemPath = null;
        public String jacocoAgentPath = null;
        public String system = null;
        public String dataDir = null;
        // public String logFile = null;
        public String initSeedDir = null;
        public String crashDir = null;
        public String corpusDir = null;

        @Override
        public String toString() {
            return new GsonBuilder()
                    .setPrettyPrinting()
                    .disableHtmlEscaping()
                    .create()
                    .toJson(this, Configuration.class);
        }

        public Boolean checkNull() {
            Field[] fields = this.getClass().getDeclaredFields();
            for (Field field : fields) {
                try {
                    Object fieldObject = field.get(this);
                    if (fieldObject == null) {
                        System.err.println("Configuration failed to find: " + field);
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }

            // assertTrue(Arrays.stream(fields).anyMatch(
            //         field -> field.getName().equals(LAST_NAME_FIELD) &&
            // field.getType().equals(String.class)));
            return true;
        }
    }

    public static void setInstance(Configuration config) {
        instance = config;
    }
}
