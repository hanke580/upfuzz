package org.zlab.upfuzz.fuzzingengine;

public class Config {
    public String serverHost = "localhost";
    public int serverPort = 6299;
    public String clientHost = "localhost";
    public int clientPort = 6300;
    public String sessionID = null;
//    public static String cassandraPath = "/home/yayu/Project/Upgrade-Fuzzing/cassandra/cassandra";
//    public static String jacocoAgentPath = "/home/yayu/Project/Upgrade-Fuzzing/upfuzz/dependencies/org.jacoco.agent-300f3f6d2b-runtime.jar";
    public static String cassandraPath = "/home/vagrant/project/cassandra-2.2.7";
    public static String upgradeCassandraPath = "/home/vagrant/project/cassandra-3.0.15";

    public static String jacocoAgentPath = "/home/vagrant/project/upfuzz/dependencies/org.jacoco.agent-300f3f6d2b-runtime.jar";
}
