package org.zlab.upfuzz.fuzzingengine.server;

import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.utils.Utilities;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Random;

public abstract class Corpus implements ICorpus {
    Random rand = new Random();
    double[] cumulativeProbabilities;
    CycleQueue<Seed>[] cycleQueues;

    public Corpus(int queueSize, double[] probabilities) {
        assert queueSize == probabilities.length;

        cumulativeProbabilities = new double[probabilities.length];
        cycleQueues = new CycleQueue[probabilities.length];
        for (int i = 0; i < cycleQueues.length; i++) {
            cycleQueues[i] = new CycleQueue<>();
        }

        cumulativeProbabilities[0] = probabilities[0];
        for (int i = 1; i < probabilities.length; i++) {
            cumulativeProbabilities[i] = cumulativeProbabilities[i - 1]
                    + probabilities[i];
        }
    }

    public abstract void addSeed(Seed seed, boolean newOriBC, boolean newUpBC,
            boolean newOriFC, boolean newUpFC, boolean newBCAfterUpgrade,
            boolean newBCAfterDowngrade, boolean newOriBoundaryChange,
            boolean newUpBoundaryChange, boolean newNonMatchableFC,
            boolean newBCVD, boolean newFCVD);

    public void addSeed(Seed seed, boolean newOriBC, boolean newOriFC,
            boolean newBCAfterUpgrade, boolean newOriBoundaryChange,
            boolean newNonMatchableFC) {
        addSeed(seed, newOriBC, false, newOriFC, false, newBCAfterUpgrade,
                false, newOriBoundaryChange, false, newNonMatchableFC,
                false, false);
    }

    public Seed getSeed() {
        // Not support CorpusVersionDeltaSixQueue
        // if current class is CorpusVersionDetlaSixQueue, throw exception
        if (cumulativeProbabilities.length != cycleQueues.length) {
            throw new RuntimeException(
                    "cumulativeProbabilities and cycleQueues have different lengths");
        }
        int i = Utilities.pickWeightedRandomChoice(cumulativeProbabilities,
                rand.nextDouble());
        return cycleQueues[i].getNextSeed();
    }

    public abstract int initCorpus();

    public static void saveSeedQueueOnDisk(Seed seed, String queueName,
            int seedID) {
        // Serialize the seed of the queue in to disk
        if (Config.getConf().corpus == null || queueName == null) {
            throw new RuntimeException(
                    "Config.getConf().corpusDir is null, cannot save seed");
        }
        // corpus/system/queueName/seed_seedID/seed
        Path queueDirPath = Paths.get(Config.getConf().corpus)
                .resolve(Config.getConf().system)
                .resolve(queueName);
        Utilities.createDirIfNotExist(queueDirPath);

        Path seedDirPath = queueDirPath.resolve("seed_" + seedID);
        Utilities.createDirIfNotExist(seedDirPath);

        // Serialize the seed
        Path filePath = seedDirPath.resolve("seed");
        try {
            FileOutputStream fileOut = new FileOutputStream(filePath.toFile());
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(seed);
            out.close();
            fileOut.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Copy the config folder
        if (seed.configIdx == -1) // test
            return;
        String configDirName = "test" + seed.configIdx;

        Path configDir = Paths.get(
                Config.getConf().configDir, Config.getConf().originalVersion
                        + "_" + Config.getConf().upgradedVersion)
                .resolve(configDirName);
        if (!configDir.toFile().exists()) {
            throw new RuntimeException(
                    "Config folder does not exist: " + configDir);
        }
        try {
            Utilities.copyDir(configDir, seedDirPath.resolve("config"));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(
                    "Cannot copy config folder: " + configDir);
        }
    }

    public static int loadSeedIntoQueue(CycleQueue cycleQueue, File queueDir,
            int initTestId) {
        if (queueDir == null || !queueDir.exists())
            return initTestId;

        assert queueDir.isDirectory();

        int testId = initTestId;

        for (File seedDir : Objects.requireNonNull(queueDir.listFiles())) {
            // there's a file called "seed" under seedDir, deserailize it
            File seedFile = new File(seedDir, "seed");
            if (!seedFile.exists())
                continue;
            Seed seed;
            try {
                FileInputStream fileIn = new FileInputStream(seedFile);
                ObjectInputStream in = new ObjectInputStream(fileIn);
                seed = (Seed) in.readObject();
                in.close();
                fileIn.close();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                continue;
            }
            if (seed == null)
                continue;

            if (!Config.getConf().reuseInitSeedConfig) {
                /**
                 * The config might not be compatible across versions.
                 */
                seed.configIdx = -1;
            } else {
                // copy over the config folder and increase the configIdx
                throw new RuntimeException("Not supported copy config");
            }

            // Handle the test id... need to avoid the conflicts
            seed.testID = testId++;
            cycleQueue.addSeed(seed);
        }
        return testId;
    }
}
