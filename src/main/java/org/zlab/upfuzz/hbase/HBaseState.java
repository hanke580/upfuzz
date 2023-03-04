package org.zlab.upfuzz.hbase;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.utils.Utilities;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HBaseState extends State {

    public Map<String, Map<String, HBaseColumnFamily>> table2families = new HashMap<>();
    public Map<String, Set<String>> table2UDTs = new HashMap<>();

    public void addColumnFamily(String tableName, String columnFamilyName,
                         HBaseColumnFamily columnFamily) {
        table2families.get(tableName).put(columnFamilyName, columnFamily);
    }

    public void addTable(String tableName) {
        if (!table2families.containsKey(tableName)) {
            table2families.put(tableName, new HashMap<>());
        }
        if (!table2UDTs.containsKey(tableName)) {
            table2UDTs.put(tableName, new HashSet<>());
        }
    }

    public Set<Parameter> getTables() {
        return Utilities.strings2Parameters(table2families.keySet());
    }

    public Set<Parameter> getColumnFamiliesInTable(String tableName) {
        return Utilities
                .strings2Parameters(table2families.get(tableName).keySet());
    }

    public HBaseColumnFamily getColumnFamily(String tableName, String columnFamilyName) {
        return table2families.get(tableName).get(columnFamilyName);
    }

    @Override
    public void clearState() {
        table2families.clear();
        table2UDTs.clear();
    }

    //public HBaseState() {
    //}

    //public void clearState() {
    //}
}
