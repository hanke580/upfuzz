package org.zlab.upfuzz.hdfs;

import org.zlab.upfuzz.fuzzingengine.configgen.ConfigGen;
import org.zlab.upfuzz.fuzzingengine.configgen.PlainTextGenerator;
import org.zlab.upfuzz.fuzzingengine.configgen.xml.XmlGenerator;

import java.nio.file.Path;
import java.nio.file.Paths;

public class HdfsConfigGen extends ConfigGen {

    @Override
    public void updateConfigBlackList() {
    }

    @Override
    public void initUpgradeFileGenerator() {
        Path defaultConfigPath = Paths.get(oldVersionPath.toString(),
                "etc/hadoop/hdfs-site.xml");
        Path defaultNewConfigPath = Paths.get(newVersionPath.toString(),
                "etc/hadoop/hdfs-site.xml");
        configFileGenerator = new XmlGenerator[1];
        configFileGenerator[0] = new XmlGenerator(defaultConfigPath,
                defaultNewConfigPath, generateFolderPath);
        extraGenerator = new PlainTextGenerator[0];
    }

    @Override
    public void initSingleFileGenerator() {
        Path defaultConfigPath = Paths.get(oldVersionPath.toString(),
                "etc/hadoop/hdfs-site.xml");
        configFileGenerator = new XmlGenerator[1];
        configFileGenerator[0] = new XmlGenerator(defaultConfigPath,
                generateFolderPath);
        extraGenerator = new PlainTextGenerator[0];
    }
}
