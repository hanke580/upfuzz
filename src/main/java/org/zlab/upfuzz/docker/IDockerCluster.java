package org.zlab.upfuzz.docker;

import java.io.File;
import java.nio.file.Path;

public interface IDockerCluster {
    String getNetworkIP();

    int start();

    void teardown();

    boolean build();

    Path getDataPath();

    IDocker getDocker(int i);
}
