package org.zlab.upfuzz.cassandra;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.utils.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CassandraTable {
    public String name;
    public List<Pair<String, CassandraTypes.TYPEType>> colName2Type;
    public List<Pair<String, CassandraTypes.TYPEType>> primaryColName2Type;

    public CassandraTable(Parameter name, Parameter colName2Type, Parameter primaryColName2Type) {
        this.name = (String) name.value;
        this.colName2Type = (List<Pair<String, CassandraTypes.TYPEType>>) colName2Type.value;
        this.primaryColName2Type = (List<Pair<String, CassandraTypes.TYPEType>>) primaryColName2Type.value;
    }
}
