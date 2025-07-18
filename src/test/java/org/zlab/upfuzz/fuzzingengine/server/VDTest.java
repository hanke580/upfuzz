package org.zlab.upfuzz.fuzzingengine.server;

import org.junit.jupiter.api.Test;
import org.zlab.ocov.Utils;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.utils.SerializationInfo;
import org.zlab.upfuzz.utils.Utilities;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.zlab.upfuzz.utils.Utilities.*;

public class VDTest {

    public static int print(Map<String, Set<String>> classInfo) {
        int count = 0;
        for (Map.Entry<String, Set<String>> entry : classInfo.entrySet()) {
            String className = entry.getKey();
            Set<String> fields = entry.getValue();
            for (String field : fields) {
                System.out.println(className + "." + field);
                count++;
            }
        }
        return count;
    }

    public static Map<String, Set<String>> diff1(
            Map<String, Set<String>> modifiedFields1,
            Map<String, Set<String>> modifiedFields2) {
        Map<String, Set<String>> modifiedFormatFields = new HashMap<>();
        // Extract fields that only exist in modifiedFields1
        for (Map.Entry<String, Set<String>> entry : modifiedFields1
                .entrySet()) {
            String className = entry.getKey();
            Set<String> fields = entry.getValue();
            if (!modifiedFields2.containsKey(className)) {
                modifiedFormatFields.computeIfAbsent(className,
                        k -> new java.util.HashSet<>()).addAll(fields);
                continue;
            }
            for (String field : fields) {
                if (!modifiedFields2.get(className).contains(field)) {
                    modifiedFormatFields.computeIfAbsent(className,
                            k -> new java.util.HashSet<>()).add(field);
                }
            }
        }
        return modifiedFormatFields;
    }

    public static Map<String, Set<String>> diff2(
            Map<String, Map<String, String>> classInfo1,
            Map<String, Map<String, String>> matchableClassInfo) {
        Map<String, Set<String>> modifiedFormatFields = new HashMap<>();
        for (Map.Entry<String, Map<String, String>> entry : classInfo1
                .entrySet()) {
            String className = entry.getKey();
            Map<String, String> oriFields = entry.getValue();
            for (String fieldName : oriFields.keySet()) {
                if (matchableClassInfo.containsKey(className)
                        && matchableClassInfo.get(className)
                                .containsKey(fieldName)) {
                    continue;
                }
                modifiedFormatFields.computeIfAbsent(className,
                        k -> new java.util.HashSet<>()).add(fieldName);
            }
        }
        return modifiedFormatFields;
    }

    // @Test
    public void run() {
        // For Debug
        new Config();

        String originalVersion = "apache-cassandra-2.2.19";
        String upgradedVersion = "apache-cassandra-3.0.30";

        Path oriFormatInfoFolder = Paths.get("configInfo")
                .resolve(originalVersion);
        Path upFormatInfoFolder = Paths.get("configInfo")
                .resolve(upgradedVersion);
        Path upgradeFormatInfoFolder = Paths.get("configInfo")
                .resolve(originalVersion + "_" + upgradedVersion);

        Map<String, Map<String, String>> oriClassInfo = Utilities
                .loadMapFromFile(
                        oriFormatInfoFolder.resolve(
                                Config.getConf().baseClassInfoFileName));
        Map<String, Map<String, String>> upClassInfo = Utilities
                .loadMapFromFile(
                        upFormatInfoFolder.resolve(
                                Config.getConf().baseClassInfoFileName));
        assert oriClassInfo != null;
        assert upClassInfo != null;

        // Replace all $ with .
        oriClassInfo = replaceDollarWithDot(oriClassInfo);
        upClassInfo = replaceDollarWithDot(upClassInfo);

        Map<String, Map<String, String>> matchableClassInfo = Utilities
                .computeMF(oriClassInfo, upClassInfo);

        // print all matchable references
        boolean printMatchable = false;
        if (printMatchable) {
            System.out.println("Matchable References:");
            for (Map.Entry<String, Map<String, String>> entry : matchableClassInfo
                    .entrySet()) {
                String className = entry.getKey();
                Map<String, String> fields = entry.getValue();
                for (Map.Entry<String, String> fieldEntry : fields.entrySet()) {
                    System.out.println(className + "." + fieldEntry.getKey());
                }
            }
        }

        boolean printNonMatchable = true;
        // print all non-matchable references
        Map<String, Set<String>> modifiedFormatFields = diff2(oriClassInfo,
                matchableClassInfo);
        // print it
        if (printNonMatchable) {
            System.out.println("Non-Matchable References:");
            int count = print(modifiedFormatFields);
            System.out.println("Total: " + count);
        }

        System.out.println();

        boolean printDiff = true;
        // Diff between static analysis and direct comparison
        Map<String, Set<String>> modifiedFields = Utilities
                .loadStringMapFromFile(
                        upgradeFormatInfoFolder.resolve("modifiedFields.json"));
        Map<String, Set<String>> diffFields = diff1(modifiedFormatFields,
                modifiedFields);
        if (printDiff) {
            System.out.println("Diff:");
            int count = print(diffFields);
            System.out.println("Total: " + count);
        }
    }

