package org.zlab.upfuzz.hdfs.MockFS;

import java.util.HashMap;
import java.util.Map;

public class FileSystem {
    static int file_size = 10240;

    INode[] inode = new INode[file_size];

    Map<String, Integer> file_table = new HashMap<String, Integer>();

    String[] user_id = new String[file_size];

    String[] group_id = new String[file_size];

    public static String randomFile() {
        return "";
    }
}
