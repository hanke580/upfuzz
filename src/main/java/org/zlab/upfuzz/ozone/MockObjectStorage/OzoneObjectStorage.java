package org.zlab.upfuzz.ozone.MockObjectStorage;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Iterator;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.zlab.upfuzz.ozone.MockObjectStorage.ObjNode.ObjType;

public class OzoneObjectStorage implements Serializable {

    static int file_size = 1024;

    Map<String, ObjNode> inodeMap = new HashMap<String, ObjNode>();

    // --------Simple FS--------
    // volume -> bucket -> key
    public Map<String, Map<String, Set<String>>> layout = new HashMap<>();

    public Set<String> buckets = new HashSet<>();
    public Set<String> volumes = new HashSet<>();
    public Set<String> keys = new HashSet<>();

    Map<String, Set<String>> snapshotDir2Filename = new HashMap<>();

    public OzoneObjectStorage() {
    }

    String[] user_id = new String[file_size];

    String[] group_id = new String[file_size];

    public void randomize() {
        randomize(0.5);
    }

    public void randomize(double ratio) {
        inodeMap.clear();
        ObjNode root = new ObjNode("/", ObjType.Volume, 1000, 1000, 0660);
        addNode(root);
        Integer num = RandomUtils.nextInt(0, 128) + 1;
        for (int i = 0; i < num; ++i) {
            ObjNode vol = getRandomVolume(), node;
            if (RandomUtils.nextDouble(0, 1) < ratio) {
                node = new ObjNode(vol,
                        RandomStringUtils.randomAlphabetic(1, 12),
                        ObjType.Bucket);
            } else {
                node = new ObjNode(vol,
                        RandomStringUtils.randomAlphabetic(1, 12),
                        ObjType.Volume);
            }
            addNode(node);
        }
    }

    private void addNode(ObjNode node) {
        inodeMap.put(node.file_path, node);
    }

    public String getRandomKeyPath() {
        // If there is no file in FS, we return NULL
        if (keys.isEmpty())
            return null;
        String[] keyArr = keys.toArray(new String[keys.size()]);
        int idx = new Random().nextInt(keys.size());
        return keyArr[idx];
    }

    public String getRandomBucketPath() {
        // If there is no file in FS, we return NULL
        if (buckets.isEmpty())
            return null;
        String[] fileArr = buckets.toArray(new String[buckets.size()]);
        int idx = new Random().nextInt(buckets.size());
        return fileArr[idx];
    }

    public String getRandomVolumePath() {
        // If there is no dir in FS, we return NULL
        if (volumes.isEmpty())
            return null;
        String[] volArr = volumes.toArray(new String[volumes.size()]);
        int idx = new Random().nextInt(volumes.size());
        return volArr[idx];
    }

    public void createKey(String path) {
        keys.add(path);
    }

    public void createBucket(String path) {
        buckets.add(path);
    }

    public void createVolume(String path) {
        volumes.add(path);
    }

    public void removeBucket(String path) {
        buckets.remove(path);
    }

    public void removeKey(String path) {
        keys.remove(path);
    }

    public void renameKey(String path, String updatePath) {
        if (keys.isEmpty())
            return;

        for (String key : keys) {
            if (key.equals(path)) {
                key = updatePath;
            }
        }
    }

    public boolean containsVolume(String path) {
        return volumes.contains(path);
    }

    public boolean containsBucket(String path) {
        return buckets.contains(path);
    }

    public boolean containsKey(String path) {
        return keys.contains(path);
    }

    public List<String> getFiles(String path) {
        List<String> retBuckets = new LinkedList<>();
        for (String bucket : buckets) {
            if (bucket.startsWith(path)) {
                retBuckets.add(bucket);
            }
        }
        return retBuckets;
    }

    public List<String> getKeys(String path) {
        List<String> retKeys = new LinkedList<>();
        for (String key : keys) {
            if (key.startsWith(path)) {
                retKeys.add(key);
            }
        }
        return retKeys;
    }

    public List<String> getDirs(String path) {
        List<String> retDirs = new LinkedList<>();
        for (String vol : volumes) {
            if (vol.startsWith(path)) {
                retDirs.add(vol);
            }
        }
        return retDirs;
    }

    public void createSnapShotFile(String dir, String filename) {
        if (!snapshotDir2Filename.containsKey(dir)) {
            snapshotDir2Filename.put(dir, new HashSet<>());
        }
        snapshotDir2Filename.get(dir).add(filename);
    }

    public void rmSnapShotFile(String dir, String filename) {
        if (snapshotDir2Filename.containsKey(dir)) {
            snapshotDir2Filename.get(dir).remove(filename);
        }
    }

    // --------Simple FS End--------

    public Integer remixHash(int x) {
        return x ^ (x >>> 16);
    }

    public ObjNode getRandomPath() {
        ObjNode[] fis = inodeMap.values().toArray(new ObjNode[0]);
        return fis[RandomUtils.nextInt(0, fis.length)];
    }

