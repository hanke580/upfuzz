package org.zlab.upfuzz.hbase;

import java.io.Serializable;
import java.util.*;
import org.apache.commons.lang3.SerializationUtils;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.utils.Pair;

public class HBaseTable implements Serializable {
    public String name;
    public List<Parameter> colName2Type;
    public List<Parameter> primaryColName2Type;
    // Doesn't support composite key now

    public Set<String> indexes;

    public HBaseTable(Parameter name, Parameter colName2Type) {
        this.name = (String) name.getValue();
        if (colName2Type != null) {
            this.colName2Type = new LinkedList<>();
            for (Parameter col : (List<Parameter>) colName2Type.getValue()) {
                this.colName2Type.add(SerializationUtils.clone(col));
            }
        }

        indexes = new HashSet<>();
    }
}