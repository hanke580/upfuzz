package org.zlab.upfuzz.docker;

import java.io.File;
import java.nio.file.Path;

public interface IDocker {
    String getNetworkIP();

    int start() throws Exception;

    void teardown();

    boolean build() throws Exception;

    void upgrade() throws Exception;

    Path getDataPath();

    String formatComposeYaml();
}
