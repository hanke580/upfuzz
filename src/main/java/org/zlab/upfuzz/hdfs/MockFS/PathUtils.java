package org.zlab.upfuzz.hdfs.MockFS;

import java.nio.file.Paths;

public class PathUtils {
    public static String join(INode dir, String file) {
        return join(dir.file_path, file);
    }

    public static String join(String dir, String file) {
        return Paths.get(dir, file).toString();
    }
}
