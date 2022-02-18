package org.zlab.upfuzz.cassandra;

import java.util.HashMap;
import java.util.Map;

public class CassandraTable {
    public String name;
    public Map<String, String> colName2Type = new HashMap<>();
    public Map<String, String> primaryColName2Type = new HashMap<>();
}
