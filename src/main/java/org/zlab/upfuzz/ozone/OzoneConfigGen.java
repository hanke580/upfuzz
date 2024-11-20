package org.zlab.upfuzz.ozone;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.zlab.upfuzz.fuzzingengine.configgen.PlainTextGenerator;
import org.zlab.upfuzz.fuzzingengine.configgen.xml.XmlGenerator;
import org.zlab.upfuzz.fuzzingengine.configgen.ConfigGen;

public class OzoneConfigGen extends ConfigGen {

    @Override
    public void updateConfigBlackList() {
    }

    @Override
    public void initUpgradeFileGenerator() {
        Path defaultConfigPath = Paths.get(oldVersionPath.toString(),
                "etc/hadoop/ozone-site.xml");
        Path defaultNewConfigPath = Paths.get(newVersionPath.toString(),
                "etc/hadoop/ozone-site.xml");
        Path defaultOriNameConfigPath = Paths.get(oldVersionPath.toString(),
                "etc/hadoop/core-site.xml");
        Path defaultUpNameConfigPath = Paths.get(newVersionPath.toString(),
                "etc/hadoop/core-site.xml");

        configFileGenerator = new XmlGenerator[2];
        configFileGenerator[0] = new XmlGenerator(defaultConfigPath,
                defaultNewConfigPath, generateFolderPath);
        configFileGenerator[1] = new XmlGenerator(defaultOriNameConfigPath,
                defaultUpNameConfigPath, generateFolderPath);
        extraGenerator = new PlainTextGenerator[0];
    }

    @Override
    public void initSingleFileGenerator() {
        Path defaultConfigPath = Paths.get(oldVersionPath.toString(),
                "etc/hadoop/ozone-site.xml");
        Path defaultNameConfigPath = Paths.get(oldVersionPath.toString(),
                "etc/hadoop/core-site.xml");
        configFileGenerator = new XmlGenerator[2];
        configFileGenerator[0] = new XmlGenerator(defaultConfigPath,
                generateFolderPath);
        configFileGenerator[1] = new XmlGenerator(defaultNameConfigPath,
                generateFolderPath);
        extraGenerator = new PlainTextGenerator[0];
    }
}
