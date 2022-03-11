package org.zlab.upfuzz.cassandra;

import org.zlab.upfuzz.State;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CassandraState extends State {
    public Map<String, CassandraTable> tables = new HashMap<>();
    public void addTable(String tableName, CassandraTable table) {
        tables.put(tableName, table);
    }

    @Override
    public void clearState() {
        tables.clear();
    }
}
