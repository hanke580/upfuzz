package org.zlab.upfuzz.hdfs.MockFS;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.zlab.upfuzz.hdfs.MockFS.INode.IType;

public class HadoopFileSystem implements Serializable {
    static int file_size = 1024;

    Map<String, INode> inodeMap = new HashMap<String, INode>();

    // --------Simple FS--------
    Set<String> files = new HashSet<>();
    Set<String> dirs = new HashSet<>();

    Map<String, Set<String>> snapshotDir2Filename = new HashMap<>();

    public HadoopFileSystem() {
        dirs.add("/");
    }

    public static List<String> fileType = new LinkedList<>();

    static {
        fileType.add(".txt");
        fileType.add(".xml");
        fileType.add(".yaml");
    }

    String[] user_id = new String[file_size];

    String[] group_id = new String[file_size];

    public void randomize() {
        randomize(0.5);
    }

    public void randomize(double ratio) {
        inodeMap.clear();
        INode root = new INode("/", IType.Dir, 1000, 1000, 0660);
        addNode(root);
        Integer num = RandomUtils.nextInt(0, 128) + 1;
        for (int i = 0; i < num; ++i) {
            INode dir = getRandomDir(), node;
            if (RandomUtils.nextDouble(0, 1) < ratio) {
                node = new INode(dir, RandomStringUtils.randomAlphabetic(1, 12),
                        IType.File);
            } else {
                node = new INode(dir, RandomStringUtils.randomAlphabetic(1, 12),
                        IType.Dir);
            }
            addNode(node);
        }
    }

    private void addNode(INode node) {
        inodeMap.put(node.file_path, node);
    }

    // --------Simple FS--------
    public String getRandomFilePath() {
        if (files.isEmpty())
            return null;
        int idx = new Random().nextInt(files.size());
        return new ArrayList<>(files).get(idx);
    }

    public String getRandomDirPath() {
        // If there is no dir in FS, we return NULL
        if (dirs.isEmpty())
            return null;
        int idx = new Random().nextInt(dirs.size());
        return new ArrayList<>(dirs).get(idx);
    }

    public void createFile(String path) {
        files.add(path);
    }

    public void createDir(String path) {
        dirs.add(path);
    }

    public void removeFile(String path) {
        files.remove(path);
    }

    public void removeDir(String path) {
        Map<String, Set<String>> updatedSnapshotDir2Filename = new HashMap<>();
        for (String dir : snapshotDir2Filename.keySet()) {
            if (!dir.startsWith(path)) {
                updatedSnapshotDir2Filename.put(dir,
                        snapshotDir2Filename.get(dir));
            }
        }
        snapshotDir2Filename = updatedSnapshotDir2Filename;

        Set<String> updatedDirs = new HashSet<>();
        for (String dir : dirs) {
            if (!dir.startsWith(path)) {
                updatedDirs.add(dir);
            }
        }
        dirs = updatedDirs;

        Set<String> updatedFiles = new HashSet<>();
        for (String file : files) {
            if (!file.startsWith(path)) {
                updatedFiles.add(file);
            }
        }
        files = updatedFiles;
    }

    public boolean containsDir(String path) {
        return dirs.contains(path);
    }

    public boolean containsFile(String path) {
        return files.contains(path);
    }

    public List<String> getFiles(String path) {
        List<String> retFiles = new LinkedList<>();
        for (String file : files) {
            if (file.startsWith(path)) {
                retFiles.add(file);
            }
        }
        return retFiles;
    }

