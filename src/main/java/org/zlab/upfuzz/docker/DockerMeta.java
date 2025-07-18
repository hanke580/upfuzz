package org.zlab.upfuzz.docker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.LogInfo;
import org.zlab.upfuzz.fuzzingengine.ShellDaemon;
import org.zlab.upfuzz.utils.Utilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
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
        original, upgraded
    }

    public enum ConfigType {
        oriconfig, upconfig
    }

    public ShellDaemon shell;

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

    public boolean collectFormatCoverage;

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

    public Process copyToContainer(String filename, String destinationDirectory)
            throws IOException {
        ArrayList<String> cmds = new ArrayList<>();
        cmds.add("docker");
        cmds.add("cp");
        cmds.add(filename);
        String copyDirectory = containerName
                + ":" + destinationDirectory;
        cmds.add(copyDirectory);

        ProcessBuilder pb = new ProcessBuilder(cmds).redirectErrorStream(true);

        if (Config.getConf().debug) {
            for (String cmd : cmds) {
                logger.info("exec cmd: " + cmd);
            }
        }
        return pb.start();
    }

    public Process updateFileInContainer(String filename,
            String modificationCommand) throws IOException {
        ArrayList<String> cmds = new ArrayList<>();
        cmds.add("docker");
        cmds.add("exec");
        cmds.add(containerName);

        cmds.add("sh");
        cmds.add("-c");

        // Construct the complete sed command with the filename at the end
        String fullCommand = modificationCommand + " " + filename;
        cmds.add(fullCommand);

        ProcessBuilder pb = new ProcessBuilder(cmds).redirectErrorStream(true);

        if (Config.getConf().debug) {
            for (String cmd : cmds) {
                logger.info("exec cmd: " + cmd);
            }
        }
        return pb.start();
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
        // if (Config.getConf().debug)
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
    public boolean copyConfig(Path configPath, int direction)
            throws IOException {
        if (Config.getConf().testSingleVersion) {
            setSingleConfig(ConfigType.oriconfig);
        } else {
            setSingleConfig(ConfigType.oriconfig);
            setSingleConfig(ConfigType.upconfig);
        }
        return true;
    }

    public void setSingleConfig(ConfigType configType) throws IOException {
        Path concreteConfigPath = configPath.resolve(configType.toString());

        assert concreteConfigPath.toFile().isDirectory();

        Path concreteConfigPathDocker = Paths.get(workdir.getPath(),
                "/persistent/config/" + configType.toString());

        concreteConfigPathDocker.toFile().mkdirs();

        for (File file : concreteConfigPath.toFile().listFiles()) {
            Files.copy(file.toPath(),
                    concreteConfigPathDocker.resolve(file.getName()),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public String[] constructGrepCommand(Path filePath, String target,
            int grepLineNum) {
        return new String[] {
                "/bin/sh", "-c",
                "grep -ah -A " + grepLineNum + " \"" + target + "\" " + filePath
        };
    }

    public static boolean isBlackListed(String errorMsg,
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
