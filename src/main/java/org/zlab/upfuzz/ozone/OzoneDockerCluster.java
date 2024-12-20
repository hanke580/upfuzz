package org.zlab.upfuzz.ozone;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.docker.DockerCluster;
import org.zlab.upfuzz.docker.IDocker;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.utils.Utilities;

public class OzoneDockerCluster extends DockerCluster {
    static Logger logger = LogManager.getLogger(OzoneDockerCluster.class);

    String omNodeIP;

    static final String includes = "org.apache.hadoop.ozone.*";
    static final String excludes = "org.apache.cassandra.*";

    public static String[] includeJacocoHandlers = {
            "org.apache.hadoop.hdds.scm.server.StorageContainerManagerStarter",
            "org.apache.hadoop.ozone.om.OzoneManagerStarter",
            "org.apache.hadoop.ozone.HddsDatanodeService"
            // "org.apache.hadoop.ozone.recon.ReconServer"
    };

    OzoneDockerCluster(OzoneExecutor executor, String version,
            int nodeNum, boolean collectFormatCoverage, Path configPath,
            int direction) {
        super(executor, version, nodeNum, collectFormatCoverage, direction);

        this.dockers = new OzoneDocker[nodeNum];
        this.omNodeIP = DockerCluster.getKthIP(hostIP, 0); // 2 means the
        this.configpath = configPath;

        initBlackListErrorLog();
    }

    public void initBlackListErrorLog() {
        blackListErrorLog.add("Error response from daemon: Container");
        blackListErrorLog.add("RECEIVED SIGNAL");
    }

    public boolean build() throws Exception {
        for (int i = 0; i < dockers.length; ++i) {
            dockers[i] = new OzoneDocker(this, i);
            dockers[i].build();
        }
        return true;
    }

    @Override
    public void refreshNetwork() {
        this.subnetID = RandomUtils.nextInt(1, 256);
        this.subnet = "192.168." + Integer.toString(subnetID) + ".0/24";
        this.hostIP = "192.168." + Integer.toString(subnetID) + ".1";
        this.omNodeIP = DockerCluster.getKthIP(hostIP, 0);
        try {
            this.build();
        } catch (Exception e) {
            logger.error("Cannot build cluster " + e);
        }
    }

    @Override
    public void prepareUpgrade() {
        logger.info("ozone prepared upgrade");
        int idx = getFirstLiveNodeIdx();
        if (idx == -1) {
            logger.error("cannot upgrade, all nodes are down");
            throw new RuntimeException(
                    "all nodes are down, cannot prepare upgrade");
        }
        String oriOzone = "/" + system + "/" + originalVersion + "/"
                + "bin/ozone";
        String[] enterSafemode = new String[] { oriOzone, "admin",
                "-safemode", "enter" };
        String[] prepareFSImage = new String[] { oriOzone, "admin",
                "-rollingUpgrade", "prepare" };
        String[] leaveSafemode = new String[] { oriOzone, "admin",
                "safemode", "exit" };
        int ret;
        // ret = dockers[idx].runProcessInContainer(enterSafemode,
        // dockers[idx].env);
        // logger.debug("enter safe mode ret = " + ret);
        // ret = dockers[idx].runProcessInContainer(prepareFSImage,
        // dockers[idx].env);
        // logger.debug("prepare image ret = " + ret);
        ret = dockers[idx].runProcessInContainer(leaveSafemode,
                dockers[idx].env);
        int retrycount = 0;
        while (ret != 0) {
            if (retrycount >= 10)
                break;
            ret = dockers[idx].runProcessInContainer(leaveSafemode,
                    dockers[idx].env);
            logger.debug("leave safemode ret = " + ret);
            retrycount += 1;
        }
    }

    @Override
    public void finalizeUpgrade() {
        logger.debug("ozone upgrade finalized");
    }

    @Override
    public boolean fullStopUpgrade() throws Exception {
        logger.info("[Ozone] Cluster full-stop upgrading...");
        // if (!Config.getConf().prepareImageFirst
        // && Config.getConf().enable_fsimage) {
        // prepareUpgrade(); // it will only be invoked once
        // }
        // prepareUpgrade();
        for (int i = 0; i < dockers.length; i++) {
            dockers[i].flush();
            dockers[i].shutdown();
        }
        ((OzoneDocker) dockers[0]).upgradeSCM();
        prepareUpgrade();
        ((OzoneDocker) dockers[1]).upgradeSCM();
        for (int i = 0; i < dockers.length; i++) {
            if (i >= 2) {
                dockers[i].upgrade();
            }
        }
        ((OzoneDocker) dockers[1]).startSCMShell();
        ((OzoneDocker) dockers[0]).startSCMShell();
        logger.info("Cluster upgraded");
        return true;
    }

    public void teardown() {
        // Chmod so that we can read/write them on the host machine
        try {
            for (int i = 0; i < dockers.length; ++i) {
                dockers[i].chmodDir();
            }
        } catch (Exception e) {
            logger.error("fail to chmod dir");
        }

        try {
            Process buildProcess = Utilities.exec(
                    new String[] { "docker", "compose", "down" }, workdir);
            buildProcess.waitFor();
            logger.info("teardown docker compose in " + workdir);
        } catch (IOException | InterruptedException e) {
            logger.error("failed to teardown docker", e);
        }

        if (!Config.getConf().keepDir) {
            try {
                Utilities.exec(new String[] { "rm", "-rf",
                        this.workdir
                                .getAbsolutePath() },
                        ".");
            } catch (IOException e) {
                e.printStackTrace();
            }
            logger.info("[teardown] deleting dir");
        }
    }

    @Override
    public String getNetworkIP() {
        return hostIP;
    }

    @Override
    public IDocker getDocker(int i) {
        return dockers[i];
    }

    static String template = ""
            + "version: '3'\n"
            + "services:\n"
            + "\n"
            + "${dockers}"
            + "\n"
            + "networks:\n"
            + "    ${networkName}:\n"
            + "        driver: bridge\n"
            + "        ipam:\n"
            + "            driver: default\n"
            + "            config:\n"
            + "                - subnet: ${subnet}\n";

    @Override
    public void formatComposeYaml() {
        Map<String, String> formatMap = new HashMap<>();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dockers.length; ++i) {
            sb.append(dockers[i].formatComposeYaml());
        }
        String dockersFormat = sb.toString();
        formatMap.put("dockers", dockersFormat);
        formatMap.put("subnet", subnet);
        formatMap.put("networkName", networkName);
        StringSubstitutor sub = new StringSubstitutor(formatMap);
        this.composeYaml = sub.replace(template);
    }
}
