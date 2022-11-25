package org.zlab.upfuzz.cassandra.cqlcommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommand;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.cassandra.CassandraTable;
import org.zlab.upfuzz.cassandra.CassandraTypes;
import org.zlab.upfuzz.utils.PAIRType;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.STRINGType;

import java.util.LinkedList;
import java.util.List;

public class ALTER_TABLE_TYPE extends CassandraCommand {

    public ALTER_TABLE_TYPE(CassandraState cassandraState) {
        super();

        Parameter keyspaceName = chooseKeyspace(cassandraState, this, null);
        this.params.add(keyspaceName);

        Parameter TableName = chooseTable(cassandraState, this, null);
        this.params.add(TableName);

        Parameter targetColumn = new ParameterType.InCollectionType(null,
                (s, c) -> ((CassandraState) s).getTable(
                        c.params.get(0).toString(),
                        c.params.get(1).toString()).primaryColName2Type,
                null, null)
                        .generateRandomParameter(cassandraState, this);
        this.params.add(targetColumn);

        ParameterType.ConcreteType newTypeType = CassandraTypes.TYPEType.instance;
        Parameter newType = newTypeType
                .generateRandomParameter(cassandraState, this);
        this.params.add(newType);
    }

    @Override
    public String constructCommandString() {
        Parameter targetColumnName = ((Pair<Parameter, Parameter>) this.params
                .get(2).getValue()).left;
        return "ALTER TABLE" +
                " " + this.params.get(0) + "."
                + this.params.get(1).toString() + " " +
                "ALTER" +
                " " + targetColumnName.toString() + " TYPE "
                + this.params.get(3).toString() + " ;";
    }

    @Override
    public void updateState(State state) {
        ParameterType.ConcreteType columnType = ParameterType.ConcreteGenericType
                .constructConcreteGenericType(PAIRType.instance,
                        new ParameterType.NotEmpty(new STRINGType(10)),
                        CassandraTypes.TYPEType.instance);

        Parameter p = new Parameter(columnType,
                new Pair<>(params.get(2), params.get(3)));
        CassandraTable table = ((CassandraState) state).getTable(
                this.params.get(0).toString(),
                this.params.get(1).toString());

        List<Parameter> newColName2Type = new LinkedList<>();
        List<Parameter> newPrimaryColName2Type = new LinkedList<>();

        for (Parameter parameter : table.colName2Type) {
            if (!parameter.toString().equals(params.get(2).toString())) {
                newColName2Type.add(parameter);
            }
        }
        newColName2Type.add(p);

        for (Parameter parameter : table.primaryColName2Type) {
            if (!parameter.toString().equals(params.get(2).toString())) {
                newPrimaryColName2Type.add(parameter);
            }
        }
        newPrimaryColName2Type.add(p);
    }
}
