package org.zlab.upfuzz.hbase;

import java.io.Serializable;
import java.util.*;
import org.apache.commons.lang3.SerializationUtils;
import org.zlab.upfuzz.Parameter;

public class HBaseColumnFamily implements Serializable {
    public String name;
    public List<Parameter> colName2Type = new ArrayList<>();
    // Doesn't support composite key now

    public Set<String> indexes;

    // public void addColumn(Parameter colName2Type){
    // this.colName2Type.add(SerializationUtils.clone(colName2Type));
    // }
    // Because the column within the column family is created dynamically

    public HBaseColumnFamily(String name, Parameter colName2Type) {
        this.name = name;
        if (colName2Type != null) {
            this.colName2Type = new LinkedList<>();
            for (Parameter col : (List<Parameter>) colName2Type.getValue()) {
                this.colName2Type.add(SerializationUtils.clone(col));
            }
        }

        indexes = new HashSet<>();
    }

    // add a column 'colName2Type' to the column family
    public void addColName2Type(Parameter colName2Type) {
        if (!this.colName2Type.contains(colName2Type))
            this.colName2Type.add(colName2Type);
    }

    // get a random column from the column family
    public Parameter getRandomQualifier() {
        int size = this.colName2Type.size();
        if (size == 0) {
            return null;
        }
        int idx = (int) (Math.random() * size);
        return this.colName2Type.get(idx);
    }
}