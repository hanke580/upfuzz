package org.zlab.upfuzz.docker;

import java.io.File;
import java.nio.file.Path;

public interface DockerBuilder {
    String getHostIP();

    String originalClusterIP();

    int start();

    void teardown();

    boolean buildDocker();

    Path getDataPath();
}
