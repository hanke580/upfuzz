package org.zlab.upfuzz.docker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.LogInfo;
import org.zlab.upfuzz.utils.Utilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public abstract class DockerMeta {
    static Logger logger = LogManager.getLogger(DockerMeta.class);

    public static class DockerState {
        public DockerVersion dockerVersion;
        public boolean alive;

        public DockerState(DockerVersion dockerVersion, boolean alive) {
            this.dockerVersion = dockerVersion;
            this.alive = alive;
        }
    }

    public enum DockerVersion {
        upgraded, original
    }

    public File workdir;
    public String[] env;
    public String containerName;
    public String type;
    public String system;
    public String originalVersion;
    public String upgradedVersion;
    public String networkName;
    public String subnet;
    public String networkIP;
    public String hostIP;
    public int agentPort;
    public String includes;
    public String excludes;
    public int index;
    public String executorID;
    public String serviceName;
    public Path configPath;

    public Set<String> targetSystemStates;

    public Process runInContainer(String[] cmd, String[] env)
            throws IOException {
        ArrayList<String> cmds = new ArrayList<>();
        cmds.add("docker");
        cmds.add("exec");
        for (String s : env) {
            cmds.add("-e");
            cmds.add(s);
        }
        cmds.add(containerName);
        cmds.addAll(Arrays.asList(cmd));
        if (Config.getConf().debug)
            logger.debug(String.join(" ", cmds));
        return Utilities.exec(cmds.toArray(new String[] {}), workdir);
    }

    public int runProcessInContainer(String[] cmd, String[] env) {
        try {
            Process p = runInContainer(cmd, env);
            p.getInputStream().readAllBytes();
            return p.waitFor();
        } catch (IOException | InterruptedException e) {
            logger.error("process failed in container: " + e);
            return -1;
        }
    }

    public int runProcessInContainer(String[] cmd) {
        try {
            return runInContainer(cmd).waitFor();
        } catch (IOException | InterruptedException e) {
            logger.error("process failed in container: " + e);
            return -1;
        }
    }

    public Process runInContainer(String[] cmd)
            throws IOException {
        String[] dockerCMD = Utilities.concatArray(
                new String[] { "docker", "exec", containerName }, cmd);
        // logger.debug(String.join(" ", dockerCMD));
        return Utilities.exec(dockerCMD, workdir);
    }

    public Process runInContainerWithPrivilege(String[] cmd)
            throws IOException {
        String[] dockerCMD = Utilities.concatArray(
                new String[] { "docker", "exec", "--privileged",
                        containerName },
                cmd);
        // logger.debug(String.join(" ", dockerCMD));
        return Utilities.exec(dockerCMD, workdir);
    }

    // NOTE If copyConfig modified then
    // FuzzingClient NyxMode configCheck may need to be modifed
    public boolean copyConfig(Path configPath) throws IOException {

        Path oriConfigPath = configPath.resolve("oriconfig");
        Path upConfigPath = configPath.resolve("upconfig");

        assert oriConfigPath.toFile().isDirectory();
        assert upConfigPath.toFile().isDirectory();

        Path oriConfigPathDocker = Paths.get(workdir.getPath(),
                "/persistent/config/oriconfig");
        Path upConfigPathDocker = Paths.get(workdir.getPath(),
                "/persistent/config/upconfig");

        oriConfigPathDocker.toFile().mkdirs();
        upConfigPathDocker.toFile().mkdirs();

        for (File file : oriConfigPath.toFile().listFiles()) {
            Files.copy(file.toPath(),
                    oriConfigPathDocker.resolve(file.getName()),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        if (!Config.getConf().testSingleVersion) {
            for (File file : upConfigPath.toFile().listFiles()) {
                Files.copy(file.toPath(),
                        upConfigPathDocker.resolve(file.getName()),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return true;
    }

    public String[] constructGrepCommand(Path filePath, String target,
            int grepLineNum) {
        return new String[] {
                "/bin/sh", "-c",
                "grep -ah -A " + grepLineNum + " \"" + target + "\" " + filePath
        };
    }

    public boolean isBlackListed(String errorMsg,
            Set<String> blackListErrorLog) {
        for (String str : blackListErrorLog) {
            if (errorMsg.contains(str)) {
                return true;
            }
        }
        return false;
    }

    public void constructLogInfo(LogInfo logInfo, Path filePath,
            Set<String> blackListErrorLog) {
        // grep ERROR/WARN from log file
        // ERROR
        String[] cmd = constructGrepCommand(filePath, "ERROR",
                Config.getConf().grepLineNum);
        try {
            Process grepProc = runInContainer(cmd);
            String result = new String(
                    grepProc.getInputStream().readAllBytes());
            if (!result.isEmpty()) {
                for (String msg : result.split("--")) {
                    if (!isBlackListed(msg, blackListErrorLog))
                        logInfo.addErrorMsg(msg);
                }
            }
        } catch (IOException e) {
            logger.error(String.format(
                    "Problem when reading log information in docker[%d]",
                    index));
            e.printStackTrace();
        }

        // WARN
        cmd = constructGrepCommand(filePath, "WARN",
                Config.getConf().grepLineNum);
        try {
            Process grepProc = runInContainer(cmd);
            String result = new String(
                    grepProc.getInputStream().readAllBytes());
            if (!result.isEmpty()) {
                for (String msg : result.split("--")) {
                    logInfo.addWARNMsg(msg);
                }
            }
        } catch (IOException e) {
            logger.error(String.format(
                    "Problem when reading log information in docker[%d]",
                    index));
            e.printStackTrace();
        }
    }

}
