package org.zlab.upfuzz.ozone;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.hdfs.MockFS.HadoopFileSystem;
import org.zlab.upfuzz.hdfs.MockFS.INode;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.MockFS.LocalFileSystem;
import org.zlab.upfuzz.ozone.MockObjectStorage.OzoneObjectStorage;
import org.zlab.upfuzz.ozone.MockObjectStorage.ObjNode;

public class OzoneState extends State {

    public String subdir;
    public String volume;
    public String bucket;
    public String key;
    public HadoopFileSystem dfs;
    public OzoneObjectStorage oos;

    // reuse local fs
    public static LocalFileSystem lfs;

    static {
        newLocalFSState();
    }

    public static void newLocalFSState() {
        String local_subdir = "/"
                + RandomStringUtils.randomAlphabetic(8, 8 + 1);
        // reuse lfs
        String localRoot = "/tmp/upfuzz/ozone" + local_subdir;
        lfs = new LocalFileSystem(localRoot);
        lfs.randomize(0.6);
    }

    public OzoneState() {
        subdir = "/" + RandomStringUtils.randomAlphabetic(8, 8 + 1);
        volume = RandomStringUtils.randomAlphabetic(8, 8 + 1);
        bucket = RandomStringUtils.randomAlphabetic(4, 9);
        // subdir = "";
        dfs = new HadoopFileSystem();
        oos = new OzoneObjectStorage();
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
        oos = new OzoneObjectStorage();
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

    public String getRandomLocalFilePathString() {
        if (lfs.localRootContainsFile()) {
            return lfs.getRandomFile().getPath();
        } else {
            return "";
        }
    }

    public ObjNode getRandomVolume() {
        return oos.getRandomVolume();
    }

    public ObjNode getRandomBucket() {
        return oos.getRandomBucket();
    }

    public ObjNode getRandomKey() {
        return oos.getRandomKey();
    }
}
