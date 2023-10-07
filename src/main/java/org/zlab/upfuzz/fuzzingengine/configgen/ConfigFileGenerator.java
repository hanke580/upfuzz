package org.zlab.upfuzz.fuzzingengine.configgen;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

public abstract class ConfigFileGenerator {
    public Path generateFolderPath;
    public int fileNameIdx;

    public ConfigFileGenerator(Path generateFolderPath) {
        this.generateFolderPath = generateFolderPath;

        if (!generateFolderPath.toFile().exists())
            generateFolderPath.toFile().mkdirs();

        // update the fileNameIdx
        File[] files = generateFolderPath.toFile().listFiles();
        int max = -1;
        for (File file : files) {
            String fileName = file.getName();
            int idx = Integer.parseInt(fileName.substring(4));
            if (idx > max)
                max = idx;
        }
        fileNameIdx = max + 1;
    }

    // upgrade version
    public abstract int generate(Map<String, String> key2vals,
            Map<String, String> key2type,
            Map<String, String> newkey2vals,
            Map<String, String> newkey2type);

    // single version
    public abstract int generate(Map<String, String> key2vals,
            Map<String, String> key2type);

    // no mutation, just copy the config file
    public abstract int generate(boolean isUpgrade);

}
