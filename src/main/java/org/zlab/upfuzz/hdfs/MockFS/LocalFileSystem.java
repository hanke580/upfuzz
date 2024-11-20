package org.zlab.upfuzz.hdfs.MockFS;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.codehaus.plexus.util.FileUtils;
import org.zlab.upfuzz.hdfs.MockFS.INode.IType;

public class LocalFileSystem extends FileSystem {
    static int file_size = 1024;

    Map<String, INode> inodeMap = new HashMap<String, INode>();

    String[] user_id = new String[file_size];

    String[] group_id = new String[file_size];

    public String localRoot;

    // If we are using too much space, we can stop generating
    // mockFS. Everytime, we only use existing files/folder
    public LocalFileSystem(String root) {
        localRoot = root;
        Paths.get(localRoot).toFile().mkdirs();
    }

    public void randomize() {
        randomize(0.5);
    }

    public void randomize(double ratio) {
        int num = RandomUtils.nextInt(0, 16) + 1;
        for (int i = 0; i < num; ++i) {
            String dir = getRandomDir().toString();
            if (RandomUtils.nextDouble(0, 1) < ratio) {
                Path newDir = Paths.get(dir,
                        RandomStringUtils.randomAlphabetic(1, 12));
                createDir(newDir.toString(), true);
            } else {
                Path newFile = Paths.get(dir,
                        RandomStringUtils.randomAlphabetic(1, 12));
                createFile(newFile.toString(), true);
            }
        }
    }

    public Integer remixHash(int x) {
        return x ^ (x >>> 16);
    }

    public File getRandomPath() {
        File[] files = new File(localRoot).listFiles();
        return files[RandomUtils.nextInt(0, files.length)];
    }

    public File getRandomPath(double ratio) {
        if (RandomUtils.nextDouble(0, 1) < ratio) {
            return getRandomDir();
        } else {
            return getRandomFile();
        }
    }

    public File getRandomFile() {
        File[] files = new File(localRoot).listFiles();
        int index = RandomUtils.nextInt(0, files.length);
        while (!files[index].isFile()) {
            index = RandomUtils.nextInt(0, files.length);
        }
        return files[index];
    }

    public boolean localRootContainsFile() {
        File[] files = new File(localRoot).listFiles();
        for (File file : files) {
            if (file.isFile()) {
                return true;
            }
        }
        return false;
    }

    // TODO improve random dir method
    public File getRandomDir() {
        try {
            Long st = System.currentTimeMillis();
            File[] files = Files
                    .find(Paths.get(localRoot), Integer.MAX_VALUE,
                            (filePath, fileAttr) -> true)
                    .map(path -> path.toFile())
                    .toArray(size -> new File[size]);
            Long ed = System.currentTimeMillis();
            // System.out.println("files size: " + files.length + "\n" + (ed -
            // st));
            int index = RandomUtils.nextInt(0, files.length);
            while (!files[index].isDirectory()) {
                index = RandomUtils.nextInt(0, files.length);
            }
            return files[index];
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    public String getRandomPathString() {
        return getRandomPath().getPath();
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

    public Boolean createDir(String file, Boolean createParents) {
        File dir = new File(file);
        if (!dir.exists()) {
            if (createParents) {
                dir.getParentFile().mkdirs();
            }
            dir.mkdir();
        }
        return true;
    }

    public Boolean createFile(String file, Boolean createParents) {
        File f = new File(file);
        if (!f.exists()) {
            if (createParents) {
                f.getParentFile().mkdirs();
            }
            try {
                f.createNewFile();
                BufferedWriter writer = new BufferedWriter(new FileWriter(f));
                writer.write(RandomStringUtils.randomAscii(1024, 65536));
            } catch (IOException e) {
                return false;
            }
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
        for (int i = 0; i < src.length - 1; ++i) {
            try {
                Files.move(Paths.get(src[i]), Paths.get(dest));
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    public Boolean copyTo(String[] src, Boolean flag) {
        String dest = src[src.length - 1];
        for (int i = 0; i < src.length - 1; ++i) {
            try {
                Files.copy(Paths.get(src[i]), Paths.get(dest));
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    public Boolean remove(String[] files, Integer flag) {
        for (String fileString : files) {
            try {
                Files.delete(Paths.get(fileString));
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }
}
