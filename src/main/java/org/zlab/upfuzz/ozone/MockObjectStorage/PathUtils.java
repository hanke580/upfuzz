package org.zlab.upfuzz.ozone.MockObjectStorage;

import java.nio.file.Paths;

public class PathUtils {
    public static String join(ObjNode dir, String file) {
        return join(dir.file_path, file);
    }

    public static String join(String dir, String file) {
        return Paths.get(dir, file).toString();
    }
}
