package org.zlab.upfuzz.docker;

import org.zlab.ocov.tracker.ObjectCoverage;
import org.zlab.upfuzz.fuzzingengine.LogInfo;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public interface IDocker {
    String getNetworkIP();

    int start() throws Exception;

    void teardown();

    boolean build() throws Exception;

    void flush() throws Exception;

    void shutdown() throws Exception;

    void upgrade() throws Exception;

    void upgradeFromCrash() throws Exception;

    void downgrade() throws Exception;

    ObjectCoverage getFormatCoverage() throws Exception;

    // remove all system data (data/ in cassandra)
    boolean clear();

    Map<String, String> readSystemState();

    LogInfo grepLogInfo(Set<String> blackListErrorLog);

    Path getDataPath();

    String formatComposeYaml();
}
