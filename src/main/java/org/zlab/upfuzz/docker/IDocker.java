package org.zlab.upfuzz.docker;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

public interface IDocker {
    String getNetworkIP();

    int start() throws Exception;

    void teardown();

    boolean build() throws Exception;

    void upgrade() throws Exception;

    Map<String, String> readSystemState();

    Path getDataPath();

    String formatComposeYaml();
}
