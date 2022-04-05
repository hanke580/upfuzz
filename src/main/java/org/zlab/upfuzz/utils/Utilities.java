package org.zlab.upfuzz.utils;

import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;
import org.zlab.upfuzz.CommandSequence;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;

public class Utilities {
    public static List<Integer> permutation(int size) {
        List<Integer> indexArray = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            indexArray.add(i);
        }
        Collections.shuffle(indexArray);
        return indexArray;
    }

    public static CommandSequence deserializeCommandSequence(Path filePath) {
        CommandSequence commandSequence = null;
        try {
            FileInputStream fileIn = new FileInputStream(filePath.toFile());
            ObjectInputStream in = new ObjectInputStream(fileIn);
            commandSequence = (CommandSequence) in.readObject();
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
        return commandSequence;
    }

    public static boolean hasNewBits(ExecutionDataStore curCoverage, ExecutionDataStore testSequenceCoverage) {

        if (testSequenceCoverage == null) return false;

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

    public static void assertCompatibility(ExecutionData curData, ExecutionData testSequenceData) {
        if (curData.getId() != testSequenceData.getId()) {
            throw new IllegalStateException(
                    format("Different ids (%016x and %016x).",
                            Long.valueOf(curData.getId()), Long.valueOf(testSequenceData.getId())));
        }
        if (!curData.getName().equals(testSequenceData.getName())) {
            throw new IllegalStateException(
                    format("Different class names %s and %s for id %016x.",
                            curData.getName(), testSequenceData.getName(), Long.valueOf(testSequenceData.getId())));
        }
        if (curData.getProbes().length != testSequenceData.getProbes().length) {
            throw new IllegalStateException(format(
                    "Incompatible execution data for class %s with id %016x.",
                    testSequenceData.getName(), Long.valueOf(testSequenceData.getId())));
        }
    }

    public static Process runProcess(ProcessBuilder pb, String desc) {
        Process p = null;
        try {
            System.out.println("Execute: " + desc);
            p = pb.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
                System.out.flush();
            }
            p.waitFor();
            in.close();
            System.out.println(desc + " Successful");

        } catch(IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return p;
    }


    // public static void runProcess(ProcessBuilder pb, String desc) {
    //     try {
    //         System.out.println("Execute: " + desc);
    //         Process p = pb.start();
    //         BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
    //         String line;
    //         while ((line = in.readLine()) != null) {
    //             System.out.println(line);
    //             System.out.flush();
    //         }
    //         p.waitFor();
    //         in.close();
    //         System.out.println(desc + " Successful");

    //     } catch(IOException | InterruptedException e) {
    //         e.printStackTrace();
    //     }
    // }

}
