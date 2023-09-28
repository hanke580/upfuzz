package org.zlab.upfuzz.cassandra;

import java.io.Serializable;
import java.util.*;
import org.apache.commons.lang3.SerializationUtils;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.utils.Pair;

public class CassandraTable implements Serializable {
    public String name;
    public List<Parameter> colName2Type;
    public List<Parameter> primaryColName2Type;
    // Doesn't support composite key now

    public Set<String> indexes;

    public CassandraTable(Parameter name, Parameter colName2Type,
            Parameter primaryColName2Type) {
        this.name = (String) name.getValue();
        if (colName2Type != null) {
            this.colName2Type = new LinkedList<>();
            for (Parameter col : (List<Parameter>) colName2Type.getValue()) {
                this.colName2Type.add(SerializationUtils.clone(col));
            }
        }
        if (primaryColName2Type != null) {
            this.primaryColName2Type = new LinkedList<>();
            for (Parameter primaryCol : (List<Parameter>) primaryColName2Type
                    .getValue()) {
                this.primaryColName2Type.add(
                        SerializationUtils.clone(primaryCol));
            }
        }
        indexes = new HashSet<>();
    }
}
