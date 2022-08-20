package org.zlab.upfuzz.docker;

public abstract class DockerCluster implements IDockerCluster {

    static public String getKthIP(String ip, int index) {
        String[] segments = ip.split("\\.");
        segments[3] = Integer.toString(index + 2);
        return String.join(".", segments);
    }
}