    // @Test
    public void testSrcVD() {
        new Config();

        // String originalVersion = "apache-cassandra-2.2.19";
        // String upgradedVersion = "apache-cassandra-3.0.30";
        String originalVersion = "hadoop-2.10.2";
        String upgradedVersion = "hadoop-3.3.6";

        Path oriFormatInfoFolder = Paths.get("configInfo")
                .resolve(originalVersion);
        Path upFormatInfoFolder = Paths.get("configInfo")
                .resolve(upgradedVersion);
        Path upgradeFormatInfoFolder = Paths.get("configInfo")
                .resolve(originalVersion + "_" + upgradedVersion);

        String fileName = Config
                .getConf().modifiedFieldsFileName;
        // String fileName =
        // Config.getConf().modifiedFieldsClassnameMustMatchFileName;
        // String fileName = Config.getConf().modifiedFieldsFileName;

        Path modifiedFieldsPath = upgradeFormatInfoFolder
                .resolve(fileName);
        Map<String, Set<String>> modifiedFields = Utils
                .loadModifiedFields(modifiedFieldsPath);

        Map<String, Map<String, String>> oriClassInfo = Utilities
                .loadMapFromFile(
                        oriFormatInfoFolder.resolve(
                                Config.getConf().baseClassInfoFileName));
        // count
        System.out.println("Original: " + count(oriClassInfo));

        Map<String, Map<String, String>> matchableClassInfo = Utilities
                .computeMFUsingModifiedFields(
                        Objects.requireNonNull(oriClassInfo),
                        modifiedFields);
        // count matchableClassInfo size
        System.out.println("Matchable: " + count(matchableClassInfo));

        // Find all non-matchable by comparing oriClassInfo and
        // matchableClassInfo
        Map<String, Set<String>> nonMatchableClassInfo = diff2(oriClassInfo,
                matchableClassInfo);
        // count nonMatchableClassInfo size
        int nonMatchableCount = 0;
        for (Map.Entry<String, Set<String>> entry : nonMatchableClassInfo
                .entrySet()) {
            nonMatchableCount += entry.getValue().size();
        }
        System.out.println("Non-Matchable: " + nonMatchableCount);

        // Print non-matchable
        boolean printNonMatchable = false;
        if (printNonMatchable) {
            for (Map.Entry<String, Set<String>> entry : nonMatchableClassInfo
                    .entrySet()) {
                String className = entry.getKey();
                Set<String> fields = entry.getValue();
                for (String field : fields) {
                    System.out.println(className + ":" + field);
                }
            }
        }

        Set<String> changedClasses = Utilities
                .computeChangedClassesUsingModifiedFields(
                        Objects.requireNonNull(Utilities
                                .loadMapFromFile(
                                        oriFormatInfoFolder.resolve(
                                                Config.getConf().baseClassInfoFileName))),
                        modifiedFields);
        // count changedClasses size
        System.out.println("Changed: " + changedClasses.size());

        boolean measureMergePoint = false;
        if (measureMergePoint) {
            // Identify merge points where objects are unmodified
            Path mergePointsFilePath = Paths.get(
                    "/PATH/TO/mergePoints_alg1.json");
            Map<String, Map<Integer, Set<SerializationInfo.MergePointInfo>>> mergePoints = Utilities
                    .loadDumpPoints(mergePointsFilePath);

            // count
            System.out
                    .println("Merge Points: " + countMergePoints(mergePoints));

            // extract: unchanged merge points
            Map<String, Map<Integer, Set<SerializationInfo.MergePointInfo>>> unchangedMergePoints = new HashMap<>();
            // Iterate merge points, print the one that's not in changedClasses
            for (Map.Entry<String, Map<Integer, Set<SerializationInfo.MergePointInfo>>> entry : mergePoints
                    .entrySet()) {
                String className = entry.getKey();
                Map<Integer, Set<SerializationInfo.MergePointInfo>> mergePointsInfo = entry
                        .getValue();
                for (Map.Entry<Integer, Set<SerializationInfo.MergePointInfo>> mergePointEntry : mergePointsInfo
                        .entrySet()) {
                    int lineNum = mergePointEntry.getKey();
                    Set<SerializationInfo.MergePointInfo> mergePointInfos = mergePointEntry
                            .getValue();
                    for (SerializationInfo.MergePointInfo mergePointInfo : mergePointInfos) {
                        if (!changedClasses
                                .contains(mergePointInfo.objectClassName)) {
                            unchangedMergePoints
                                    .computeIfAbsent(className,
                                            k -> new HashMap<>())
                                    .put(lineNum, mergePointInfos);
                        }
                    }
                }
            }
            // count
            System.out.println("Unchanged Merge Points: "
                    + countMergePoints(unchangedMergePoints));
            boolean printUnchangedMergePoints = false;
            if (printUnchangedMergePoints) {
                // print it
                for (Map.Entry<String, Map<Integer, Set<SerializationInfo.MergePointInfo>>> entry : unchangedMergePoints
                        .entrySet()) {
                    String className = entry.getKey();
                    Map<Integer, Set<SerializationInfo.MergePointInfo>> mergePointsInfo = entry
                            .getValue();
                    for (Map.Entry<Integer, Set<SerializationInfo.MergePointInfo>> mergePointEntry : mergePointsInfo
                            .entrySet()) {
                        int lineNum = mergePointEntry.getKey();
                        Set<SerializationInfo.MergePointInfo> mergePointInfos = mergePointEntry
                                .getValue();
                        for (SerializationInfo.MergePointInfo mergePointInfo : mergePointInfos) {
                            System.out.println(className + ":" + lineNum + " "
                                    + mergePointInfo.objectClassName);
                        }
                    }
                }
            }
        }
    }

