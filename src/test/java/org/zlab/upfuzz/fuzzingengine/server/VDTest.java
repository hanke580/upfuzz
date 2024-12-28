package org.zlab.upfuzz.fuzzingengine.server;

import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.utils.Utilities;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
        // print all non-matchable references
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
        Path configInfoFolder = Paths.get("configInfo")
                .resolve(originalVersion + "_" + upgradedVersion);

        Map<String, Map<String, String>> oriClassInfo = Utilities
                .loadMapFromFile(
                        oriFormatInfoFolder.resolve(
                                Config.getConf().baseClassInfoFileName));
        Map<String, Map<String, String>> upClassInfo = Utilities
                .loadMapFromFile(
                        upFormatInfoFolder.resolve(
                                Config.getConf().baseClassInfoFileName));

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
                        configInfoFolder.resolve("modifiedFields.json"));
        Map<String, Set<String>> diffFields = diff1(modifiedFormatFields,
                modifiedFields);
        if (printDiff) {
            System.out.println("Diff:");
            int count = print(diffFields);
            System.out.println("Total: " + count);
        }

        //
    }
}
