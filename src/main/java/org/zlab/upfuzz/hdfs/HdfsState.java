package org.zlab.upfuzz.hdfs;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.hdfs.MockFS.HadoopFileSystem;
import org.zlab.upfuzz.hdfs.MockFS.INode;
import org.zlab.upfuzz.hdfs.MockFS.LocalFileSystem;

public class HdfsState extends State {

    public String subdir;
    public HadoopFileSystem dfs;

    // reuse local fs
    public static LocalFileSystem lfs;

    static {
        newLocalFSState();
    }

    public static void newLocalFSState() {
        String local_subdir = "/"
                + RandomStringUtils.randomAlphabetic(8, 8 + 1);
        // reuse lfs
        String localRoot = "/tmp/upfuzz/hdfs" + local_subdir;
        lfs = new LocalFileSystem(localRoot);
        lfs.randomize(0.6);
    }

    public HdfsState() {
        subdir = "/" + RandomStringUtils.randomAlphabetic(8, 8 + 1);
        dfs = new HadoopFileSystem();
        dfs.randomize(0.6);
        // A small chance to use the new fs state
        if (RandomUtils.nextDouble(0, 1) < Config.getConf().new_fs_state_prob) {
            newLocalFSState();
        }
    }

    public void randomize(double ratio) {
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
