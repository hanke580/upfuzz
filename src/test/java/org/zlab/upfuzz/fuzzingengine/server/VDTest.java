package org.zlab.upfuzz.fuzzingengine.server;

import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.utils.Utilities;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class VDTest {

    @Test
    public void printNonMatchableRef() {
        // For Debug
        new Config();

        String originalVersion = "apache-cassandra-2.2.19";
        String upgradedVersion = "apache-cassandra-3.0.30";

        Path oriFormatInfoFolder = Paths.get("configInfo")
                .resolve(originalVersion);
        Path upFormatInfoFolder = Paths.get("configInfo")
                .resolve(upgradedVersion);

        Map<String, Map<String, String>> oriClassInfo = Utilities
                .loadMapFromFile(
                        oriFormatInfoFolder.resolve(
                                Config.getConf().baseClassInfoFileName));
        Map<String, Map<String, String>> upClassInfo = Utilities
                .loadMapFromFile(
                        upFormatInfoFolder.resolve(
                                Config.getConf().baseClassInfoFileName));

        Map<String, Map<String, String>> matchableClassInfo = Utilities
                .computeMF(
                        oriClassInfo, upClassInfo);

        // print all non-matchable references
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
                System.out.println(className + "." + fieldName);
            }
        }
    }
}
