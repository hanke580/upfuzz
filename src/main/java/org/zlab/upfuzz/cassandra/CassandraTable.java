package org.zlab.upfuzz.cassandra;

import org.zlab.upfuzz.ParameterType;

import java.util.HashMap;
import java.util.Map;

public class CassandraTable {
    public String name;
    public Map<String, ParameterType> colName2Type = new HashMap<>();
    public Map<String, ParameterType> primaryColName2Type = new HashMap<>();
}