    public List<String> getDirs(String path) {
        List<String> retDirs = new LinkedList<>();
        for (String dir : dirs) {
            if (dir.startsWith(path)) {
                retDirs.add(dir);
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

    public String getRandomSnapshotPath() {
        // If there is no file in FS, we return NULL
        if (snapshotDir2Filename.isEmpty())
            return null;
        // Random pick one from snapshotDir2Filename
        String[] dirArr = snapshotDir2Filename.keySet().toArray(new String[0]);
        int idx = new Random().nextInt(snapshotDir2Filename.size());
        String snapshotDir = dirArr[idx];
        // now pick a file
        String[] fileArr = snapshotDir2Filename.get(snapshotDir)
                .toArray(new String[0]);
        idx = new Random()
                .nextInt(snapshotDir2Filename.get(snapshotDir).size());
        return Paths.get(snapshotDir, fileArr[idx]).toString();
    }

    // --------Simple FS End--------

    public Integer remixHash(int x) {
        return x ^ (x >>> 16);
    }

    public INode getRandomPath() {
        INode[] fis = inodeMap.values().toArray(new INode[0]);
        return fis[RandomUtils.nextInt(0, fis.length)];
    }

    public INode getRandomPath(double ratio) {
        if (RandomUtils.nextDouble(0, 1) < ratio) {
            return getRandomDir();
        } else {
            return getRandomFile();
        }
    }

    public INode getRandomFile() {
        INode[] fis = inodeMap.values().toArray(new INode[0]);
        int index = RandomUtils.nextInt(0, fis.length);
        while (!fis[index].isFile()) {
            index = RandomUtils.nextInt(0, fis.length);
        }
        return fis[index];
    }

    // TODO improve random dir method
    public INode getRandomDir() {
        INode[] fis = inodeMap.values().toArray(new INode[0]);
        int index = RandomUtils.nextInt(0, fis.length);
        while (!fis[index].isDir()) {
            index = RandomUtils.nextInt(0, fis.length);
        }
        return fis[index];
    }

    public String getRandomPathString() {
        return getRandomPath().file_path;
    }

    public INode getNode(Integer index) {
        INode[] fis = inodeMap.values().toArray(new INode[0]);
        if (index < fis.length) {
            return fis[index];
        } else {
            return null;
        }
    }

    public INode getNode(String fileName) {
        return inodeMap.get(fileName);
    }

    public Boolean createPath(Path path) {
        while (path != null) {
            if (inodeMap.containsKey(path)) {
                break;
            } else {
                INode dirNode = new INode(path.toString(), IType.Dir, 1000,
                        1000, 0660);
                inodeMap.put(dirNode.file_path, dirNode);
                path = path.getParent();
            }
        }
        return true;
    }

    public Boolean createFile(String file, Boolean createParents) {
        INode fileNode = new INode(file, IType.File, 1000, 1000, 0660);
        if (!inodeMap.containsKey(fileNode.file_path)) {
            if (createParents) {
                createPath(Paths.get(file).getParent());
            }
            inodeMap.put(fileNode.file_path, fileNode);
        } else {
            // TODO if the file alreadt exists
            // maybe overwrite
            // leave false now
            return false;
        }
        return true;
    }

    public Boolean moveTo(String[] src, Integer flag) {
        String dest = src[src.length - 1];
        if (src.length < 2) {
            return false;
        }
        if (src.length > 2) {
            INode destNode = getNode(dest);
            if (destNode == null || destNode.i_type == IType.File) {
                return false;
            }
        }
        for (int i = 0; i < src.length - 1; ++i) {
            String source = src[i];
            if (!moveTo(source, dest, flag)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 1. move to null -> rename
     * 2. move to dir -> rename
     * 3. move to file (INVALID)
     */

    public Boolean moveTo(String src, String dest, Integer flag) {
        INode srcNode = getNode(src), destNode = getNode(dest);
        if (srcNode == null) {
            return false;
        }
        if (destNode == null) {
            inodeMap.remove(src);
            srcNode.setPath(dest);
            inodeMap.put(srcNode.file_path, srcNode);
        } else {
            if (destNode.i_type == IType.Dir) {
                String newFilePath = PathUtils.join(destNode,
                        srcNode.file_name);
                srcNode.setPath(newFilePath);
                inodeMap.put(srcNode.file_path, srcNode);

                inodeMap.remove(src);
            } else if (destNode.i_type == IType.File) {
                srcNode.setPath(dest);
                inodeMap.put(srcNode.file_path, srcNode);

                inodeMap.remove(src);
                inodeMap.remove(dest);
            }
        }
        return false;
    }

    public Boolean copyTo(String[] src, Boolean flag) {
        String dest = src[src.length - 1];
        if (src.length < 2) {
            return false;
        }
        if (src.length > 2) {
            INode destNode = getNode(dest);
            if (destNode == null || destNode.isFile()) {
                return false;
            }
        }

        for (int i = 0; i < src.length - 1; ++i) {
            INode source = getNode(src[i]);
            if (source == null) {
                return false;
            }
            if (source.i_type == IType.File) {
                copyFileTo(source, dest, null);
            }
        }
        return true;
    }

    /**
     * 1. copy file to null
     * 2. copy file to dir
     * 3. copy file to file (INVALID)
     */
    public Boolean copyFileTo(INode source, String dest, Object object) {
        INode destNode = getNode(dest);
        if (destNode == null) {
            destNode = new INode(source);
            inodeMap.put(destNode.file_path, destNode);
        } else if (destNode.isDir()) {
            INode newFileNode = new INode(source);
            newFileNode.setPath(PathUtils.join(destNode, source.file_name));
            inodeMap.put(newFileNode.file_path, newFileNode);
        } else if (destNode.isFile()) {
            return false;
        }
        return true;
    }

    /**
     * 1. copy dir to null
     * 2. copy dir to dir
     * 3. copy dir to file (INVALID)
     */
    public Boolean copyDirTo(INode source, String dest, Boolean flag) {
        INode destNode = getNode(dest);
        if (destNode == null) {
            destNode = new INode(source);
            destNode.setPath(dest);
            inodeMap.put(destNode.file_path, destNode);
        } else if (destNode.isDir()) {
            INode newFileNode = new INode(source);
            newFileNode.setPath(PathUtils.join(destNode, source.file_name));
            inodeMap.put(newFileNode.file_path, newFileNode);
        } else if (destNode.isFile()) {
            return false;
        }
        return true;
    }

    public Boolean remove(String[] files, Integer flag) {
        for (int i = 0; i < files.length; ++i) {
            INode file = getNode(files[i]);
            if (file.isFile()) {
                if (!removeFile(file, flag)) {
                    return false;
                }
            } else if (file.isDir()) {
                if (!removeDir(file, flag)) {
                    return false;
                }
            }
        }
        return true;
    }

    public Boolean removeDir(INode file, Integer flag) {
        Boolean deleted = false;
        for (Iterator<Map.Entry<String, INode>> it = inodeMap.entrySet()
                .iterator(); it.hasNext();) {
            Map.Entry<String, INode> entry = it.next();
            if (entry.getKey().startsWith(file.file_path)) {
                it.remove();
                deleted = true;
            }
        }
        return deleted;
    }

    public Boolean removeFile(INode file, Integer flag) {
        if (inodeMap.containsKey(file.file_path)) {
            inodeMap.remove(file.file_path);
            return true;
        }
        return false;
    }
}
