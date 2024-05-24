package org.zlab.upfuzz.hbase;

import org.apache.commons.lang3.SerializationUtils;
import org.zlab.upfuzz.Parameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Utils {

    public static HBaseColumnFamily deepCopyHBaseCF(HBaseColumnFamily cf) {
        HBaseColumnFamily newCF = new HBaseColumnFamily(cf.name, null);
        for (Parameter col : cf.colName2Type) {
            newCF.addColName2Type(SerializationUtils.clone(col));
        }
        return newCF;
    }

    public static Map<String, HBaseColumnFamily> deepCopyTable(
            Map<String, HBaseColumnFamily> oriTable) {
        Map<String, HBaseColumnFamily> newTable = new HashMap<>();
        for (String cfName : newTable.keySet()) {
            HBaseColumnFamily cf = newTable.get(cfName);
            newTable.put(cfName, deepCopyHBaseCF(cf));
        }
        return newTable;
    }

}
