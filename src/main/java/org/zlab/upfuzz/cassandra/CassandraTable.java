package org.zlab.upfuzz.cassandra;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.utils.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CassandraTable {
    public String name;
    public List<Parameter> colName2Type;
    public List<Parameter> primaryColName2Type;

    public CassandraTable(Parameter name, Parameter colName2Type, Parameter primaryColName2Type) {
        this.name = (String) name.getValue();
        if (colName2Type != null) {
            this.colName2Type = (List<Parameter>) colName2Type.getValue();
        }
        if (primaryColName2Type != null) {
            this.primaryColName2Type = (List<Parameter>) primaryColName2Type.getValue();
        }
    }
}
