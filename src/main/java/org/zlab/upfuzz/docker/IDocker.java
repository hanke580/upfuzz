package org.zlab.upfuzz.docker;

import org.zlab.upfuzz.fuzzingengine.LogInfo;

import java.nio.file.Path;
import java.util.Map;

public interface IDocker {
    String getNetworkIP();

    int start() throws Exception;

    void teardown();

    boolean build() throws Exception;

    void upgrade() throws Exception;

    void downgrade() throws Exception;

    // Stop the process in container
    // Don't stop the container
    boolean shutdown();

    // remove all system data (data/ in cassandra)
    boolean clear();

    Map<String, String> readSystemState();

    LogInfo readLogInfo();

    Path getDataPath();

    String formatComposeYaml();
}
