package org.zlab.upfuzz.hbase;

import org.zlab.upfuzz.fuzzingengine.configgen.ConfigGen;
import org.zlab.upfuzz.fuzzingengine.configgen.PlainTextGenerator;
import org.zlab.upfuzz.fuzzingengine.configgen.xml.XmlGenerator;

import java.nio.file.Path;
import java.nio.file.Paths;

public class HBaseConfigGen extends ConfigGen {
    @Override
    public void updateConfigBlackList() {
    }

    @Override
    public void initUpgradeFileGenerator() {
        Path defaultConfigPath = Paths.get(oldVersionPath.toString(),
                "conf/hbase-site.xml");
        Path defaultNewConfigPath = Paths.get(newVersionPath.toString(),
                "conf/hbase-site.xml");
        assert depSystemPath != null : "Please provide depSystemPath";
        Path defaultHdfsConfigPath = Paths.get(depSystemPath.toString(),
                "etc/hadoop/hdfs-site.xml");
        Path defaultHdfsNamenodeConfigPath = Paths.get(
                depSystemPath.toString(),
                "etc/hadoop/core-site.xml");
        Path defaultRegionserversPath = Paths.get(oldVersionPath.toString(),
                "conf/regionservers");
        Path defaultNewRegionserversPath = Paths.get(
                newVersionPath.toString(),
                "conf/regionservers");
        configFileGenerator = new XmlGenerator[3];
        configFileGenerator[0] = new XmlGenerator(defaultConfigPath,
                defaultNewConfigPath, generateFolderPath);
        configFileGenerator[1] = new XmlGenerator(defaultHdfsConfigPath,
                defaultHdfsConfigPath, generateFolderPath);
        configFileGenerator[2] = new XmlGenerator(
                defaultHdfsNamenodeConfigPath,
                defaultHdfsNamenodeConfigPath, generateFolderPath);
        extraGenerator = new PlainTextGenerator[1];
        extraGenerator[0] = new PlainTextGenerator(defaultRegionserversPath,
                defaultNewRegionserversPath, "regionservers",
                generateFolderPath);
    }

    @Override
    public void initSingleFileGenerator() {
        Path defaultConfigPath = Paths.get(oldVersionPath.toString(),
                "conf/hbase-site.xml");
        assert depSystemPath != null : "Please provide depSystemPath";
        Path defaultHdfsConfigPath = Paths.get(depSystemPath.toString(),
                "etc/hadoop/hdfs-site.xml");
        Path defaultHdfsNamenodeConfigPath = Paths.get(
                depSystemPath.toString(),
                "etc/hadoop/core-site.xml");
        Path defaultRegionserversPath = Paths.get(oldVersionPath.toString(),
                "conf/regionservers");
        configFileGenerator = new XmlGenerator[3];
        configFileGenerator[0] = new XmlGenerator(defaultConfigPath,
                generateFolderPath);
        configFileGenerator[1] = new XmlGenerator(defaultHdfsConfigPath,
                defaultHdfsConfigPath, generateFolderPath);
        configFileGenerator[2] = new XmlGenerator(
                defaultHdfsNamenodeConfigPath,
                defaultHdfsNamenodeConfigPath, generateFolderPath);
        extraGenerator = new PlainTextGenerator[1];
        extraGenerator[0] = new PlainTextGenerator(defaultRegionserversPath,
                "regionservers",
                generateFolderPath);
    }
}
