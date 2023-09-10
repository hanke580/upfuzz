package org.zlab.upfuzz.fuzzingengine.configgen;

import java.io.*;
import java.nio.file.Path;
import java.util.Map;
import org.zlab.upfuzz.docker.DockerCluster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.Config;

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

    public PlainTextGenerator(Path defaultFilePath, String generationType,
            Path generateFolderPath) {
        super(generateFolderPath);
        this.defaultFilePath = defaultFilePath;
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
        if (generationType.equals("regionservers")) {
            try {
                FileWriter writerOri = new FileWriter(oriSavePath.toFile());
                for (String regionName : Config.getConf().REGIONSERVERS) {
                    writerOri.write(regionName + "\n");
                }
                writerOri.close();

                FileWriter writerUp = new FileWriter(upSavePath.toFile());
                for (String regionName : Config.getConf().REGIONSERVERS) {
                    writerUp.write(regionName + "\n");
                }
                writerUp.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return fileNameIdx++;
    }

    @Override
    public int generate(Map<String, String> key2vals,
            Map<String, String> key2type) {
        // TODO: HBase single version testing
        assert false : "HBase single version testing is not yet supported";
        return 0;
    }
}
