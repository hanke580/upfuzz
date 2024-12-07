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

    // @Test
    public void printNonMatchableRef() {
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

        Map<String, Map<String, String>> matchableClassInfo = Utilities
                .computeMF(oriClassInfo, upClassInfo);

        // print all non-matchable references
        Map<String, Set<String>> modifiedFormatFields = new HashMap<>();
        for (Map.Entry<String, Map<String, String>> entry : oriClassInfo
                .entrySet()) {
            String className = entry.getKey();
            Map<String, String> oriFields = entry.getValue();
            for (String fieldName : oriFields.keySet()) {
                if (matchableClassInfo.containsKey(className)
                        && matchableClassInfo.get(className)
                                .containsKey(fieldName)) {
                    continue;
                }
                // System.out.println(className + "." + fieldName);
                modifiedFormatFields.computeIfAbsent(className,
                        k -> new java.util.HashSet<>()).add(fieldName);
            }
        }

        // Load modified fields...
        Map<String, Set<String>> modifiedFields = Utilities
                .loadStringMapFromFile(
                        configInfoFolder.resolve("modifiedFields.json"));

        // Diff between modifiedFields and modifiedFormatFields
        // Only print the ones that exists in modifiedFormatFields but not in
        // modifiedFields
        for (Map.Entry<String, Set<String>> entry : modifiedFormatFields
                .entrySet()) {
            String className = entry.getKey();
            Set<String> fields = entry.getValue();
            if (!modifiedFields.containsKey(className)) {
                // print all
                for (String field : fields) {
                    System.out.println(className + "." + field);
                }
                continue;
            }
            for (String field : fields) {
                if (!modifiedFields.get(className).contains(field)) {
                    System.out.println(className + "." + field);
                }
            }

        }

    }
}
