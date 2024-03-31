package org.zlab.upfuzz.fuzzingengine;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.packet.*;
import org.zlab.upfuzz.fuzzingengine.packet.Packet.PacketType;
import org.zlab.upfuzz.utils.Utilities;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.nyx.LibnyxInterface;

import static org.zlab.upfuzz.nyx.MiniClientMain.runTheTestsBeforeChangingVersion;
import static org.zlab.upfuzz.nyx.MiniClientMain.setTestType;
import static org.zlab.upfuzz.nyx.MiniClientMain.changeVersionAndRunTheTests;

class NyxStackedTestThread implements Callable<StackedFeedbackPacket> {

    static Logger logger = LogManager.getLogger(NyxStackedTestThread.class);

    private StackedFeedbackPacket stackedFeedbackPacket;
    private final int direction;
    private final StackedTestPacket stackedTestPacket;
    private Path configPath;
    private Path previousConfigPath;
    private LibnyxInterface libnyx;
    private boolean sameConfigAsLastTime;
    private boolean isDowngradeSupported;
    // private AtomicInteger decision; // Shared decision variable
    // private BlockingQueue<StackedFeedbackPacket>
    // feedbackPacketQueueBeforeVersionChange;

    // If the cluster cannot start up for 3 times, it's serious
    int CLUSTER_START_RETRY = 3; // stop retry for now

    public NyxStackedTestThread(int direction,
            StackedTestPacket stackedTestPacket,
            LibnyxInterface libnyx,
            Path configPath, Path previousConfigPath,
            boolean sameConfigAsLastTime,
            boolean isDowngradeSupported) {
        this.direction = direction;
        this.stackedTestPacket = stackedTestPacket;
        this.libnyx = libnyx;
        this.previousConfigPath = previousConfigPath;
        this.configPath = configPath;
        this.sameConfigAsLastTime = sameConfigAsLastTime;
        this.isDowngradeSupported = isDowngradeSupported;
    }

    public StackedFeedbackPacket getStackedFeedbackPacket() {
        return stackedFeedbackPacket;
    }