    public ObjNode getRandomPath(double ratio) {
        double randomValue = RandomUtils.nextDouble(0, 1);

        if (randomValue < ratio / 3) {
            return getRandomVolume();
        } else if (randomValue < 2 * ratio / 3) {
            return getRandomBucket();
        } else {
            return getRandomKey();
        }
    }

    public ObjNode getRandomKey() {
        ObjNode[] fis = inodeMap.values().toArray(new ObjNode[0]);
        int index = RandomUtils.nextInt(0, fis.length);
        while (!fis[index].isKey()) {
            index = RandomUtils.nextInt(0, fis.length);
        }
        return fis[index];
    }

    public ObjNode getRandomBucket() {
        ObjNode[] fis = inodeMap.values().toArray(new ObjNode[0]);
        int index = RandomUtils.nextInt(0, fis.length);
        while (!fis[index].isBucket()) {
            index = RandomUtils.nextInt(0, fis.length);
        }
        return fis[index];
    }

    // TODO improve random dir method
    public ObjNode getRandomVolume() {
        ObjNode[] vis = inodeMap.values().toArray(new ObjNode[0]);
        int index = RandomUtils.nextInt(0, vis.length);
        while (!vis[index].isVolume()) {
            index = RandomUtils.nextInt(0, vis.length);
        }
        return vis[index];
    }

    public String getRandomPathString() {
        return getRandomPath().file_path;
    }

    public ObjNode getNode(Integer index) {
        ObjNode[] fis = inodeMap.values().toArray(new ObjNode[0]);
        if (index < fis.length) {
            return fis[index];
        } else {
            return null;
        }
    }

    public ObjNode getNode(String fileName) {
        return inodeMap.get(fileName);
    }

    public Boolean createPath(Path path) {
        while (path != null) {
            if (inodeMap.containsKey(path)) {
                break;
            } else {
                ObjNode volNode = new ObjNode(path.toString(), ObjType.Volume,
                        1000,
                        1000, 0660);
                inodeMap.put(volNode.file_path, volNode);
                path = path.getParent();
            }
        }
        return true;
    }

    public Boolean createBucket(String file, Boolean createParents) {
        ObjNode bucketNode = new ObjNode(file, ObjType.Bucket, 1000, 1000,
                0660);
        if (!inodeMap.containsKey(bucketNode.file_path)) {
            if (createParents) {
                createPath(Paths.get(file).getParent());
            }
            inodeMap.put(bucketNode.file_path, bucketNode);
        } else {
            // TODO if the file alreadt exists
            // maybe overwrite
            // leave false now
            return false;
        }
        return true;
    }

    public Boolean remove(String[] buckets, Integer flag) {
        for (int i = 0; i < buckets.length; ++i) {
            ObjNode bucket = getNode(buckets[i]);
            if (bucket.isKey()) {
                if (!removeKey(bucket, flag)) {
                    return false;
                }
            } else if (bucket.isBucket()) {
                if (!removeBucket(bucket, flag)) {
                    return false;
                }
            } else if (bucket.isVolume()) {
                if (!removeVolume(bucket, flag)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void removeVolume(String path) {
        Map<String, Set<String>> updatedSnapshotDir2Filename = new HashMap<>();
        for (String dir : snapshotDir2Filename.keySet()) {
            if (!dir.startsWith(path)) {
                updatedSnapshotDir2Filename.put(dir,
                        snapshotDir2Filename.get(dir));
            }
        }
        snapshotDir2Filename = updatedSnapshotDir2Filename;

        Set<String> updatedVols = new HashSet<>();
        for (String vol : volumes) {
            if (!vol.startsWith(path)) {
                updatedVols.add(vol);
            }
        }
        volumes = updatedVols;

        Set<String> updatedBuckets = new HashSet<>();
        for (String bucket : buckets) {
            if (!bucket.startsWith(path)) {
                updatedBuckets.add(bucket);
            }
        }
        buckets = updatedBuckets;
    }

    public Boolean removeVolume(ObjNode bucket, Integer flag) {
        Boolean deleted = false;
        for (Iterator<Map.Entry<String, ObjNode>> it = inodeMap.entrySet()
                .iterator(); it.hasNext();) {
            Map.Entry<String, ObjNode> entry = it.next();
            if (entry.getKey().startsWith(bucket.file_path)) {
                it.remove();
                deleted = true;
            }
        }
        return deleted;
    }

    public Boolean removeBucket(ObjNode bucket, Integer flag) {
        if (inodeMap.containsKey(bucket.file_path)) {
            inodeMap.remove(bucket.file_path);
            return true;
        }
        return false;
    }

    public void printStorage() {
        System.out.println("Ozone volumes: ");
        System.out.println(String.join(", ", volumes));
        System.out.println("Ozone buckets: ");
        System.out.println(String.join(", ", buckets));
    }

    public Boolean removeKey(ObjNode key, Integer flag) {
        if (inodeMap.containsKey(key.file_path)) {
            inodeMap.remove(key.file_path);
            return true;
        }
        return false;
    }
}
