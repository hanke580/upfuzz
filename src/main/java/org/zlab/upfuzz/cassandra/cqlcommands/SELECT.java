package org.zlab.upfuzz.cassandra.cqlcommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommand;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.utils.BOOLType;
import org.zlab.upfuzz.utils.Pair;

import java.util.Collection;
import java.util.List;

/**
 * SELECT * | select_expression | DISTINCT partition
 * FROM [keyspace_name.] table_name
 * [WHERE partition_value
 *    [AND clustering_filters
 *    [AND static_filters]]]
 * [ORDER BY PK_column_name ASC|DESC]
 * [LIMIT N]
 * [ALLOW FILTERING]
 */
public class SELECT extends CassandraCommand {

    public SELECT(State state, Object init0, Object init1, Object init2,
            Object init3, Object init4) {
        super();

        assert state instanceof CassandraState;
        CassandraState cassandraState = (CassandraState) state;

        Parameter keyspaceName = chooseKeyspace(cassandraState, this,
                init0);
        this.params.add(keyspaceName); // [0]

        Parameter TableName = chooseTable(cassandraState, this, init1);
        this.params.add(TableName); // [1]

        ParameterType.ConcreteType selectColumnsType = new ParameterType.SubsetType<>(
                null,
                (s, c) -> ((CassandraState) s).getTable(
                        c.params.get(0).toString(),
                        c.params.get(1).toString()).colName2Type,
                p -> ((Pair<Parameter, Parameter>) (((Parameter) p)
                        .getValue())).left);
        Parameter selectColumns = selectColumnsType
                .generateRandomParameter(state, this, init2);
        this.params.add(selectColumns); // Param2

        ParameterType.ConcreteType whereColumnsType = new ParameterType.FrontSubsetType(
                null,
                (s, c) -> ((CassandraState) s).getTable(
                        c.params.get(0).toString(),
                        c.params.get(1).toString()).primaryColName2Type,
                null);
        Parameter whereColumns = whereColumnsType
                .generateRandomParameter(state, this, init3);
        this.params.add(whereColumns); // Param 3

        ParameterType.ConcreteType whereValuesType = new ParameterType.Type2ValueType(
                null, (s, c) -> (Collection) c.params.get(3).getValue(),
                p -> ((Pair<?, ?>) ((Parameter) p).value).right);
        Parameter insertValues = whereValuesType
                .generateRandomParameter(state, this, init4);
        this.params.add(insertValues); // Param4
    }

    public SELECT(State state) {

        Parameter keyspaceName = chooseKeyspace(state, this, null);
        this.params.add(keyspaceName); // Param 0

        Parameter TableName = chooseTable(state, this, null);
        this.params.add(TableName); // Param 1

        // Subset of primary columns
        ParameterType.ConcreteType selectColumnsType = new ParameterType.SubsetType<>(
                null,
                (s, c) -> ((CassandraState) s).getTable(
                        c.params.get(0).toString(),
                        c.params.get(1).toString()).colName2Type,
                p -> ((Pair<Parameter, Parameter>) (((Parameter) p)
                        .getValue())).left);
        Parameter selectColumns = selectColumnsType
                .generateRandomParameter(state, this);
        this.params.add(selectColumns); // Param2

        ParameterType.ConcreteType whereColumnsType = new ParameterType.FrontSubsetType(
                null,
                (s, c) -> ((CassandraState) s).getTable(
                        c.params.get(0).toString(),
                        c.params.get(1).toString()).primaryColName2Type,
                null);
        Parameter whereColumns = whereColumnsType
                .generateRandomParameter(state, this);
        this.params.add(whereColumns); // Param 3

        ParameterType.ConcreteType whereValuesType = new ParameterType.Type2ValueType(
                null, (s, c) -> (Collection) c.params.get(3).getValue(),
                p -> ((Pair) ((Parameter) p).value).right);
        Parameter insertValues = whereValuesType
                .generateRandomParameter(state, this);
        this.params.add(insertValues); // Param4

        if (Config.getConf().enable_ORDERBY_IN_SELECT) {
            // Whether to use the last columns from where Columns for ORDER BY
            ParameterType.ConcreteType useOrderType = new BOOLType();
            Parameter useOrder = useOrderType.generateRandomParameter(state,
                    this);
            this.params.add(useOrder); // Param 5

            // Use ASC or DESC
            // true: ASC, false: DESC
            ParameterType.ConcreteType ascOrdescType = new BOOLType();
            Parameter ascOrdesc = ascOrdescType.generateRandomParameter(state,
                    this);
            this.params.add(ascOrdesc); // Param 6
        }
    }

    @Override
    public String constructCommandString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ");
        if (params.get(2).isEmpty(null, this)) {
            sb.append("* ");
        } else {
            List<Parameter> selectColumns = (List<Parameter>) params.get(2)
                    .getValue();
            for (int i = 0; i < selectColumns.size(); i++) {
                sb.append(selectColumns.get(i).toString());
                if (i < selectColumns.size() - 1) {
                    sb.append(", ");
                }
            }
        }
        sb.append(" FROM ").append(params.get(0)).append(".")
                .append(params.get(1));

        int whereColsSize = ((List<?>) params.get(3).getValue()).size();
        if (whereColsSize > 0) {

            sb.append(" " + "WHERE" + " ");
            ParameterType.ConcreteType whereColumnsType = new ParameterType.StreamMapType(
                    null, (s, c) -> (Collection) c.params.get(3).getValue(),
                    p -> ((Pair<Parameter, Parameter>) ((Parameter) p).value).left);

            List<Parameter> whereColumns = (List<Parameter>) whereColumnsType
                    .generateRandomParameter(null, this).getValue();
            List<Parameter> whereValues = (List<Parameter>) this.params
                    .get(4).getValue();

            assert whereColumns.size() == whereValues.size();

            // Has a where clause
            if (whereColsSize > 1 && params.size() == 7
                    && (Boolean) params.get(5).value) {
                // has an [order by] clause
                int i = 0;
                for (; i < whereColumns.size() - 1; i++) {
                    sb.append(whereColumns.get(i).toString()).append(" = ")
                            .append(whereValues.get(i).toString());
                    if (i < whereColumns.size() - 2) {
                        sb.append(" AND ");
                    }
                }

                if (Config.getConf().enable_ORDERBY_IN_SELECT) {
                    String order = (Boolean) params.get(6).getValue() ? "ASC"
                            : "DESC";
                    // Pick the 1th column in the primary key since currently we
                    // do
                    // not support composite partition key, so the second column
                    // is
                    // always the first clustering column
                    sb.append(" ORDER BY ")
                            .append(whereColumns.get(1).toString()).append(" ")
                            .append(order);
                }
            } else {
                for (int i = 0; i < whereColumns.size(); i++) {
                    sb.append(whereColumns.get(i).toString()).append(" = ")
                            .append(whereValues.get(i).toString());
                    if (i < whereColumns.size() - 1) {
                        sb.append(" AND ");
                    }
                }
            }
        }
        sb.append(";");
        return sb.toString();
    }

    @Override
    public void updateState(State state) {
    }
}