    @Override
    public StackedFeedbackPacket call() throws Exception {
        logger.info("Previous Config Path: " + previousConfigPath);
        logger.info("Same config as last time: " + this.sameConfigAsLastTime);
        if (this.previousConfigPath == null || !this.sameConfigAsLastTime) {
            // the miniClient will setup the distributed system according to the
            // defaultStackedTestPacket and the config
            Path defaultStackedTestPath = Paths.get(this.libnyx.getSharedir(),
                    "stackedTestPackets",
                    "defaultStackedPacket.ser");
            Path defaultTestPlanPath = Paths.get(this.libnyx.getSharedir(),
                    "testPlanPackets",
                    "defaultTestPlanPacket.ser");
            Path sharedConfigPath = Paths.get(this.libnyx.getSharedir(),
                    "archive.tar.gz");
            try {
                // Created sharedir/stackedTestPackets directory
                Paths.get(this.libnyx.getSharedir(), "stackedTestPackets")
                        .toFile().mkdir();
                Paths.get(this.libnyx.getSharedir(), "testPlanPackets")
                        .toFile().mkdir();
                // Copy the default stacked packet
                Utilities.writeObjectToFile(defaultStackedTestPath.toFile(),
                        stackedTestPacket);

                // Copy the config file to the sharedir
                // Zip the config into a zip file
                Process tar = Utilities.exec(
                        new String[] { "tar",
                                "-czf", "archive.tar.gz",
                                "./", },
                        configPath.toFile());
                tar.waitFor();

                System.out.println(configPath
                        .resolve("archive.tar.gz").toAbsolutePath().toString());
                FileUtils.copyFile(
                        configPath.resolve("archive.tar.gz")
                                .toFile(),
                        sharedConfigPath.toFile(), true);

            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } catch (InterruptedException e) {
                // zip failed
                e.printStackTrace();
                return null;
            }
        }
        if (this.previousConfigPath == null) {
            logger.info("Should create a new nyx vm");
            long startTime = System.currentTimeMillis();
            this.libnyx.nyxNew();
            // setTestDirection(this.direction);
            // setClientGroup(stackedTestPacket.clientGroupForVersionDelta);
            if (Config.getConf().debug) {
                logger.info(
                        "[Fuzzing Client] First execution: Time needed to start up a new nyx vm "
                                + (System.currentTimeMillis() - startTime)
                                + " milliseconds");
            }
        } else if (!this.sameConfigAsLastTime) {
            long startTime = System.currentTimeMillis();
            this.libnyx.nyxShutdown();
            this.libnyx.nyxNew();
            // setTestDirection(this.direction);
            // setClientGroup(stackedTestPacket.clientGroupForVersionDelta);
            if (Config.getConf().debug) {
                logger.info(
                        "[Fuzzing Client] New config: Time needed to shutdown old nyx vm and start a new nyx vm "
                                + (System.currentTimeMillis() - startTime)
                                + " seconds");
            }
        }
        // this.previousConfigPath = configPath;

        // Now write the stackedTestPacket to be used for actual tests
        logger.info("[Fuzzing Client] Starting New Execution");
        long startTime3 = System.currentTimeMillis();
        String stackedTestFileLocation = "stackedTestPackets/"
                + RandomStringUtils.randomAlphanumeric(8) + ".ser";
        Path stackedTestPath = Paths.get(this.libnyx.getSharedir(),
                stackedTestFileLocation);
        System.out.println(stackedTestPath);
        if (Config.getConf().debug) {
            logger.info("[Fuzzing Client] time for getting stacked test path "
                    + (System.currentTimeMillis() - startTime3)
                    + " milliseconds");
        }

        long startTime4 = System.currentTimeMillis();
        try {
            Utilities.writeObjectToFile(stackedTestPath.toFile(),
                    this.stackedTestPacket);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        if (Config.getConf().debug) {
            logger.info("[Fuzzing Client] time for writing test packet to file "
                    + (System.currentTimeMillis() - startTime4)
                    + " milliseconds");
        }

        // tell the nyx agent where to find the stackedTestPacket
        long startTime5 = System.currentTimeMillis();
        this.libnyx.setInput(stackedTestFileLocation); // set the test file
                                                       // location as input
        if (Config.getConf().debug) {
            logger.info("[Fuzzing Client] time for libnyx setInput function "
                    + (System.currentTimeMillis() - startTime5)
                    + " milliseconds");
        }

        setTestType(0);
        // setTestDirection(this.direction);
        // setIsDowngradeSupported(this.isDowngradeSupported);
        long startTime6 = System.currentTimeMillis();
        this.libnyx.nyxExec();
        if (Config.getConf().debug) {
            logger.info("[Fuzzing Client] time for NyxExec() function "
                    + (System.currentTimeMillis() - startTime6)
                    + " milliseconds");
            logger.info("[Fuzzing Client] Total time for Nyx-UpFuzz execution "
                    + (System.currentTimeMillis() - startTime3)
                    + " milliseconds");
        }

        // String storagePath = executor.dockerCluster.workdir.getAbsolutePath()
        // .toString();
        String archive_name = "";
        String directoryPath = Paths.get(this.libnyx.getWorkdir(),
                "dump").toAbsolutePath().toString();
        File directory = new File(directoryPath);

        // Check if the provided path is a directory
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".tar.gz")) {
                        archive_name = file.getName();
                        break;
                    }
                }
            } else {
                logger.info(
                        "[HKLOG] Fuzzing Client: No files found in the directory.");
            }
        } else {
            logger.info(
                    "[HKLOG] Fuzzing Client: Provided path is not a directory.");
        }

        if (!archive_name.equals("")) {
            String storagePath = directoryPath + "/" + archive_name;
            String unzip_archive_command = "cd " + directoryPath + "/ ; "
                    + "tar -xzf " + archive_name + " ; "
                    + "cp persistent/stackedFeedbackPacket.ser " + directoryPath
                    + " ; "
                    + "cd - ;"
                    + "mv " + storagePath + " "
                    + Paths.get(this.libnyx.getSharedir())
                    + "/$(date +'%Y-%m-%d-%H-%M-%S')-" + archive_name + " ; ";

            try {
                long startTime2 = System.currentTimeMillis();
                ProcessBuilder builder = new ProcessBuilder();
                builder.command("/bin/bash", "-c", unzip_archive_command);
                // builder.directory(new File(System.getProperty("user.home")));
                builder.redirectErrorStream(true);

                Process process = builder.start();
                int exitCode = process.waitFor();
                if (Config.getConf().debug) {
                    logger.info(
                            "[Fuzzing Client] Time needed to unzip the fuzzing storage archive and moving it to the workdir: "
                                    + (System.currentTimeMillis() - startTime2)
                                    + " milliseconds");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // get feedback file from hpush dir (in workdir)
        long startTimeFdbk = System.currentTimeMillis();
        Path stackedFeedbackPath = Paths.get(this.libnyx.getWorkdir(),
                "dump",
                "stackedFeedbackPacket.ser");

        // convert it to StackedFeedbackPacket
        StackedFeedbackPacket stackedFeedbackPacket;
        try (DataInputStream in = new DataInputStream(new FileInputStream(
                stackedFeedbackPath.toAbsolutePath().toString()))) {
            int intType = in.readInt();
            if (intType == -1) {
                logger.info("Executor startup error!");
                return null;
            }
            if (intType != PacketType.StackedFeedbackPacket.value) {
                logger.info("Incorrect packet type hit");
                return null;
            }
            stackedFeedbackPacket = StackedFeedbackPacket.read(in);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        if (Config.getConf().debug) {
            logger.info(
                    "[Fuzzing Client] Time needed to read the feedback packet: "
                            + (System.currentTimeMillis() - startTimeFdbk)
                            + " milliseconds");
        }

        return stackedFeedbackPacket;
    }
}