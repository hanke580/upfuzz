package org.zlab.upfuzz.cassandra;

import org.zlab.upfuzz.State;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CassandraState extends State {
    public Map<String, Map<String, CassandraTable>> keyspace2tables = new HashMap<>();

    public void addTable(String keyspaceName, String tableName, CassandraTable table) {
        keyspace2tables.get(keyspaceName).put(tableName, table);
    }

    public void addKeyspace(String keyspaceName) {
        if (!keyspace2tables.containsKey(keyspaceName)) {
            keyspace2tables.put(keyspaceName, new HashMap<>());
        }
    }

    public Set<String> getKeyspaces() {
        return keyspace2tables.keySet();
    }

    public Set<String> getTablesInKeyspace(String keyspaceName) {
        return keyspace2tables.get(keyspaceName).keySet();
    }

    public CassandraTable getTable(String keyspaceName, String tableName) {
        return keyspace2tables.get(keyspaceName).get(tableName);
    }



    @Override
    public void clearState() {
        keyspace2tables.clear();
    }
}
