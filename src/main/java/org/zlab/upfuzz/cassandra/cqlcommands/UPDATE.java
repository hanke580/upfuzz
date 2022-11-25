package org.zlab.upfuzz.cassandra.cqlcommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommand;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.utils.Pair;

import java.util.Collection;
import java.util.List;

public class UPDATE extends CassandraCommand {

    public UPDATE(CassandraState state) {
        Parameter keyspaceName = chooseKeyspace(state, this, null);
        this.params.add(keyspaceName);

        Parameter TableName = chooseTable(state, this, null);
        this.params.add(TableName);

        ParameterType.ConcreteType columnsType = new ParameterType.SuperSetType(
                new ParameterType.SubsetType(null,
                        (s, c) -> ((CassandraState) s).getTable(
                                c.params.get(0).toString(),
                                c.params.get(1).toString()).colName2Type,
                        null),
                (s, c) -> ((CassandraState) s).getTable(
                        c.params.get(0).toString(),
                        c.params.get(1).toString()).primaryColName2Type,
                null);
        Parameter columns = columnsType
                .generateRandomParameter(state, this);
        this.params.add(columns);

        ParameterType.ConcreteType insertValuesType = new ParameterType.Type2ValueType(
                null, (s, c) -> (Collection) c.params.get(2).getValue(), // columns
                p -> ((Pair) ((Parameter) p).value).right);
        Parameter insertValues = insertValuesType
                .generateRandomParameter(state, this);
        this.params.add(insertValues);

        ParameterType.ConcreteType whereColumnsType = new ParameterType.NotEmpty(
                new ParameterType.FrontSubsetType(null,
                        (s, c) -> ((CassandraState) s).getTable(
                                c.params.get(0).toString(),
                                c.params.get(1)
                                        .toString()).primaryColName2Type,
                        null));
        Parameter whereColumns = whereColumnsType
                .generateRandomParameter(state, this);
        this.params.add(whereColumns); // Param4

        ParameterType.ConcreteType whereValuesType = new ParameterType.Type2ValueType(
                null, (s, c) -> (Collection) c.params.get(2).getValue(),
                p -> ((Pair) ((Parameter) p).value).right);
        Parameter whereValues = whereValuesType
                .generateRandomParameter(state, this);
        this.params.add(whereValues); // Param5
    }

    @Override
    public String constructCommandString() {

        Parameter keyspaceName = params.get(0);
        Parameter tableName = params.get(1);
        ParameterType.ConcreteType columnNameType = new ParameterType.StreamMapType(
                null, (s, c) -> (Collection) c.params.get(2).getValue(),
                p -> ((Pair) ((Parameter) p).getValue()).left);
        Parameter columnName = columnNameType.generateRandomParameter(null,
                this);
        Parameter insertValues = params.get(3);

        StringBuilder setClause = new StringBuilder();
        List<Parameter> colNames = (List<Parameter>) columnName.getValue();
        List<Parameter> colVals = (List<Parameter>) insertValues.getValue();

        assert colNames.size() == colVals.size();

        for (int i = 0; i < colNames.size(); i++) {
            setClause.append(colNames.get(i).toString()).append(" = ")
                    .append(colVals.get(i));
            if (i != colNames.size() - 1) {
                setClause.append(",");
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE " + keyspaceName.toString() + "." +
                tableName.toString() + " " +
                "SET " + setClause + " ");

        sb.append("WHERE" + " ");

        ParameterType.ConcreteType whereColumnsType = new ParameterType.StreamMapType(
                null, (s, c) -> (Collection) c.params.get(4).getValue(),
                p -> ((Pair<Parameter, Parameter>) ((Parameter) p).value).left);

        List<Parameter> whereColumns = (List<Parameter>) whereColumnsType
                .generateRandomParameter(null, this).getValue();
        List<Parameter> whereValues = (List<Parameter>) this.params.get(5)
                .getValue();

        assert whereValues.size() == whereValues.size();

        for (int i = 0; i < whereColumns.size(); i++) {
            sb.append(whereColumns.get(i).toString() + " = "
                    + whereValues.get(i).toString());
            if (i < whereColumns.size() - 1) {
                sb.append(" AND ");
            }
        }
        sb.append(";");

        return sb.toString();
    }

    @Override
    public void updateState(State state) {

    }
}
