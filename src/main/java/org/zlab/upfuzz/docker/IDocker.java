package org.zlab.upfuzz.docker;

import java.io.File;
import java.nio.file.Path;

public interface IDocker {
    String getNetworkIP();

    int start();

    void teardown();

    boolean build();

    Path getDataPath();

    String formatComposeYaml();
}
