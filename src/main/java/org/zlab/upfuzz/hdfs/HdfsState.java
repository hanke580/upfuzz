package org.zlab.upfuzz.hdfs;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.hdfs.MockFS.HadoopFileSystem;
import org.zlab.upfuzz.hdfs.MockFS.INode;
import org.zlab.upfuzz.hdfs.MockFS.LocalFileSystem;

public class HdfsState extends State {

    public static String subdir;
    private static String localRoot;
    public static HadoopFileSystem dfs;
    public static LocalFileSystem lfs;

    static {
        newLocalFSState();
    }

    public static void newLocalFSState() {
        subdir = "/" + RandomStringUtils.randomAlphabetic(8, 8 + 1);
        localRoot = "/tmp/upfuzz/hdfs" + subdir;
        dfs = new HadoopFileSystem();
        lfs = new LocalFileSystem(localRoot);
        randomize(0.6);
    }

    public HdfsState() {
        // A small chance to use the new fs state
        if (RandomUtils.nextDouble(0, 1) < Config.getConf().new_fs_state_prob) {
            newLocalFSState();
        }
    }

    public static void randomize(double ratio) {
        dfs.randomize(ratio);
        lfs.randomize(ratio);
    }

    @Override
    public void clearState() {
        dfs = new HadoopFileSystem();
        // lfs remain the same
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
