package org.zlab.upfuzz.docker;

import java.io.File;
import java.nio.file.Path;

public interface IDockerCluster {
    String getNetworkIP();

    int start() throws Exception;

    void teardown();

    boolean build() throws Exception;

    Path getDataPath();

    void upgrade() throws Exception;

    IDocker getDocker(int i);
}
