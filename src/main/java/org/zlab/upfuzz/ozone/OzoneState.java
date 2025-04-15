package org.zlab.upfuzz.ozone;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.hdfs.MockFS.HadoopFileSystem;
import org.zlab.upfuzz.hdfs.MockFS.INode;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.MockFS.LocalFileSystem;
import org.zlab.upfuzz.utils.Utilities;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OzoneState extends State {
    public String subdir;
    public String volumePrefix;

    public String volume;
    public String bucket;
    public String key;
    public HadoopFileSystem dfs;
    // public OzoneObjectStorage oos;

    // layout: volume -> bucket -> key
    public Map<String, Map<String, Set<String>>> layout = new HashMap<>();

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
        volumePrefix = RandomStringUtils.randomAlphabetic(8, 8 + 1)
                .toLowerCase();

        // Seems not in use
        volume = RandomStringUtils.randomAlphabetic(8, 8 + 1);
        bucket = RandomStringUtils.randomAlphabetic(4, 9);

        dfs = new HadoopFileSystem();
        dfs.randomize(0.6);
        // A small chance to use the new fs state
        if (RandomUtils.nextDouble(0, 1) < Config.getConf().new_fs_state_prob) {
            newLocalFSState();
        }
    }

    public void addVolume(String volumeName) {
        if (!layout.containsKey(volumeName)) {
            layout.put(volumeName, new HashMap<>());
        }
    }

    public void deleteVolume(String volumeName) {
        layout.remove(volumeName);
    }

    public void addBucket(String volumeName, String bucketName) {
        // volume must exist!
        assert layout.containsKey(volumeName);
        if (!layout.get(volumeName).containsKey(bucketName))
            layout.get(volumeName).put(bucketName, new HashSet<>());
        // otherwise, do nothing
    }

    public void deleteBucket(String volumeName, String bucketName) {
        assert layout.containsKey(volumeName);
        layout.get(volumeName).remove(bucketName);
    }

    public void addKey(String volumeName, String bucketName, String keyName) {
        assert layout.containsKey(volumeName);
        assert layout.get(volumeName).containsKey(bucketName);
        layout.get(volumeName).get(bucketName).add(keyName);
    }

    public void deleteKey(String volumeName, String bucketName,
            String keyName) {
        assert layout.containsKey(volumeName);
        assert layout.get(volumeName).containsKey(bucketName);
        layout.get(volumeName).get(bucketName).remove(keyName);
    }

    public Set<Parameter> getVolumes() {
        return Utilities.strings2Parameters(layout.keySet());
    }

    public Set<Parameter> getBuckets(String volumeName) {
        if (!layout.containsKey(volumeName))
            return new HashSet<>();
        return Utilities.strings2Parameters(layout.get(volumeName).keySet());
    }

    public Set<Parameter> getKeys(String volumeName, String bucketName) {
        if (!layout.containsKey(volumeName))
            return new HashSet<>();
        if (!layout.get(volumeName).containsKey(bucketName))
            return new HashSet<>();
        return Utilities
                .strings2Parameters(layout.get(volumeName).get(bucketName));
    }

    public void randomize(double ratio) {
        dfs.randomize(ratio);
        lfs.randomize(ratio);
    }

    @Override
    public void clearState() {
        dfs = new HadoopFileSystem();
        // lfs remain the same
        // clear layout
        layout.clear();
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
}
