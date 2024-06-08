package org.zlab.upfuzz.cassandra.cqlcommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommand;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.cassandra.CassandraTypes;
import org.zlab.upfuzz.utils.Pair;

import java.util.Collection;
import java.util.List;

/**
 * DELETE firstname, lastname
 *   FROM cycling.cyclist_name
 *   USING TIMESTAMP 1318452291034
 *   WHERE lastname = 'VOS';
 *
 *   DELETE column_name FROM table_name WHERE partition_key = value [AND clustering_key = value];
 */
public class DELETE extends CassandraCommand {

    public DELETE(State state) {
        /**
         * Delete the whole column for now.
         */
        Parameter keyspaceName = chooseKeyspace(state, this, null);
        this.params.add(keyspaceName); // Param0

        Parameter TableName = chooseTable(state, this, null);
        this.params.add(TableName); // Param1

        // Pick the subset of the primary columns, and make sure it's on the
        // right order
        // First Several Type
        ParameterType.ConcreteType whereColumnsType = new ParameterType.NotEmpty(
                new ParameterType.FrontSubsetType(null,
                        (s, c) -> ((CassandraState) s).getTable(
                                c.params.get(0).toString(),
                                c.params.get(1)
                                        .toString()).primaryColName2Type,
                        null));
        Parameter whereColumns = whereColumnsType
                .generateRandomParameter(state, this);
        this.params.add(whereColumns); // Param2

        ParameterType.ConcreteType whereValuesType = new ParameterType.Type2ValueType(
                null, (s, c) -> (Collection) c.params.get(2).getValue(),
                p -> ((Pair) ((Parameter) p).value).right);
        Parameter insertValues = whereValuesType
                .generateRandomParameter(state, this);
        this.params.add(insertValues); // Param3

        Parameter regColumnType = new CassandraTypes.RegColumnType()
                .generateRandomParameter(state, this);
        this.params.add(regColumnType); // Param4
    }

    @Override
    public String constructCommandString() {
        StringBuilder sb = new StringBuilder();

        String deleteCellName = this.params.get(4).toString();
        sb.append("DELETE" + " ");
        if (!deleteCellName.isEmpty())
            sb.append(deleteCellName + " ");
        sb.append("FROM" + " ");
        sb.append(params.get(0) + "." + params.get(1).toString());
        sb.append(" " + "WHERE" + " ");

        ParameterType.ConcreteType whereColumnsType = new ParameterType.StreamMapType(
                null, (s, c) -> (Collection) c.params.get(2).getValue(),
                p -> ((Pair<Parameter, Parameter>) ((Parameter) p).value).left);

        List<Parameter> whereColumns = (List<Parameter>) whereColumnsType
                .generateRandomParameter(null, this).getValue();
        List<Parameter> whereValues = (List<Parameter>) this.params.get(3)
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