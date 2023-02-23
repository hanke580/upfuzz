package org.zlab.upfuzz.hbase;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.utils.Utilities;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HBaseState extends State {

    public Map<String, Map<String, HBaseTable>> keyspace2tables = new HashMap<>();
    public Map<String, Set<String>> keyspace2UDTs = new HashMap<>();

    public void addTable(String keyspaceName, String tableName,
                         HBaseTable table) {
        keyspace2tables.get(keyspaceName).put(tableName, table);
    }

    public void addKeyspace(String keyspaceName) {
        if (!keyspace2tables.containsKey(keyspaceName)) {
            keyspace2tables.put(keyspaceName, new HashMap<>());
        }
        if (!keyspace2UDTs.containsKey(keyspaceName)) {
            keyspace2UDTs.put(keyspaceName, new HashSet<>());
        }
    }

    public Set<Parameter> getKeyspaces() {
        return Utilities.strings2Parameters(keyspace2tables.keySet());
    }

    public Set<Parameter> getTablesInKeyspace(String keyspaceName) {
        return Utilities
                .strings2Parameters(keyspace2tables.get(keyspaceName).keySet());
    }

    public HBaseTable getTable(String keyspaceName, String tableName) {
        return keyspace2tables.get(keyspaceName).get(tableName);
    }

    @Override
    public void clearState() {
        keyspace2tables.clear();
        keyspace2UDTs.clear();
    }

    //public HBaseState() {
    //}

    //public void clearState() {
    //}
}
