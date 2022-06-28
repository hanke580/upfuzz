package org.zlab.upfuzz.docker;

import java.io.File;

public interface DockerBuilder {
    String getHostIP();

    String originalClusterIP();

    String upgradedClusterIP();

    int start();

    void teardown();

    boolean buildDocker();
}
