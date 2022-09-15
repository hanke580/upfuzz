package org.zlab.upfuzz.docker;

import java.io.IOException;
import java.nio.file.Path;

public abstract class Docker extends DockerMeta implements IDocker {

    public abstract void chmodDir() throws IOException, InterruptedException;
}
