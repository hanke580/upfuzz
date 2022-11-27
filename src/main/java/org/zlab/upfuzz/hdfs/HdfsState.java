package org.zlab.upfuzz.hdfs;

import org.apache.commons.lang3.RandomStringUtils;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.MockFS.HadoopFileSystem;
import org.zlab.upfuzz.hdfs.MockFS.INode;
import org.zlab.upfuzz.hdfs.MockFS.LocalFileSystem;
import org.zlab.upfuzz.utils.Utilities;

import java.util.UUID;

public class HdfsState extends State {

    private String localRoot;

    public HadoopFileSystem dfs;
    public LocalFileSystem lfs;

    public String subdir;

    public HdfsState() {
//        subdir = UUID.randomUUID().toString().replace("-", "");

        subdir = "/" + RandomStringUtils.randomAlphabetic(8, 8 + 1);
        localRoot = "/tmp/upfuzz/hdfs" + subdir;

        dfs = new HadoopFileSystem();
        lfs = new LocalFileSystem(localRoot);

        randomize(0.6);
    }

    public void randomize(double ratio) {
        dfs.randomize(ratio);
        lfs.randomize(ratio);
    }

    @Override
    public void clearState() {
        // TODO Auto-generated method stub
    }

    public INode getRandomHadoopPath() {
        return dfs.getRandomPath();
    }

    public String getRandomHadoopPathString() {
        return dfs.getRandomPathString();
    }

    public String getRandomLocalPathString() {
        return lfs.getRandomPathString();
    }
}
