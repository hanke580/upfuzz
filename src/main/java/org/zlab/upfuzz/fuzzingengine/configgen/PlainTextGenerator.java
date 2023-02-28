package org.zlab.upfuzz.fuzzingengine.configgen;

import java.io.File;
import java.nio.file.Path;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import org.zlab.upfuzz.docker.DockerCluster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PlainTextGenerator extends ConfigFileGenerator {
    private static final Logger logger = LogManager
            .getLogger(PlainTextGenerator.class);

    public Path defaultFilePath;
    public Path defaultNewFilePath;

    public String generationType;

    public PlainTextGenerator(Path defaultFilePath, Path defaultNewFilePath,
            String generationType, Path generateFolderPath) {
        super(generateFolderPath);
        this.defaultFilePath = defaultFilePath;
        this.defaultNewFilePath = defaultNewFilePath;
        this.generationType = generationType;
    }

    public int generate(Map<String, String> key2vals,
            Map<String, String> key2type,
            Map<String, String> newkey2vals,
            Map<String, String> newkey2type) {
        Path savePath = generateFolderPath
                .resolve(String.format("test%d", fileNameIdx));
        Path oriConfig = savePath.resolve("oriconfig");
        Path upConfig = savePath.resolve("upconfig");
        oriConfig.toFile().mkdirs();
        upConfig.toFile().mkdirs();
        Path oriSavePath = oriConfig.resolve(defaultFilePath.getFileName());
        Path upSavePath = upConfig.resolve(defaultNewFilePath.getFileName());
        if (generationType.equals("regionservers") && nodeNum > 0) {
            String[] RegionIPs = new String[nodeNum - 1];
            for (int i = 0; i < nodeNum - 1; i++) {
                RegionIPs[i] = new String(
                        DockerCluster.getKthIP(hostIP, i + 1));
            }
            String RegionServerIPs = String.join("\n", RegionIPs) + "\n";
            try (FileOutputStream output = new FileOutputStream(
                    oriSavePath.toFile())) {
                output.write(RegionServerIPs.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return fileNameIdx++;
    }
}
