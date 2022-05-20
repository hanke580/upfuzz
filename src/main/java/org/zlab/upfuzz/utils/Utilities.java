package org.zlab.upfuzz.utils;

import static java.lang.String.format;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;

import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;
import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.fuzzingengine.Config;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


public class Utilities {
    public static List<Integer> permutation(int size) {
        List<Integer> indexArray = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            indexArray.add(i);
        }
        Collections.shuffle(indexArray);
        return indexArray;
    }

    public static Pair<CommandSequence, CommandSequence> deserializeCommandSequence(Path filePath) {
        Pair<CommandSequence, CommandSequence> commandSequencePair = null;
        try {
            FileInputStream fileIn = new FileInputStream(filePath.toFile());
            ObjectInputStream in = new ObjectInputStream(fileIn);
            commandSequencePair = (Pair<CommandSequence, CommandSequence>) in.readObject();
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

    public static boolean hasNewBits(ExecutionDataStore curCoverage, ExecutionDataStore testSequenceCoverage) {

        if (testSequenceCoverage == null)
            return false;

        if (curCoverage == null) {
            curCoverage = testSequenceCoverage;
            return true;
        } else {
            for (final ExecutionData testSequenceData : testSequenceCoverage.getContents()) {

                final Long id = Long.valueOf(testSequenceData.getId());
                final ExecutionData curData = curCoverage.get(id);

                // For one class, merge the coverage
                if (curData != null) {
                    assertCompatibility(curData, testSequenceData);
                    int[] curProbes = curData.getProbes();
                    final int[] testSequenceProbes = testSequenceData.getProbes();
                    for (int i = 0; i < curProbes.length; i++) {
                        // Now only try with the boolean first
                        if (curProbes[i] == 0 && testSequenceProbes[i] != 0) {
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

    public static Pair<Integer, Integer> getCoverageStatus(ExecutionDataStore curCoverage) {
        if (curCoverage == null) {
            return new Pair(0, 0);
        }

        int coveredProbes = 0;
        int probeNum = 0;

        for (final ExecutionData curData : curCoverage.getContents()) {
            int[] curProbes = curData.getProbes();
            probeNum += curProbes.length;
            for (int i = 0; i < curProbes.length; i++) {
                if (curProbes[i] != 0) coveredProbes++;
            }
        }
        return new Pair(coveredProbes, probeNum);
    }



    public static void assertCompatibility(ExecutionData curData, ExecutionData testSequenceData) {
        if (curData.getId() != testSequenceData.getId()) {
            throw new IllegalStateException(format("Different ids (%016x and %016x).", Long.valueOf(curData.getId()),
                    Long.valueOf(testSequenceData.getId())));
        }
        if (!curData.getName().equals(testSequenceData.getName())) {
            throw new IllegalStateException(format("Different class names %s and %s for id %016x.", curData.getName(),
                    testSequenceData.getName(), Long.valueOf(testSequenceData.getId())));
        }
        if (curData.getProbes().length != testSequenceData.getProbes().length) {
            throw new IllegalStateException(format("Incompatible execution data for class %s with id %016x.",
                    testSequenceData.getName(), Long.valueOf(testSequenceData.getId())));
        }
    }

    public static Process runProcess(ProcessBuilder pb, String desc) {
        Process p = null;
        try {
            // System.out.println("Execute: " + desc);
            p = pb.start();
            // BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            // String line;
            // while ((line = in.readLine()) != null) {
            //     System.out.println(line);
            //     System.out.flush();
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
        Process p = pb.start();
        return p;
    }

    public static Process exec(String[] cmds, String path) throws IOException {
        return exec(cmds, new File(path));
    }

    public static String readProcess(Process p) {
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            p.waitFor();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static String getGitBranch(String path) throws IOException, InterruptedException {
        Process p = exec(new String[] { "git", "rev-parse", "--abbrev-ref", "HEAD" }, path);
        String gitBranch = readProcess(p).replace("\n", "");
        return gitBranch;
    }

    public static String getGitTag(String path) throws IOException, InterruptedException {
        Process p = exec(new String[] { "git", "describe", "--abbrev=0", "--tags", "HEAD" }, path);
        String gitBranch = readProcess(p).replace("\n", "");
        return gitBranch;
    }



    public static String getMainClassName() throws ClassNotFoundException {
        for (Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
            Thread thread = entry.getKey();
            // System.out.println(thread.getThreadGroup().getName() );
            if (thread.getThreadGroup() != null && thread.getThreadGroup().getName().equals("main")) {
                for (StackTraceElement stackTraceElement : entry.getValue()) {
                    // System.out.println(stackTraceElement.getClassName()+ " " + stackTraceElement.getMethodName() + " " + stackTraceElement.getFileName());
                    if (stackTraceElement.getMethodName().equals("main")) {

                        try {
                            Class<?> c = Class.forName(stackTraceElement.getClassName());
                            Class[] argTypes = new Class[] { String[].class };
                            //This will throw NoSuchMethodException in case of fake main methods
                            c.getDeclaredMethod("main", argTypes);
                            // return stackTraceElement.getClassName();
                        } catch (NoSuchMethodException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return null;
    }

    public static void clearCassandraDataDir() {

        // ProcessBuilder pb = new ProcessBuilder("sh", "clean.sh");
        // pb.directory(new File("/home/vagrant/project/upfuzz"));
        // try {
        //     Process p = pb.start();
        //     p.waitFor();
        // } catch (IOException | InterruptedException e) {
        //     e.printStackTrace();
        // }

        ProcessBuilder pb = new ProcessBuilder("rm", "-rf", "data");
        pb.directory(new File(Config.getConf().cassandraPath));
        try {
            pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        pb = new ProcessBuilder("rm", "-rf", "data");
        pb.directory(new File(Config.getConf().upgradeCassandraPath));
        try {
            pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public static boolean write2TXT(File file, String content) {

        try{
            // If file doesn't exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);

            // Write in file
            bw.write(content);

            // Close connection
            bw.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }

        return true;
    }

    public static boolean writeCmdSeq(File file, Object object) {
        try {
            FileOutputStream fileOut =
                    new FileOutputStream(file);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(object);
            out.close();
            fileOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }


    public static Map<String, List<Map.Entry<String, String>>> loadFunctoinFromStaticAnalysis(Path fileName) {
        JSONParser jsonParser = new JSONParser();
        Map<String, List<Map.Entry<String, String>>> funcToInst = new HashMap<>();

        try (FileReader reader = new FileReader(fileName.toFile()))
        {
            Object obj = jsonParser.parse(reader);
            JSONObject jsonObject = (JSONObject) obj;
            Set<Map.Entry<String, String>> entrySet = jsonObject.entrySet();

            for (Map.Entry<String, String> e : entrySet) {
                String[] ret = e.getValue().split(e.getKey().replace("$", "\\$") + "\\(");
                assert(ret.length == 2);
                String ClassName = ret[0].substring(0, ret[0].length() - 1);
                String MethodName = e.getKey();
                String ParamDesc = "(" + ret[1];

                // Only instrument class that's inside org.apache.cassandra.*
                if (ClassName.contains("cassandra")) {
                    if (funcToInst.containsKey(ClassName)) {
                        funcToInst.get(ClassName).add(new AbstractMap.SimpleEntry<>(MethodName, ParamDesc));
                    } else {
                        List<Map.Entry<String,String>> list = new ArrayList<>();
                        list.add(new AbstractMap.SimpleEntry<>(MethodName, ParamDesc));
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
        Map<String, List<Map.Entry<String, String>>> funcs = Utilities.loadFunctoinFromStaticAnalysis(filePath);
        for (String className: funcs.keySet()) {
            System.out.print(className + ":");
        }
        System.out.println();
    }

}