    // @Test
    public void testBinaryVD() {
        new Config();

        String originalVersion = "apache-cassandra-2.2.19";
        String upgradedVersion = "apache-cassandra-3.0.30";

        Path oriFormatInfoFolder = Paths.get("configInfo")
                .resolve(originalVersion);
        Path upFormatInfoFolder = Paths.get("configInfo")
                .resolve(upgradedVersion);
        Path upgradeFormatInfoFolder = Paths.get("configInfo")
                .resolve(originalVersion + "_" + upgradedVersion);
        Path modifiedFieldsPath = upgradeFormatInfoFolder
                .resolve(Config.getConf().modifiedFieldsFileName);
        Map<String, Set<String>> modifiedFields = Utils
                .loadModifiedFields(modifiedFieldsPath);

        Map<String, Map<String, String>> oriClassInfo = Utilities
                .loadMapFromFile(
                        oriFormatInfoFolder.resolve(
                                Config.getConf().baseClassInfoFileName));
        Map<String, Map<String, String>> upClassInfo = Utilities
                .loadMapFromFile(upFormatInfoFolder.resolve(
                        Config.getConf().baseClassInfoFileName));

        Map<String, Map<String, String>> matchableClassInfo = Utilities
                .computeMF(
                        Objects.requireNonNull(oriClassInfo),
                        Objects.requireNonNull(upClassInfo));
        System.out.println("Matchable: " + count(matchableClassInfo));

        // Find all non-matchable by comparing oriClassInfo and
        // matchableClassInfo
        Map<String, Set<String>> nonMatchableClassInfo = diff2(oriClassInfo,
                matchableClassInfo);
        int nonMatchableCount = 0;
        for (Map.Entry<String, Set<String>> entry : nonMatchableClassInfo
                .entrySet()) {
            nonMatchableCount += entry.getValue().size();
        }
        System.out.println("Non-Matchable: " + nonMatchableCount);

        Set<String> changedClasses = Utilities.computeChangedClasses(
                Objects.requireNonNull(Utilities
                        .loadMapFromFile(
                                oriFormatInfoFolder.resolve(
                                        Config.getConf().baseClassInfoFileName))),
                Objects.requireNonNull(Utilities
                        .loadMapFromFile(upFormatInfoFolder.resolve(
                                Config.getConf().baseClassInfoFileName))));
        System.out.println("Changed: " + changedClasses.size());
    }
}
