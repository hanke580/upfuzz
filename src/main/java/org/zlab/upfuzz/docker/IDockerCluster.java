package org.zlab.upfuzz.docker;

import java.io.File;
import java.nio.file.Path;

public interface IDockerCluster {
    String getNetworkIP();

    int start() throws Exception;

    void teardown();

    boolean build() throws Exception;

    void prepareUpgrade() throws Exception;

    boolean fullStopUpgrade() throws Exception;

    boolean rollingUpgrade() throws Exception;

    boolean downgrade() throws Exception;

    void flush() throws Exception;

    boolean freshStartNewVersion() throws Exception;

    void upgrade(int nodeIndex) throws Exception;

    void downgrade(int nodeIndex) throws Exception;

    IDocker getDocker(int i);
}
