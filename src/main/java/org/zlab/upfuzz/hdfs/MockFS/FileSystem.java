package org.zlab.upfuzz.hdfs.MockFS;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.zlab.upfuzz.hdfs.MockFS.INode.IType;

public abstract class FileSystem implements Serializable {
    static int file_size = 1024;

    Map<String, Integer> file_table = new HashMap<String, Integer>();

    String[] user_id = new String[file_size];

    String[] group_id = new String[file_size];

    public static String randomFile() {
        return "";
    }
}
