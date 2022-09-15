package org.zlab.upfuzz.docker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.utils.Utilities;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public abstract class DockerMeta {

    static Logger logger = LogManager.getLogger(DockerMeta.class);

    public File workdir;
    public String[] env;
    public String name;
    public String type;
    public String system;
    public String originalVersion;
    public String upgradedVersion;
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
    public String containerName;

    public Process runInContainer(String[] cmd, String[] env)
            throws IOException, InterruptedException {
        StringBuilder sb = new StringBuilder();
        ArrayList<String> cmds = new ArrayList<>();
        cmds.add("docker");
        cmds.add("exec");
        for (int i = 0; i < env.length; ++i) {
            cmds.add("-e");
            cmds.add(env[i]);
        }
        cmds.add(containerName);
        cmds.addAll(Arrays.asList(cmd));
        logger.debug(String.join(" ", cmds));
        return Utilities.exec(cmds.toArray(new String[] {}), workdir);
    }

    public Process runInContainer(String[] cmd)
            throws IOException {
        String[] dockerCMD = Utilities.concatArray(
                new String[] { "docker", "exec", containerName }, cmd);
        logger.debug(String.join(" ", dockerCMD));
        return Utilities.exec(dockerCMD, workdir);
    }

    public Process runInContainerWithPrivilege(String[] cmd)
            throws IOException {
        String[] dockerCMD = Utilities.concatArray(
                new String[] { "docker", "exec", "--privileged",
                        containerName },
                cmd);
        logger.debug(String.join(" ", dockerCMD));
        return Utilities.exec(dockerCMD, workdir);
    }

    public abstract String formatComposeYaml();

}
