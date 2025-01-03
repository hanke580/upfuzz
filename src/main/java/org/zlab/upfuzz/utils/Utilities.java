package org.zlab.upfuzz.utils;

import static java.lang.String.format;

import java.io.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.FeedBack;
import org.zlab.upfuzz.fuzzingengine.packet.StackedTestPacket;
import org.zlab.upfuzz.fuzzingengine.packet.TestPacket;

public class Utilities {
    static Logger logger = LogManager.getLogger(Utilities.class);
    public static Random rand = new Random();

    public static List<Integer> permutation(int size) {
        List<Integer> indexArray = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            indexArray.add(i);
        }
        Collections.shuffle(indexArray);
        return indexArray;
    }

    public static Pair<CommandSequence, CommandSequence> deserializeCommandSequence(
            Path filePath) {
        Pair<CommandSequence, CommandSequence> commandSequencePair = null;
        try {
            FileInputStream fileIn = new FileInputStream(filePath.toFile());
            ObjectInputStream in = new ObjectInputStream(fileIn);
            commandSequencePair = (Pair<CommandSequence, CommandSequence>) in
                    .readObject();
            in.close();
            fileIn.close();
        } catch (IOException i) {
            i.printStackTrace();
            return null;
        } catch (ClassNotFoundException c) {
            System.out.println("Command class not found");
            c.printStackTrace();
            return null;
        }
        return commandSequencePair;
    }

    public static boolean isEqualCoverage(ExecutionDataStore curCoverage,
            ExecutionDataStore testSequenceCoverage) {
        // Return true if two coverage is identical
        if (testSequenceCoverage == null && curCoverage != null)
            return false;

        if (testSequenceCoverage != null && curCoverage == null)
            return false;

        for (final ExecutionData testSequenceData : testSequenceCoverage
                .getContents()) {

            final Long id = Long.valueOf(testSequenceData.getId());
            final ExecutionData curData = curCoverage.get(id);

            // For one class, merge the coverage
            if (curData != null) {
                assertCompatibility(curData, testSequenceData);
                int[] curProbes = curData.getProbes();
                final int[] testSequenceProbes = testSequenceData.getProbes();
                for (int i = 0; i < curProbes.length; i++) {
                    // Now only try with the boolean first
                    if ((curProbes[i] == 0 && testSequenceProbes[i] != 0) ||
                            (curProbes[i] != 0 && testSequenceProbes[i] == 0)) {
                        // logger.debug("cur probes: ");
                        // for (int j = 0; j < curProbes.length; j++) {
                        // logger.debug(curProbes[j] + " ");
                        // }
                        // logger.debug("test probes: ");
                        // for (int j = 0; j < testSequenceProbes.length; j++) {
                        // logger.debug(testSequenceProbes[j] + " ");
                        // }

                        // logger.debug("probe len = " + curProbes.length);
                        // logger.debug("Class " + testSequenceData.getName() +
                        // " id: [" + i + "]"
                        // + " is different!");
                        return false;
                    }
                }
            } else {
                logger.debug("curData not triggered " +
                        testSequenceData.getName());
                return false;
            }
        }
        return true;
    }

    public static void printCoverages(int testId, FeedBack[] feedBacks,
            String type) {
        FeedBack fb = new FeedBack();
        int i = 0;
        for (FeedBack feedBack : feedBacks) {
            for (ExecutionData curData : feedBack.originalCodeCoverage
                    .getContents()) {
                final Long id = Long.valueOf(curData.getId());
                final String name = curData.getName();
                int[] curProbes = curData.getProbes();
                System.out.println();
                logger.info(
                        "printing coverages for testId: " + testId + ", " +
                                "feedback probe " + id + ", class " + name
                                + " from " + type + ": ");
                String probeDetails = "";
                for (int j = 0; j < curProbes.length; j++) {
                    probeDetails += (curProbes[j] + " ");
                }
                logger.info(probeDetails);
                i += 1;
            }
        }
    }

    public static boolean hasNewBits(ExecutionDataStore curCoverage,
            ExecutionDataStore testSequenceCoverage) {
        if (Config.getConf().enableHitCount) {
            return hasNewBitsAccum(curCoverage, testSequenceCoverage);
        } else {
            if (Config.getConf().debugCoverage) {
                return hasNewBitsDebug(curCoverage, testSequenceCoverage);
            } else {
                return hasNewBitsBoolean(curCoverage, testSequenceCoverage);
            }
        }
    }

    private static boolean hasNewBitsBoolean(ExecutionDataStore curCoverage,
            ExecutionDataStore testSequenceCoverage) {

        if (testSequenceCoverage == null)
            return false;

        if (curCoverage == null) {
            return true;
        } else {
            for (final ExecutionData testSequenceData : testSequenceCoverage
                    .getContents()) {

                final long id = testSequenceData.getId();
                final ExecutionData curData = curCoverage.get(id);

                // For one class, merge the coverage
                if (curData != null) {
                    assertCompatibility(curData, testSequenceData);
                    int[] curProbes = curData.getProbes();
                    final int[] testSequenceProbes = testSequenceData
                            .getProbes();
                    for (int i = 0; i < curProbes.length; i++) {
                        // Now only try with the boolean first
                        if (curProbes[i] == 0 && testSequenceProbes[i] != 0) {
                            // System.out.println();
                            // System.out.print("cur probes: ");
                            // for (int j = 0; j < curProbes.length; j++) {
                            // System.out.print(curProbes[j] + " ");
                            // }
                            // System.out.println();
                            // System.out.print("test probes: ");
                            // for (int j = 0; j < testSequenceProbes.length;
                            // j++) {
                            // System.out.print(testSequenceProbes[j] + " ");
                            // }
                            // System.out.println();

                            // System.out
                            // .println("probe len = " + curProbes.length);
                            // System.out.println("Class "
                            // + testSequenceData.getName() + " id: [" + i
                            // + "]" + " is different!");
                            return true;
                        }
                    }
                } else {
                    return true;
                }
            }
            return false;
        }
    }

    private static boolean hasNewBitsDebug(ExecutionDataStore curCoverage,
            ExecutionDataStore testSequenceCoverage) {

        if (testSequenceCoverage == null)
            return false;

        if (curCoverage == null) {
            return true;
        } else {
            boolean newBit = false;
            for (final ExecutionData testSequenceData : testSequenceCoverage
                    .getContents()) {

                final long id = testSequenceData.getId();
                final ExecutionData curData = curCoverage.get(id);

                // For one class, merge the coverage
                if (curData != null) {
                    assertCompatibility(curData, testSequenceData);
                    int[] curProbes = curData.getProbes();
                    final int[] testSequenceProbes = testSequenceData
                            .getProbes();
                    for (int i = 0; i < curProbes.length; i++) {
                        // Now only try with the boolean first
                        if (curProbes[i] == 0 && testSequenceProbes[i] != 0) {
                            logger.debug("[Coverage] class "
                                    + testSequenceData.getName()
                                    + " has a new coverage");
                            // System.out.println();
                            // System.out.print("cur probes: ");
                            // for (int j = 0; j < curProbes.length; j++) {
                            // System.out.print(curProbes[j] + " ");
                            // }
                            // System.out.println();
                            // System.out.print("test probes: ");
                            // for (int j = 0; j < testSequenceProbes.length;
                            // j++) {
                            // System.out.print(testSequenceProbes[j] + " ");
                            // }
                            // System.out.println();

                            // System.out.println("probe len = " +
                            // curProbes.length);
                            // System.out.println("Class " +
                            // testSequenceData.getName() +
                            // " id: [" + i + "]"
                            // + " is different!");
                            newBit = true;
                            break;
                        }
                    }
                } else {
                    logger.debug(
                            "[Coverage] class " + testSequenceData.getName()
                                    + " has new coverage (new class)");
                    newBit = true;
                }
            }
            return newBit;
        }
    }

    public static void writeObjectToFile(File file, Object obj)
            throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        logger.info("Got serializable object? " + obj instanceof Serializable);
        oos.writeObject(obj);
        logger.info(true);
        oos.close();
        fos.close();
    }

    public static Object readObjectFromFile(File file)
            throws IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream(file);
        ObjectInputStream ois = new ObjectInputStream(fis);
        Object result = ois.readObject();
        ois.close();
        fis.close();
        return result;
    }

    public static boolean hasNewBitsAccum(ExecutionDataStore curCoverage,
            ExecutionDataStore testSequenceCoverage) {

        if (testSequenceCoverage == null)
            return false;

        if (curCoverage == null) {
            return true;
        } else {
            for (final ExecutionData testSequenceData : testSequenceCoverage
                    .getContents()) {
                final long id = testSequenceData.getId();
                final ExecutionData curData = curCoverage.get(id);

                // For one class, merge the coverage
                if (curData != null) {
                    assertCompatibility(curData, testSequenceData);
                    int[] curProbes = curData.getProbes();
                    final int[] testSequenceProbes = testSequenceData
                            .getProbes();
                    for (int i = 0; i < curProbes.length; i++) {
                        if (curProbes[i] < testSequenceProbes[i]
                                && getBucketIndex(
                                        curProbes[i]) < getBucketIndex(
                                                testSequenceProbes[i])) {
                            if (Config.getConf().debugHitCount) {
                                logger.debug("[Coverage] class "
                                        + testSequenceData.getName()
                                        + " has a new coverage" + " curProb = "
                                        + curProbes[i] + " curBucket = "
                                        + getBucketIndex(curProbes[i])
                                        + " testProb = " + testSequenceProbes[i]
                                        + " curBucket = " + getBucketIndex(
                                                testSequenceProbes[i]));
                            }
                            return true;
                        }
                    }
                } else {
                    return true;
                }
            }
            return false;
        }
    }

    public static int mergeMax(ExecutionDataStore curCoverage,
            ExecutionDataStore testSequenceCoverage) {
        int score = 0;

        if (testSequenceCoverage == null)
            return score;

        if (curCoverage == null) {
            throw new RuntimeException("Please initialize curCoverage");
        }

        for (final ExecutionData testSequenceData : testSequenceCoverage
                .getContents()) {

            final long id = testSequenceData.getId();
            final ExecutionData curData = curCoverage.get(id);

            // For one class, merge the coverage
            if (curData != null) {
                assertCompatibility(curData, testSequenceData);
                int[] curProbes = curData.getProbes();
                final int[] testSequenceProbes = testSequenceData
                        .getProbes();
                for (int i = 0; i < curProbes.length; i++) {
                    if (curProbes[i] < testSequenceProbes[i]) {
                        score += testSequenceProbes[i] - curProbes[i];
                        curProbes[i] = testSequenceProbes[i];
                    }
                }
            } else {
                final int[] testSequenceProbes = testSequenceData
                        .getProbes();
                for (int i = 0; i < testSequenceProbes.length; i++) {
                    score += testSequenceProbes[i];
                }
            }
            curCoverage.merge(testSequenceData);
        }
        return score;
    }

    // Overwrite the merge function
    // use max instead of merge
    // compute the new bits bring by testSequenceCoverage
    public static boolean computeDelta(ExecutionDataStore curCoverage,
            ExecutionDataStore testSequenceCoverage) {

        System.out.println("Computing Delta");

        if (testSequenceCoverage == null)
            return false;

        if (curCoverage == null) {
            return true;
        } else {
            for (final ExecutionData testSequenceData : testSequenceCoverage
                    .getContents()) {

                boolean findNewBit = false;
                final Long id = Long.valueOf(testSequenceData.getId());
                final ExecutionData curData = curCoverage.get(id);

                // For one class, merge the coverage
                if (curData != null) {
                    assertCompatibility(curData, testSequenceData);
                    int[] curProbes = curData.getProbes();
                    final int[] testSequenceProbes = testSequenceData
                            .getProbes();
                    for (int i = 0; i < curProbes.length; i++) {
                        // Now only try with the boolean first
                        if (curProbes[i] == 0 && testSequenceProbes[i] != 0) {
                            continue;
                        } else {
                            findNewBit = true;
                            testSequenceProbes[i] = 0;
                        }
                    }
                    // if (findNewBit) {
                    // System.out.println(
                    // "new bit class: " + testSequenceData.getName());
                    // }
                } else {
                    // System.out.println(
                    // "new bit class: " + testSequenceData.getName());
                    continue;
                }
            }
            return false;
        }
    }

    private static int getBucketIndex(int exec_count) {
        if (exec_count == 1)
            return 0;
        if (exec_count == 2)
            return 1;
        if (exec_count == 3)
            return 2;
        if (exec_count >= 4 && exec_count <= 7)
            return 3;
        if (exec_count >= 8 && exec_count <= 15)
            return 4;
        if (exec_count >= 16 && exec_count <= 31)
            return 5;
        if (exec_count >= 32 && exec_count <= 127)
            return 6;
        return 7; // For 128+ executions
    }

    public static Pair<Integer, Integer> getCoverageStatus(
            ExecutionDataStore curCoverage) {
        if (curCoverage == null) {
            return new Pair(0, 0);
        }

        int coveredProbes = 0;
        int probeNum = 0;

        for (final ExecutionData curData : curCoverage.getContents()) {
            int[] curProbes = curData.getProbes();
            probeNum += curProbes.length;
            for (int i = 0; i < curProbes.length; i++) {
                if (curProbes[i] != 0)
                    coveredProbes++;
            }
        }
        return new Pair(coveredProbes, probeNum);
    }

    public static void assertCompatibility(ExecutionData curData,
            ExecutionData testSequenceData) {
        if (curData.getId() != testSequenceData.getId()) {
            throw new IllegalStateException(
                    format("Different ids (%016x and %016x).",
                            Long.valueOf(curData.getId()),
                            Long.valueOf(testSequenceData.getId())));
        }
        if (!curData.getName().equals(testSequenceData.getName())) {
            throw new IllegalStateException(
                    format("Different class names %s and %s for id %016x.",
                            curData.getName(), testSequenceData.getName(),
                            Long.valueOf(testSequenceData.getId())));
        }
        if (curData.getProbes().length != testSequenceData.getProbes().length) {
            throw new IllegalStateException(format(
                    "Incompatible execution data for class %s with id %016x.",
                    testSequenceData.getName(),
                    Long.valueOf(testSequenceData.getId())));
        }
    }

    public static Process runProcess(ProcessBuilder pb, String desc) {
        Process p = null;
        try {
            // System.out.println("Execute: " + desc);
            p = pb.start();
            // BufferedReader in = new BufferedReader(new
            // InputStreamReader(p.getInputStream()));
            // String line;
            // while ((line = in.readLine()) != null) {
            // System.out.println(line);
            // System.out.flush();
            // }
            p.waitFor();
            // in.close();
            // System.out.println(desc + " Successful");

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return p;
    }

    public static Process exec(String[] cmds, File path) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(cmds).redirectErrorStream(true);
        pb.directory(path);
        return pb.start();
    }

    public static Process exec(String[] cmds, String path) throws IOException {
        return exec(cmds, new File(path));
    }

    public static String readProcess(Process p) {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(p.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            p.waitFor();
            in.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static String readProcessErrorStream(Process p) {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(p.getErrorStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            p.waitFor();
            in.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static String getGitBranch(String path)
            throws IOException, InterruptedException {
        Process p = exec(
                new String[] { "git", "rev-parse", "--abbrev-ref", "HEAD" },
                path);
        String gitBranch = readProcess(p).replace("\n", "");
        return gitBranch;
    }

    public static String getGitTag(String path)
            throws IOException, InterruptedException {
        Process p = exec(
                new String[] { "git", "describe", "--abbrev=0", "--tags",
                        "HEAD" },
                path);
        String gitBranch = readProcess(p).replace("\n", "");
        return gitBranch;
    }

    public static String getMainClassName() throws ClassNotFoundException {
        String sunJavaCommand = System.getProperty("sun.java.command");
        if (sunJavaCommand != null) {
            return sunJavaCommand.split(" ")[0];
        }
        return null;
        // for (Entry<Thread, StackTraceElement[]> entry : Thread
        // .getAllStackTraces().entrySet()) {
        // Thread thread = entry.getKey();
        // // System.out.println(thread.getThreadGroup().getName() );
        // if (thread.getThreadGroup() != null
        // && thread.getThreadGroup().getName().equals("main")) {
        // for (StackTraceElement stackTraceElement : entry.getValue()) {
        // // System.out.println(stackTraceElement.getClassName()+ " "
        // // + stackTraceElement.getMethodName() + " " +
        // // stackTraceElement.getFileName());
        // if (stackTraceElement.getMethodName().equals("main")) {

        // try {
        // Class<?> c = Class
        // .forName(stackTraceElement.getClassName());
        // Class[] argTypes = new Class[] { String[].class };
        // // This will throw NoSuchMethodException in case of
        // // fake main methods
        // c.getDeclaredMethod("main", argTypes);
        // // return stackTraceElement.getClassName();
        // } catch (NoSuchMethodException e) {
        // e.printStackTrace();
        // }
        // }
        // }
        // }
        // }
        // return null;
    }

    public static boolean oneOf(Random rand, int n) {
        if (n <= 0) {
            throw new RuntimeException("n in oneOf <= 0");
        }
        return rand.nextInt(n) == 0;
    }

    public static boolean nOutOf(Random rand, int x, int y) {
        // probability x/y
        if (y <= 0 || x < 0) {
            throw new RuntimeException("n in oneOf <= 0");
        }
        return rand.nextInt(y) < x;
    }

    public static List<Integer> pickKoutofN(int k, int n) {
        if (k > n || n <= 0)
            return null;
        List<Integer> indexes = new LinkedList<>();
        for (int i = 0; i < n; i++) {
            indexes.add(i);
        }
        if (k == n)
            return indexes;
        Collections.shuffle(indexes);
        List<Integer> retIndexes = new LinkedList<>();
        for (int i = 0; i < k; i++) {
            retIndexes.add(indexes.get(i));
        }
        return retIndexes;
    }

    public static boolean write2TXT(File file, String content, boolean append) {

        try {
            // If file doesn't exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file.getAbsoluteFile(), append);
            BufferedWriter bw = new BufferedWriter(fw);

            // Write in file
            bw.write(content);

            // Close connection
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    public static boolean writeCmdSeq(File file, Object object) {
        try {
            FileOutputStream fileOut = new FileOutputStream(file);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(object);
            out.close();
            fileOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static Map<String, List<Map.Entry<String, String>>> loadFunctoinFromStaticAnalysis(
            Path fileName) {
        JSONParser jsonParser = new JSONParser();
        Map<String, List<Map.Entry<String, String>>> funcToInst = new HashMap<>();

        try (FileReader reader = new FileReader(fileName.toFile())) {
            Object obj = jsonParser.parse(reader);
            JSONObject jsonObject = (JSONObject) obj;
            Set<Map.Entry<String, String>> entrySet = jsonObject.entrySet();

            for (Map.Entry<String, String> e : entrySet) {
                String[] ret = e.getValue()
                        .split(e.getKey().replace("$", "\\$") + "\\(");
                assert (ret.length == 2);
                String ClassName = ret[0].substring(0, ret[0].length() - 1);
                String MethodName = e.getKey();
                String ParamDesc = "(" + ret[1];

                // Only instrument class that's inside org.apache.cassandra.*
                if (ClassName.contains("cassandra")) {
                    if (funcToInst.containsKey(ClassName)) {
                        funcToInst.get(ClassName).add(
                                new AbstractMap.SimpleEntry<>(MethodName,
                                        ParamDesc));
                    } else {
                        List<Map.Entry<String, String>> list = new ArrayList<>();
                        list.add(new AbstractMap.SimpleEntry<>(MethodName,
                                ParamDesc));
                        funcToInst.put(ClassName, list);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return funcToInst;
    }

    public void generateJacocoIncludeOption() {
        Path filePath = Paths.get("/Users/hanke/Desktop/SerDes.json");
        Map<String, List<Map.Entry<String, String>>> funcs = Utilities
                .loadFunctoinFromStaticAnalysis(filePath);
        for (String className : funcs.keySet()) {
            System.out.print(className + ":");
        }
        System.out.println();
    }

    // biasedRand returns a random int in range [0..n),
    // probability of n-1 is k times higher than probability of 0.
    public static int biasRand(Random rand, int n, int k) {
        double nf = (float) n;
        double kf = (float) k;
        double rf = nf * (kf / 2 + 1) * rand.nextFloat();
        double bf = (-1 + Math.sqrt(1 + 2 * kf * rf / nf)) * nf / kf;
        return (int) bf;
    }

    public static int generateExponentialRandom(Random rand, double lambda,
            int lowerBound, int upperBound) {
        double randomExponential = Math.log(1 - rand.nextDouble()) / (-lambda);
        // Scale and shift to fit the range [lowerBound, upperBound]
        int scaledValue = (int) (randomExponential + lowerBound);
        // Ensure it does not exceed the upperBound
        return Math.min(scaledValue, upperBound);
    }

    public static <T> T[] concatArray(T[] array1, T[] array2) {
        T[] result = Arrays.copyOf(array1, array1.length + array2.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    public static Set<Parameter> strings2Parameters(
            Collection<String> strings) {
        Set<Parameter> ret = new HashSet<>();
        for (String str : strings) {
            ret.add(new Parameter(CONSTANTSTRINGType.instance, str));
        }
        return ret;
    }

    public static ParameterType.InCollectionType createInStringCollectionType(
            Collection<String> strings) {
        return new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities
                        .strings2Parameters(
                                strings),
                null);
    }

    public static ParameterType.OptionalType createOptionalString(
            String s) {
        return new ParameterType.OptionalType(
                new CONSTANTSTRINGType(s), null);
    }

    public static Set<Parameter> strings2Parameters(
            String[] strings) {
        Set<Parameter> ret = new HashSet<>();
        for (String str : strings) {
            ret.add(new Parameter(CONSTANTSTRINGType.instance, str));
        }
        return ret;
    }

    public static Set<String> parameters2Strings(
            Collection<Parameter> parameters) {
        Set<String> ret = new HashSet<>();
        for (Parameter parameter : parameters) {
            assert parameter.getValue() instanceof String;
            ret.add((String) parameter.getValue());
        }
        return ret;
    }

    public static void saveSeed(CommandSequence commandSequence,
            CommandSequence validationCommandSequence, Path filePath) {
        try {
            FileOutputStream fileOut = new FileOutputStream(filePath.toFile());
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(
                    new Pair<>(commandSequence, validationCommandSequence));
            out.close();
            fileOut.close();
            System.out.println("Serialized data is saved in " +
                    filePath.toString());
        } catch (IOException i) {
            i.printStackTrace();
            return;
        }
    }

    public static void printCommandSequence(CommandSequence cs) {
        System.out.println("Command Sequence:");
        for (String str : cs.getCommandStringList()) {
            System.out.println("str: " + str);
        }
        System.out.println();
    }

    public static int randWithRange(Random rand, int min, int max) {
        // [min, max)
        return rand.nextInt(max - min) + min;
    }

    public static boolean contains(String val, String[] set) {
        for (String e : set) {
            if (e.equals(val)) {
                return true;
            }
        }
        return false;

    }

    public static String encodeString(String s) {
        String encodedString = Base64.getEncoder().encodeToString(s.getBytes());
        return encodedString;
    }

    public static String decodeString(String s) {
        byte[] decodedBytes = Base64.getDecoder().decode(s);
        String decodedString = new String(decodedBytes);
        return decodedString;
    }

    public static Double randDouble(Random rand, Double rangeMin,
            Double rangeMax) {
        double randomValue = rangeMin
                + (rangeMax - rangeMin) * rand.nextDouble();
        return randomValue;
    }

    public static String maskTimeStampHHSS(String str) {
        // remove HH:SS
        String regex = "([0-1]?[0-9]|2[0-3]):[0-5][0-9]";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        return matcher.replaceAll("");
    }

    public static String maskTimeStampYYYYMMDD(String str) {
        // remove YYYY-MM-DD
        String regex = "\\d{4}-\\d{2}-\\d{2}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        return matcher.replaceAll("");
    }

    // Mask Ruby object (For Hbase)
    public static String maskRubyObject(String str) {
        String filteredOutput = str.replaceAll("(?m)^=> #<Java::.*?$", "")
                .trim();
        return filteredOutput;
    }

    public static int[] computeDiffBrokenInv(
            int[] lastBrokenInv,
            int[] curBrokenInv) {
        assert lastBrokenInv.length == curBrokenInv.length;
        int len = lastBrokenInv.length;
        int[] diffBrokenInv = new int[len];
        for (int i = 0; i < len; i++) {
            diffBrokenInv[i] = curBrokenInv[i] - lastBrokenInv[i];
        }
        return diffBrokenInv;
    }

    public static List<Integer> extractTestIDs(
            StackedTestPacket stackedTestPacket) {
        List<Integer> testIDs = new LinkedList<>();
        for (TestPacket tp : stackedTestPacket.getTestPacketList()) {
            testIDs.add(tp.testPacketID);
        }
        return testIDs;
    }

    public static long generateRandomLong(long min, long max) {
        return ThreadLocalRandom.current().nextLong(min, max + 1);
    }

    public static String maskScanTime(String s) {
        return s.replaceAll("Took [0-9]+.[0-9]+ seconds", "");
    }

    public static int parseInt(String str) {
        if (str.equals("Integer.MAX_VALUE"))
            return Integer.MAX_VALUE;
        if (str.equals("Integer.MIN_VALUE"))
            return Integer.MIN_VALUE;
        return Integer.parseInt(str);
    }

    public static void setRandomDeleteItems(Set<?> set, int numItemsToDelete) {
        if (numItemsToDelete > set.size()) {
            throw new IllegalArgumentException(
                    "Number of items to delete exceeds set size.");
        }

        List<?> list = new ArrayList<>(set);
        Random rand = new Random();

        for (int i = 0; i < numItemsToDelete; i++) {
            int randomIndex = rand.nextInt(list.size());
            set.remove(list.get(randomIndex));
            list.remove(randomIndex);
        }
    }

    public static boolean setRandomDeleteAtLeaseOneItem(Set<?> set) {
        if (set.size() == 0) {
            return false;
        }
        // numItemsToDelete: [1, set.size()]
        int numItemsToDelete = randWithRange(rand, 1, set.size() + 1);
        List<?> list = new ArrayList<>(set);
        Random rand = new Random();

        for (int i = 0; i < numItemsToDelete; i++) {
            int randomIndex = rand.nextInt(list.size());
            set.remove(list.get(randomIndex));
            list.remove(randomIndex);
        }
        return true;
    }

    // Only test usage: from bishal
    public static List<String> createExampleCommands() {
        // String[] commandSequenceList = {
        // "CREATE KEYSPACE distributed_test_keyspace WITH REPLICATION=
        // {'class' : 'SimpleStrategy', 'replication_factor': 3 };",
        // "CREATE TABLE distributed_test_keyspace.tbl (pk int, ck int, v
        // int, PRIMARY KEY (pk, ck));",
        // "INSERT INTO distributed_test_keyspace.tbl (pk, ck, v) VALUES (1,
        // 1, 1) IF NOT EXISTS;",
        // "UPDATE distributed_test_keyspace.tbl SET v = 3 WHERE pk = 1 and
        // ck = 1 IF v = 2;",
        // "UPDATE distributed_test_keyspace.tbl SET v = 2 WHERE pk = 1 and
        // ck = 1 IF v = 1;",
        // };
        // String[] validationCommandsList = {
        // "SELECT * FROM distributed_test_keyspace.tbl WHERE pk = 1;",
        // };

        // String[] commandSequenceList2 = { //1,2,3,6,7
        // "CREATE KEYSPACE uuid7bc0babf3a4d4970b4f20f4376046e36 WITH
        // REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor'
        // : 2 };",
        // "CREATE KEYSPACE IF NOT EXISTS
        // uuid22437341ec0044a397da18834342ac32 WITH REPLICATION = { 'class'
        // : 'SimpleStrategy', 'replication_factor' : 2 };",
        // "CREATE TYPE uuid22437341ec0044a397da18834342ac32.y (y
        // set<TEXT>,zZAspM set<INT>,cAPh TEXT,JJZdN TEXT,QMMI
        // set<INT>,JptplUFOay TEXT,OZXBDHHAL TEXT);",
        // "DROP KEYSPACE uuid7bc0babf3a4d4970b4f20f4376046e36;",
        // "DROP KEYSPACE uuid22437341ec0044a397da18834342ac32;",
        // "CREATE KEYSPACE IF NOT EXISTS
        // uuidb0fa2c07e69b4a679dc6725e8697fc91 WITH REPLICATION = { 'class'
        // : 'SimpleStrategy', 'replication_factor' : 2 };",
        // "CREATE TABLE uuidb0fa2c07e69b4a679dc6725e8697fc91.QMMI (QMMI
        // INT,OZXBDHHAL set<TEXT>,Zba set<TEXT>,JptplUFOay TEXT,y
        // TEXT,JJZdN TEXT,UCTCKyMQZwcolZRRsOwT TEXT, PRIMARY KEY
        // (JptplUFOay, y, UCTCKyMQZwcolZRRsOwT, JJZdN )) WITH
        // speculative_retry = 'ALWAYS';",
        // "ALTER TABLE uuidb0fa2c07e69b4a679dc6725e8697fc91.QMMI RENAME
        // cAPh TO cAPh;",
        // "DELETE FROM uuidb0fa2c07e69b4a679dc6725e8697fc91.QMMI WHERE cAPh
        // = 'GVbImcbCdFxPGtKYyNQwiIXYVAdmRJBrQageKxhEZZXGujCWjan' AND y =
        // 'vLIJbZlyHZeVUHuVXbbdBwoPHyidfgxDmRIYeSEkolubnEotnMPPLKKraBnUmyvHVwkWpuzvugVsanfnUOBhIqfybBUAjHYnSxmlovLCcpdkKDgeXDqTuwUKjZERnuWPrHDKGERdqtGT'
        // AND UCTCKyMQZwcolZRRsOwT =
        // 'rSmSxYEuESMgCBrGtYWIjkUDcLlLVYKJKlEFtJmsLkLigaFJXhHBMtEzdGftoHvMLJNuHWdjzFdnhEWBPioHcjVyWRKZnwdWkeAxWWrWxQamAuudiBurzaWKmIGIDstuOxNJGiwEVdqfNZWLEAPAVbjwSIPdbrWCxJcaElU'
        // AND JJZdN = 'hfLBnzt';",
        // "TRUNCATE TABLE uuidb0fa2c07e69b4a679dc6725e8697fc91.QMMI;",
        // "ALTER TABLE uuidb0fa2c07e69b4a679dc6725e8697fc91.QMMI ADD
        // znIkGzEuT set<TEXT> ;",
        // "USE uuidb0fa2c07e69b4a679dc6725e8697fc91;",
        // "ALTER TABLE uuidb0fa2c07e69b4a679dc6725e8697fc91.QMMI ADD
        // JptplUFOay set<INT> ;",
        // "ALTER TABLE uuidb0fa2c07e69b4a679dc6725e8697fc91.QMMI ALTER
        // JJZdN TYPE set<TEXT> ;",
        // "USE uuidb0fa2c07e69b4a679dc6725e8697fc91;",
        // "ALTER TABLE uuidb0fa2c07e69b4a679dc6725e8697fc91.QMMI ADD
        // TKljkanAl set<INT> ;"
        // };

        // String[] validationCommandsList2 = {
        // "SELECT UCTCKyMQZwcolZRRsOwT, y, QMMI, JptplUFOay, TKljkanAl,
        // cAPh, Zba, JJZdN, znIkGzEuT FROM
        // uuidb0fa2c07e69b4a679dc6725e8697fc91.QMMI WHERE cAPh =
        // 'rpoHvlFcAsdcvNElBCpoXMpnOnnBupzTNtArrLEthXKNbDnqMYOQCXPOuozalwfKAdXlOHiaVYrFBPKLVxEBvlquSGOqSApexrdZBUtejrARzXPMqtcOljkPzXQbDxnhDzPkhQvdAczgjhImChKsaEmVAtgKWufWOUxCQhLfDEeLLjLzKQRmUWhQyoPaowwpjJHuTUGcwpFNsiWQZeiFpDogwesfZzKuyhIwiAykwOlLCx'
        // AND y =
        // 'cwSKgnEeKGQHIqbJnXGZZVEaGJqbPKkOGyGXWrxwUwwNMRXfmqHGoBIhIGOrUcXwWOvpXIANQFhETriSMpZefgflbwyvKHLhQaxwSfsRaCJCxPvqGSbxjnRLyBqUVmrAfwERnrqhzUGGGGOWnvbUulvPKMqUdpzqvhjihlkFbFpsuaozAlbilrLY'
        // AND UCTCKyMQZwcolZRRsOwT = 'hDuATCtjOoQcXvPufnZXugTqoPIRsqnuifvp'
        // ORDER BY y ASC;",
        // "SELECT JJZdN, UCTCKyMQZwcolZRRsOwT, cAPh, TKljkanAl, JptplUFOay,
        // Zba, OZXBDHHAL FROM uuidb0fa2c07e69b4a679dc6725e8697fc91.QMMI
        // WHERE cAPh =
        // 'ONklQQIzEUdtjqFXiiICCFxCgwdHLYTGpXOmrcAJEcQvtHcDePpYNfbGVEQJORSdIUKFWOhNGohrGlvhNbNVtazXBIkvYcHkBKPxMDhjimtoli'
        // ORDER BY y DESC;",
        // "SELECT OZXBDHHAL, y, cAPh FROM
        // uuidb0fa2c07e69b4a679dc6725e8697fc91.QMMI WHERE cAPh =
        // 'IQokwRsRfWuSQyihOexAkPgicoaSD' AND y =
        // 'xpgAfIXYJhpGUqgzObWggnUrkrpwUjhBsTbqymZEQluosmrsMtXfvogGVMkAHcAenFFfDUALIrcXPUEGbtiArksOUQtmOYgBEUCgtbilDuuqIMEcUfTJSjYYlHBPaIWfJqHoKOZQTCtlLVnPSSNowocQQpcNvnAMJEvPAiTKbLIJeDNrNgwgmoouVWBTQObQgzmBHGaYShUULLunlcJSEywRVGMKCwu'
        // AND UCTCKyMQZwcolZRRsOwT =
        // 'BcceMDyJMMwRbqWplXPeuMlIOkWoTwWNMVFuezCwqkleXJSyibTWVNsIjtbbRqOMxOylbGxsQDwkYioWNtRPOtKJSoooRAiGKmqsrJiWSjLCRXrNkYZVrNKabOjsnwxaFFfDTrOHHvLAuCrOoMgMGBEPaBNuM'
        // ORDER BY y DESC;",
        // "SELECT JJZdN FROM uuidb0fa2c07e69b4a679dc6725e8697fc91.QMMI
        // WHERE cAPh =
        // 'FQlTaZGmRvxWzuccowrrgdtkgJiOCnWxOfOmuukwFJBCBEkZeaXlADZpJpswFQoTCVNNnyj'
        // AND y = 'wXE' AND UCTCKyMQZwcolZRRsOwT =
        // 'BeCzSTBXirTVhUgGFNPCsdiloYEahrfQVGClTHJalB';",
        // "SELECT cAPh, JJZdN, TKljkanAl, UCTCKyMQZwcolZRRsOwT, JptplUFOay,
        // QMMI, OZXBDHHAL, y, znIkGzEuT FROM
        // uuidb0fa2c07e69b4a679dc6725e8697fc91.QMMI;",
        // "SELECT Zba, UCTCKyMQZwcolZRRsOwT, znIkGzEuT FROM
        // uuidb0fa2c07e69b4a679dc6725e8697fc91.QMMI;",
        // "SELECT cAPh, UCTCKyMQZwcolZRRsOwT, Zba, znIkGzEuT FROM
        // uuidb0fa2c07e69b4a679dc6725e8697fc91.QMMI;",
        // "SELECT y, UCTCKyMQZwcolZRRsOwT FROM
        // uuidb0fa2c07e69b4a679dc6725e8697fc91.QMMI WHERE cAPh =
        // 'dLtlNYdbujEXjJylvHENctHRtssfdiFnpvmyVzBKcbUunCKIvlwavHGLEofAgIVkSIjYcoIfRMRGKGXgfSqSvuLOAvxJtIwbmKzHiVjVpDlROHBUEeyloemClOmuAKuhUojrLpn'
        // AND y =
        // 'WtGscttHzsnQKvOSfdTYHkGNjMdJCcqZmcrhFUfAZvrBgVSafgnlFnWMkcelUCKPrnmJGIlBntJWLVnvjZmnSdvRyPfIltNxrHOaTOfriAGsrTuRalJvwSzEBOpONbwxHIVGUMCpdAjfZDWR'
        // AND UCTCKyMQZwcolZRRsOwT =
        // 'RjsxWeNqwPRNsxJNluSoCgvxnmvtSijaDzsnqjlFJWEEKUXXPJPhRgtfidRyhjRvRTqAheGnLjWRLcCjbzINNamFRsJgFjPQTXwBrVomLZkVXWUHhfaBWLGXvfXfEIdPnELBkZHkYFOvQikOmSBpEjqhoEeSckHPKtySUGAjfOnMZrmvdmVovOAndBqCGsezCeTInjKNDEwbCXiYnRROWLiBmdBiaMUBecnQsGwE'
        // ORDER BY y DESC;",
        // "SELECT y, JJZdN, znIkGzEuT, OZXBDHHAL, cAPh FROM
        // uuidb0fa2c07e69b4a679dc6725e8697fc91.QMMI WHERE cAPh =
        // 'MyJVcXMGXewQINOhhxqEkscubxvkjDXqgbuBWgxSjdsvmOyHWJiOtgjDnzIBtXEtGOSGuITQPTkTinooCEjKYMiPsiwGrZIhKPNrHdfYLQEKUWcoUfdqvnYnbZVrCfRhuVTigkwIWPoyQgmlGtIQxflpjhAvROWdksIxHBmiNQALLyMpVXAxqPGLVpJfQLgJIdnosYulTMHrnyvHkkwUirSltCgzXMuugkqBBiUeZXDAiRNnclqjiquZmHRXy'
        // AND y =
        // 'TOVCaPnaISZjXqsmzjDRdgjBBcNPSSQqioVEJQQPoYUkXviPChjZxtYuPbXPqtdoyDKcEuRBtWFskEyKszyNjfLzNghflVsfDVkhWUKmoGBFtNSVlZoMzZnXzFoPyvKGhQoTEfPsuacWLBzypzKGkTOcZUfkyOjmRex'
        // ORDER BY y DESC;",
        // "SELECT * FROM uuidb0fa2c07e69b4a679dc6725e8697fc91.QMMI WHERE
        // cAPh =
        // 'yrMbCQaUVupOeuekzjACCGPJJhBoyuDFjLEBOkFPDppIJKAUrdfScSfhXLIHVfMXaiAzXafdYzXSRCVZfoaTgLvPzXuFkmDwMuecZLXgchehnRTkzAXhYtZsdXXGGTotABflUzb'
        // ORDER BY y ASC;",
        // "SELECT QMMI, OZXBDHHAL, JptplUFOay FROM
        // uuidb0fa2c07e69b4a679dc6725e8697fc91.QMMI WHERE cAPh =
        // 'jttJKjamrJXgLcXBpzYcceQnrFrLHjkznwtMUKPMrVbSDfexffbXtNSfKMGYaYpxdDsRwbeeWZyDDdhEeGmLehYpjmKiwnGtayZUIekVBCrLqGKMxftDVpCJZTGtXIxelpPiyoEogUYiCPrlpJAjhatMRboVlFVyHYnxQoSciHPcaTkFGCmTJLjSSiqKgRPdLnJHlrSpfHZOmyPejUYAjoyfpkkNSRdxqyJfoElO'
        // AND y = 'vyWJtHRfyrKpouqfRTDseowSjcGroRzXqpwYyTYgXnVeoGuqM';",
        // "SELECT UCTCKyMQZwcolZRRsOwT, TKljkanAl, Zba, OZXBDHHAL, y FROM
        // uuidb0fa2c07e69b4a679dc6725e8697fc91.QMMI WHERE cAPh =
        // 'knsxgfYJLOgWuXbhkmZZtXtmjkhXzqqMInfxnhgUiPIScqsdUeNhWHydrLmFMNYHYdAPPbXAoeMQFypzuSdhXUwmGaZclrYoOLIWRdQvHLeYjDeJngpcCzJtiCQHGYmFoztOMuACAqELEheJDihyekUCZppWAzJEXvMjcPCtrDQYkvhNOLYEwuzoMleONlSuSJOuugvFbicfdFUaojo'
        // AND y = 'mVOlqvkSTpTpymWMbKUQNAVULnLV' AND UCTCKyMQZwcolZRRsOwT =
        // 'XUtpvrFTjCeqbkgzLbsssBQrItwKACuvjpjzXUiGTEWzAHYXyLjzQITnxJyJdHyAOlVTgnuZBaXureAMbcQVTQZugGWZuAkjqUxApIdwjMEIPnCbTnlnAfo';",
        // "SELECT JptplUFOay, y, cAPh FROM
        // uuidb0fa2c07e69b4a679dc6725e8697fc91.QMMI WHERE cAPh =
        // 'kNwjXgHXWjkwDdpQILNMfrNHaZEZkXcmMOfztsGsotShnLwbPdvStBysxB' AND
        // y =
        // 'UUvfLVsxCdIbBXOxXGoVMRcmqyWcaumPZCeltQsUrPWFyYbCdipPLHpaEavQpsAQZeHbaErmD'
        // AND UCTCKyMQZwcolZRRsOwT =
        // 'DVbTDsFKTYjnmGWyhdOFpRQOfnPxrfQyqZFlEcem' AND JJZdN =
        // 'ALJkDNqOIpZbdvOnZSYnjsLQCTvgKQiaqlSfRBpFMrTtxcoByXEvpQNWoyOPlhJRweGlcyPYMTKWIHZFtWmdOXKeAvhIhKB';",
        // "SELECT UCTCKyMQZwcolZRRsOwT, cAPh, OZXBDHHAL, znIkGzEuT, y,
        // QMMI, JJZdN FROM uuidb0fa2c07e69b4a679dc6725e8697fc91.QMMI WHERE
        // cAPh =
        // 'TKoaMHQVxuhmyqtfYShifSJZlHViNwSGbyikpichHGTCnHeubDiRJvhhWXGdQhDwdAeFkPltviIZwpozGELYLvPUnwGygOmaBsREeLMXmdagwJOWwhFoaOemgSfMpuELfnciSlIiDEsWfPFrayNcUfeqHlWzsCRlustCJHvRMZqkKuMCzPp'
        // AND y = 'fTAdIEajfapGzxccfsGmOILUwFkhHwNhEgFc';",
        // "SELECT UCTCKyMQZwcolZRRsOwT, y, JptplUFOay, znIkGzEuT,
        // OZXBDHHAL, cAPh, QMMI, JJZdN FROM
        // uuidb0fa2c07e69b4a679dc6725e8697fc91.QMMI;",
        // "SELECT cAPh, UCTCKyMQZwcolZRRsOwT, OZXBDHHAL FROM
        // uuidb0fa2c07e69b4a679dc6725e8697fc91.QMMI WHERE cAPh =
        // 'ObYEJXGwJdMGxSrCwNREgLUMehxDpXsFVENLsJFzRctUFFtgfkxTDSzhcDstsRANdqhktvIuidgCQrzDlCHhkRWQZYvWkDON'
        // AND y =
        // 'BfhuiKpfMufxugRyvPmkpSLTmkDDkablwIrPcyQixwinYxzCjqzXlyrqKdZnEWabuynibgaCMSZQcGdTRKMaPmVnJBaDAjYoMqzGeLJBYxcQJpwkdQXNgOlGTXNbPRVviPmQaYrZJUAHjbEjGQQPMOnAsIWViMRC'
        // AND UCTCKyMQZwcolZRRsOwT =
        // 'DQkTMihcijbwPgUkEtWlJDbnNmdkIvmGBbVkGroYtmFydVdGMOFoNQehEIjBwJNURCrCKEIKzUmAuTQXtxcPYkbUGqGcTeMmQOaKfXLfVzkFFFvBjPVWLDlwFPerPIwvuDnVhAxuuLloPwjVyUBpmalsGEBcUSUMDtgupywsoCgzzujJuEFMIobEmlhigQaQeYXMiJKsOyddGdAtbGMPpBOBPGUwmH'
        // AND JJZdN =
        // 'wqyKJdHjgczsdFtbyoTEUnhmTfZGWfZpOKUFXNGsIaSGXXaANylWhPqZYmqBAgsjioIfnJpLHyZnSXLITpNMp';",
        // "SELECT OZXBDHHAL, UCTCKyMQZwcolZRRsOwT FROM
        // uuidb0fa2c07e69b4a679dc6725e8697fc91.QMMI WHERE cAPh =
        // 'YvnEUbTIDekDJMJeawGaUgmluZTFXCgQzjSmyDatfPiPNmTDzJQNXYZwtnjNjsMSeqfDHwGZHoghQBWHiItnfWsCsVAicCstW'
        // AND y = 'SUr';"
        // };
        // List<String> originalCommandSequenceList = Arrays
        // .asList(commandSequenceList);
        return null;
    }

    public static List<List<String>> createExampleHbaseCommands() {
        String[] originalCommandSequence = {
                "create 'uuid40199938b7974c3fa73809905c796040', {NAME => 'uuid13ca032f27c347c58360951111a2cf44', VERSIONS => 3, COMPRESSION => 'GZ', BLOOMFILTER => 'ROWCOL', IN_MEMORY => 'true'}, {NAME => 'uuid383fbc0575e44bc0b1b6c904f1baf7aa', VERSIONS => 3, COMPRESSION => 'GZ', BLOOMFILTER => 'NONE', IN_MEMORY => 'false'}, {NAME => 'uuidd4302963ab75484eada3b82bd5c6f5c5', VERSIONS => 3, COMPRESSION => 'GZ', BLOOMFILTER => 'ROWCOL', IN_MEMORY => 'true'}, {NAME => 'uuid1f5bbeebb39f4976a2aa31072e63d7a7', VERSIONS => 3, COMPRESSION => 'GZ', BLOOMFILTER => 'ROWCOL', IN_MEMORY => 'false'}, {NAME => 'uuid72e1e39b18974a1fb79e9eeed04307c4', VERSIONS => 3, COMPRESSION => 'NONE', BLOOMFILTER => 'ROW', IN_MEMORY => 'false'}, {NAME => 'uuidbb9598272e4241c699b542ed55d24f6b', VERSIONS => 3, COMPRESSION => 'GZ', BLOOMFILTER => 'ROW', IN_MEMORY => 'true'}",
                "create 'uuidb1d2edba9eef4dfc82df56555f05e13f', {NAME => 'uuid5ddbc6fe6ae040fcbf2bba8cf1e3547f', VERSIONS => 3, COMPRESSION => 'NONE', BLOOMFILTER => 'ROWCOL', IN_MEMORY => 'false'}, {NAME => 'uuid53c63b08e452488ab65f1765f22cfa3c', VERSIONS => 3, COMPRESSION => 'NONE', BLOOMFILTER => 'ROWCOL', IN_MEMORY => 'false'}, {NAME => 'uuid578f1550ce9540d39ef6929d8a50394f', VERSIONS => 3, COMPRESSION => 'NONE', BLOOMFILTER => 'ROWCOL', IN_MEMORY => 'true'}, {NAME => 'uuidd9f725cef4634ee2b3e0d29ad028627c', VERSIONS => 3, COMPRESSION => 'NONE', BLOOMFILTER => 'NONE', IN_MEMORY => 'false'}, {NAME => 'uuida044b7c92e9c405dbed9120e1e4a56ce', VERSIONS => 1, COMPRESSION => 'GZ', BLOOMFILTER => 'ROW', IN_MEMORY => 'true'}, {NAME => 'uuid16a2fe69618c44ff91f156971c9c5693', VERSIONS => 1, COMPRESSION => 'NONE', BLOOMFILTER => 'ROW', IN_MEMORY => 'true'}",
                "create 'uuid8d085b3437c34e049fadf77f6942c612', {NAME => 'uuid05b02fd6c1904b639ff773648ea01bc2', VERSIONS => 3, COMPRESSION => 'NONE', BLOOMFILTER => 'ROWCOL', IN_MEMORY => 'true'}, {NAME => 'uuidfa6a5a65a34a463daf3e3af680997a65', VERSIONS => 1, COMPRESSION => 'NONE', BLOOMFILTER => 'NONE', IN_MEMORY => 'true'}, {NAME => 'uuid669521c776ba4676bf0c451fefafe492', VERSIONS => 1, COMPRESSION => 'GZ', BLOOMFILTER => 'ROWCOL', IN_MEMORY => 'true'}, {NAME => 'uuid5de32a51c3f046ec921099e494a5a210', VERSIONS => 3, COMPRESSION => 'NONE', BLOOMFILTER => 'ROWCOL', IN_MEMORY => 'true'}, {NAME => 'uuid76b359885d2c480c8048b6249f218a13', VERSIONS => 1, COMPRESSION => 'NONE', BLOOMFILTER => 'ROW', IN_MEMORY => 'false'}, {NAME => 'uuid94827bb96409447aa23c008786e718f4', VERSIONS => 1, COMPRESSION => 'GZ', BLOOMFILTER => 'ROWCOL', IN_MEMORY => 'false'}",
                "clone_table_schema 'uuid8d085b3437c34e049fadf77f6942c612', 'uuidefd27eadbce74e27966ae4d63a7e8d80'",
                "set_quota TYPE => THROTTLE, THROTTLE_TYPE => READ, TABLE => 'uuidefd27eadbce74e27966ae4d63a7e8d80', LIMIT => '1124M/sec'",
                "create 'uuidb63ed4017c8947388480f6bb6a77c78d', {NAME => 'uuidb4215f8f99bb4fe9874465e00dd3fb2a', VERSIONS => 3, COMPRESSION => 'GZ', BLOOMFILTER => 'ROW', IN_MEMORY => 'false'}, {NAME => 'uuida2b5150ec6e04ad4b0f084c541131999', VERSIONS => 3, COMPRESSION => 'NONE', BLOOMFILTER => 'ROW', IN_MEMORY => 'false'}, {NAME => 'uuida1bbf0389e7f43e49c8b9c826eb71c0e', VERSIONS => 1, COMPRESSION => 'NONE', BLOOMFILTER => 'NONE', IN_MEMORY => 'false'}, {NAME => 'uuid7f39aa8cbe3747bdba80cf3db95ad40c', VERSIONS => 1, COMPRESSION => 'GZ', BLOOMFILTER => 'NONE', IN_MEMORY => 'false'}, {NAME => 'uuideac7f8f636d641dfb3c81c7e911ce2c9', VERSIONS => 3, COMPRESSION => 'GZ', BLOOMFILTER => 'ROWCOL', IN_MEMORY => 'false'}, {NAME => 'uuid319d8c9fb9324c19a8b164ef946aa744', VERSIONS => 2, COMPRESSION => 'GZ', BLOOMFILTER => 'NONE', IN_MEMORY => 'false'}",
                "disable_rpc_throttle",
                "truncate_preserve 'uuid8d085b3437c34e049fadf77f6942c612'",
                "put 'uuid40199938b7974c3fa73809905c796040', 'uuidf43def325bcf408998b1960edb13a5dd', 'uuidd4302963ab75484eada3b82bd5c6f5c5:ywLPilkA', {'dwQPqYylseMFrVTVnlaYbtZFuqsYYxqAfoeNBxoflOFrVhmXKTuJfcZJpwcwcRvZg','JjkjGGNBecMwhnwYlnltykuWZyShUYGfOrsMDMgbEbYstcrogGLdunbApqQD','UukOakPTtmtowlNJioCwhcvEfHpnzXurGGKYpAPQWYsDPIXKqpxgfcedAsNHfMTyegfbIgTZshVRPFVMHaoDvaWOzeaiQRGDGxGmRJtQGGfXrcytuvkwMknVZuYygjeuMEzyTkdbIpXEEMDTOFMcHMZNgPEMbUtTqxLLMvVZGFPGCrUhSjwP','xaJZpZvdxwxwXdCDyfenLKEMtrxJcFHaeBQUDFmPIXFOQNQEOmMfpyetCOfEsZWLzMzPDpIksMREnLZ','qPSUDzejdtTWThqVBytMrtuJFrbItpuqFytFkSHrAPfSqGbNdPauuBWsydYDaxe'}",
                "enable_rpc_throttle",
                "alter 'uuidb63ed4017c8947388480f6bb6a77c78d', {NAME => 'uuid319d8c9fb9324c19a8b164ef946aa744', VERSIONS => 4, IN_MEMORY => true}",
                "clone_table_schema 'uuid40199938b7974c3fa73809905c796040', 'uuid332c6cd0da6244d0ad4bdeefa0444997'",
                "disable 'uuid40199938b7974c3fa73809905c796040'",
                "enable 'uuid8d085b3437c34e049fadf77f6942c612'",
                "disable 'uuidb63ed4017c8947388480f6bb6a77c78d'",
                "drop 'uuidb1d2edba9eef4dfc82df56555f05e13f'",
                "alter 'uuid40199938b7974c3fa73809905c796040', {NAME => 'rGmQNKiPm', VERSIONS => 2, COMPRESSION => 'NONE'}"
        };

        String[] validationCommandSequence = {
                "is_disabled 'uuidb63ed4017c8947388480f6bb6a77c78d'",
                "exists 'uuid40199938b7974c3fa73809905c796040'",
                "scan 'uuidefd27eadbce74e27966ae4d63a7e8d80'",
                "exists 'uuid40199938b7974c3fa73809905c796040'",
                "count 'uuid332c6cd0da6244d0ad4bdeefa0444997', INTERVAL => 225, CACHE => 225",
                "get_splits 'uuid332c6cd0da6244d0ad4bdeefa0444997'",
                "scan 'uuid8d085b3437c34e049fadf77f6942c612'",
                "scan 'uuidefd27eadbce74e27966ae4d63a7e8d80'",
                "scan 'uuid40199938b7974c3fa73809905c796040'",
                "list_quota_table_sizes",
                "get_splits 'uuid40199938b7974c3fa73809905c796040'",
                "is_disabled 'uuidefd27eadbce74e27966ae4d63a7e8d80'",
                "is_disabled 'uuidb63ed4017c8947388480f6bb6a77c78d'",
                "get 'uuid40199938b7974c3fa73809905c796040', 'uuidf43def325bcf408998b1960edb13a5dd', {COLUMN => 'uuidd4302963ab75484eada3b82bd5c6f5c5:ywLPilkA', VERSIONS => 1}",
                "balance_switch",
                "balance_switch",
                "is_disabled 'uuidefd27eadbce74e27966ae4d63a7e8d80'",
                "get_splits 'uuid40199938b7974c3fa73809905c796040'",
                "scan 'uuid8d085b3437c34e049fadf77f6942c612'",
                "get 'uuid40199938b7974c3fa73809905c796040', 'uuidf43def325bcf408998b1960edb13a5dd', {COLUMN => 'uuidd4302963ab75484eada3b82bd5c6f5c5:ywLPilkA', VERSIONS => 1}",
                "is_disabled 'uuid40199938b7974c3fa73809905c796040'",
                "scan 'uuidefd27eadbce74e27966ae4d63a7e8d80'",
                "list_quotas",
                "get_splits 'uuid40199938b7974c3fa73809905c796040'",
                "scan 'uuidefd27eadbce74e27966ae4d63a7e8d80'",
                "exists 'uuid332c6cd0da6244d0ad4bdeefa0444997'",
                "count 'uuidb63ed4017c8947388480f6bb6a77c78d'",
                "get_splits 'uuidb63ed4017c8947388480f6bb6a77c78d'",
                "scan 'uuid332c6cd0da6244d0ad4bdeefa0444997'",
                "get_splits 'uuidefd27eadbce74e27966ae4d63a7e8d80'"
        };

        List<String> originalCommandSequenceList = Arrays
                .asList(originalCommandSequence);
        List<String> validationCommandSequenceList = Arrays
                .asList(validationCommandSequence);

        List<List<String>> commandSequencesList = new ArrayList<>();
        commandSequencesList.add(originalCommandSequenceList);
        commandSequencesList.add(validationCommandSequenceList);
        return commandSequencesList;
    }

    public static void serializeSingleCommand(String cmd, OutputStream os)
            throws IOException {
        byte[] cmdBytes = cmd.getBytes(StandardCharsets.UTF_8);
        int cmdLen = cmdBytes.length;
        ByteBuffer buffer = ByteBuffer.allocate(4); // Integer.SIZE / Byte.SIZE
        buffer.putInt(cmdLen);
        byte[] lengthBytes = buffer.array();
        os.write(lengthBytes);
        os.write(cmdBytes);

        os.flush();
    }

    public static String deserializeSingleCommandResult(DataInputStream dis)
            throws IOException {
        int readLength = dis.readInt();
        byte[] messageBytes = new byte[readLength];
        dis.readFully(messageBytes);
        return new String(messageBytes);
    }

    public static int pickWeightedRandomChoice(double[] cumulativeProbabilities,
            double r) {
        // Determine which choice to pick
        for (int i = 0; i < cumulativeProbabilities.length; i++) {
            if (r <= cumulativeProbabilities[i]) {
                return i;
            }
        }
        // Fallback (should never happen if probabilities sum to 1)
        assert false : "the accumulated probability should sum to 1.0.";
        return cumulativeProbabilities.length - 1;
    }

    public static void copyDir(Path sourceDir, Path targetDir)
            throws IOException {
        try {
            FileUtils.copyDirectory(sourceDir.toFile(), targetDir.toFile());
        } catch (IOException e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void createDirIfNotExist(Path dir) {
        if (!dir.toFile().exists()) {
            boolean status = dir.toFile().mkdirs();
            if (!status) {
                throw new RuntimeException("Cannot create corpusDir: " + dir);
            }
        }
    }

    public static void sleepAndExit(int sleepTime) {
        try {
            Thread.sleep(sleepTime * 1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("[Debugging Mode] System exit");
        System.exit(1);
    }

    public static <T> Set<T> subSet(Set<T> targetSet) {
        // if size is 0, return empty set
        if (targetSet.size() == 0) {
            return new HashSet<>();
        }
        List<T> targetSetList = new ArrayList<>(targetSet);
        int numItems = rand.nextInt(targetSetList.size());
        Set<T> subSet = new HashSet<>();
        for (int i = 0; i < numItems; i++) {
            int randomIndex = rand.nextInt(targetSetList.size());
            subSet.add(targetSetList.get(randomIndex));
            targetSetList.remove(randomIndex);
        }
        return subSet;
    }

    public static void filterForwardReadFor14803(
            CommandSequence validationCommandSequence) {
        List<Command> newCommands = new LinkedList<>();
        for (Command command : validationCommandSequence.commands) {
            if (command.constructCommandString().contains(" DESC")) {
                newCommands.add(command);
            }
        }
        validationCommandSequence.commands = newCommands;
    }

    public static class ExponentialProbabilityModel {
        private final double c; // Initial probability
        private final double k; // Decay constant

        public ExponentialProbabilityModel(double c, double targetProbability,
                int targetN) {
            this.c = c;
            this.k = -Math.log(targetProbability / c) / targetN;
        }

        public double calculateProbability(int N) {
            return c * Math.exp(-k * N);
        }
    }

    // json: load map from a file
    public static Map<String, Map<String, String>> loadMapFromFile(
            Path filePath) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(filePath.toFile(), Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Map<String, Set<String>> loadStringMapFromFile(
            Path filePath) {
        // Read the map from the JSON file
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, Set<String>> mapFromFile = objectMapper.readValue(
                    filePath.toFile(),
                    new TypeReference<Map<String, Set<String>>>() {
                    });
            return mapFromFile;
        } catch (IOException e) {
            System.err.println(
                    "Exception happen when loading output from " + filePath);
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Map<String, String>> computeMF(
            Map<String, Map<String, String>> oriClassInfo,
            Map<String, Map<String, String>> upClassInfo) {
        assert oriClassInfo != null && upClassInfo != null;
        // classname -> fieldName -> fieldType
        // Compute intersection (classname, fieldName and fieldType are the
        // same)
        Map<String, Map<String, String>> mf = new HashMap<>();
        for (String className : oriClassInfo.keySet()) {
            if (upClassInfo.containsKey(className)) {
                Map<String, String> oriFieldInfo = oriClassInfo.get(className);
                Map<String, String> upFieldInfo = upClassInfo.get(className);
                Map<String, String> commonFieldInfo = new HashMap<>();
                for (String fieldName : oriFieldInfo.keySet()) {
                    if (upFieldInfo.containsKey(fieldName)) {
                        String oriFieldType = oriFieldInfo.get(fieldName);
                        String upFieldType = upFieldInfo.get(fieldName);
                        if (oriFieldType.equals(upFieldType)) {
                            commonFieldInfo.put(fieldName, oriFieldType);
                        }
                    }
                }
                if (!commonFieldInfo.isEmpty()) {
                    mf.put(className, commonFieldInfo);
                }
            }
        }
        return mf;
    }

    public static Set<String> computeChangedClasses(
            Map<String, Map<String, String>> oriClassInfo,
            Map<String, Map<String, String>> upClassInfo) {
        assert oriClassInfo != null && upClassInfo != null;
        Set<String> changedClasses = new HashSet<>();
        // classname -> (fieldName, fieldType)
        // For each class, check whether all fields' name and type are the same
        // Otherwise, include it in the return set
        for (String className : oriClassInfo.keySet()) {
            if (!upClassInfo.containsKey(className)
                    || oriClassInfo.get(className).size() != upClassInfo
                            .get(className).size()) {
                changedClasses.add(className);
                continue;
            }
            Map<String, String> oriFieldInfo = oriClassInfo.get(className);
            Map<String, String> upFieldInfo = upClassInfo.get(className);
            // check whether all fields are the same
            for (String fieldName : oriFieldInfo.keySet()) {
                if (!upFieldInfo.containsKey(fieldName) || !oriFieldInfo
                        .get(fieldName).equals(upFieldInfo.get(fieldName))) {
                    changedClasses.add(className);
                    break;
                }
            }
        }
        return changedClasses;
    }

    public static Map<String, Map<String, String>> computeMFUsingModifiedFields(
            Map<String, Map<String, String>> oriClassInfo,
            Map<String, Set<String>> modifiedFields) {
        assert oriClassInfo != null && modifiedFields != null;
        // Extract all field declaration that's not in modifiedFields
        Map<String, Map<String, String>> mf = new HashMap<>();
        for (String className : oriClassInfo.keySet()) {
            // replace $ with . in className
            String classNameReplaced = className.replace("$", ".");
            if (modifiedFields.containsKey(classNameReplaced)) {
                Map<String, String> oriFieldInfo = oriClassInfo.get(className);
                Set<String> modFieldInfo = modifiedFields
                        .get(classNameReplaced);

                // Find all fields that only exist in oriFieldInfo
                Map<String, String> commonFieldInfo = new HashMap<>();
                for (String fieldName : oriFieldInfo.keySet()) {
                    if (!modFieldInfo.contains(fieldName)) {
                        commonFieldInfo.put(fieldName,
                                oriFieldInfo.get(fieldName));
                    }
                }
                if (!commonFieldInfo.isEmpty()) {
                    mf.put(className, commonFieldInfo);
                }
            } else {
                // This means it's not modified at all
                mf.put(className, oriClassInfo.get(className));
            }
        }
        return mf;
    }

    public static Set<String> computeChangedClassesUsingModifiedFields(
            Map<String, Map<String, String>> oriClassInfo,
            Map<String, Set<String>> modifiedFields) {
        assert oriClassInfo != null && modifiedFields != null;
        Set<String> changedClasses = new HashSet<>();
        for (String className : oriClassInfo.keySet()) {
            // replace $ with . in className
            String classNameReplaced = className.replace("$", ".");
            if (modifiedFields.containsKey(classNameReplaced)) {
                Map<String, String> oriFieldInfo = oriClassInfo.get(className);
                Set<String> modFieldInfo = modifiedFields
                        .get(classNameReplaced);
                // Check whether there's an intersection
                for (String fieldName : oriFieldInfo.keySet()) {
                    if (modFieldInfo.contains(fieldName)) {
                        // An intersection => the class is changed
                        changedClasses.add(className);
                        break;
                    }
                }
            }
        }
        return changedClasses;
    }

    public static void printMF(
            Map<String, Map<String, String>> matchableClassInfo) {
        // print matchableClassInfo
        logger.debug("Matchable Class Info:");
        for (Map.Entry<String, Map<String, String>> entry : matchableClassInfo
                .entrySet()) {
            logger.debug(entry.getKey() + ": " + entry.getValue());
        }
    }

    public static Map<String, Map<Integer, Set<SerializationInfo.MergePointInfo>>> loadDumpPoints(
            Path filePath) {
        // Read the map from the JSON file
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, Map<Integer, Set<SerializationInfo.MergePointInfo>>> mapFromFile = objectMapper
                    .readValue(filePath.toFile(),
                            new TypeReference<Map<String, Map<Integer, Set<SerializationInfo.MergePointInfo>>>>() {
                            });
            return mapFromFile;
        } catch (IOException e) {
            System.err.println(
                    "Exception happen when loading output from " + filePath);
            throw new RuntimeException(e);
        }
    }

    public static int count(
            Map<String, Map<String, String>> matchableClassInfo) {
        int count = 0;
        for (Map.Entry<String, Map<String, String>> entry : matchableClassInfo
                .entrySet()) {
            count += entry.getValue().size();
        }
        return count;
    }

    public static int countMergePoints(
            Map<String, Map<Integer, Set<SerializationInfo.MergePointInfo>>> objectMergePoints) {
        int count = 0;
        for (Map<Integer, Set<SerializationInfo.MergePointInfo>> mergePoints : objectMergePoints
                .values()) {
            for (Set<SerializationInfo.MergePointInfo> mergePointInfos : mergePoints
                    .values()) {
                count += mergePointInfos.size();
            }
        }
        return count;
    }

    public static Map<String, Map<String, String>> replaceDollarWithDot(
            Map<String, Map<String, String>> classInfo) {
        // Class => {fieldname, fieldtype}, only replace $ for classname
        Map<String, Map<String, String>> newClassInfo = new HashMap<>();
        for (Map.Entry<String, Map<String, String>> entry : classInfo
                .entrySet()) {
            String className = entry.getKey();
            // deep copy fields
            Map<String, String> fields = new HashMap<>();
            for (Map.Entry<String, String> fieldEntry : entry.getValue()
                    .entrySet()) {
                String fieldName = fieldEntry.getKey();
                String fieldType = fieldEntry.getValue();
                fields.put(fieldName, fieldType);
            }
            newClassInfo.put(className.replace("$", "."), fields);
        }
        return newClassInfo;
    }

    // concate variable length of strings with " "
    public static String concat(String... strings) {
        StringBuilder sb = new StringBuilder();
        for (String s : strings) {
            sb.append(s);
            sb.append(" ");
        }
        return sb.toString();
    }
}