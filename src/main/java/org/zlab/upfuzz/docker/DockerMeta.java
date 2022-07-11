package org.zlab.upfuzz.docker;

import java.io.File;

public abstract class DockerMeta {
    public File workdir;
    public String type;
    public String system;
    public String version;
    public String networkName;
    public String subnet;
    public String networkIP;
    public String hostIP;
    public String seedIP;
    public int agentPort;
    public String includes;
    public String excludes;
    public int index;
    public String executorID;
}
