package org.zlab.upfuzz.cassandra.cqlcommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommand;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.STRINGType;

import java.util.List;

public class ALTER_TABLE_RENAME extends CassandraCommand {
    /**
     * You can only rename clustering columns, which are part of the primary key.
     * You cannot rename the partition key.
     * You can index a renamed column.
     * You cannot rename a column if an index has been created on it.
     * You cannot rename a static column, since you cannot use a static column in the table's primary key.
     *
     * Not fully implement these restrictions, exception might be thrown.
     * * Only make sure the column to be rename is in PRIMARY KEY
     */
    public ALTER_TABLE_RENAME(CassandraState cassandraState) {
        Parameter keyspaceName = chooseKeyspace(cassandraState, this, null);
        params.add(keyspaceName);

        Parameter TableName = chooseTable(cassandraState, this, null);
        params.add(TableName);

        Parameter targetColumn = new ParameterType.InCollectionType(null,
                (s, c) -> ((CassandraState) s).getTable(
                        c.params.get(0).toString(),
                        c.params.get(1).toString()).primaryColName2Type,
                null, null)
                        .generateRandomParameter(cassandraState, this);
        params.add(targetColumn);

        Parameter newColumnName = new ParameterType.NotInCollectionType(
                new STRINGType(Config.getConf().CASSANDRA_COLUMN_NAME_MAX_SIZE),
                (s, c) -> ((CassandraState) s).getTable(
                        c.params.get(0).toString(),
                        c.params.get(1).toString()).colName2Type,
                p -> ((Pair) (((Parameter) p).getValue())).left)
                        .generateRandomParameter(cassandraState, this);
        params.add(newColumnName);
    }

    @Override
    public String constructCommandString() {

        String targetColumnName = ((Pair) params.get(2).getValue()).left
                .toString();

        StringBuilder sb = new StringBuilder();
        sb.append("ALTER TABLE");
        sb.append(" " + this.params.get(0) + "."
                + this.params.get(1).toString() + " ");
        sb.append("RENAME");
        sb.append(" " + targetColumnName + " ");
        sb.append("TO" + " " + this.params.get(3).toString() + ";");
        return sb.toString();
    }

    @Override
    public void updateState(State state) {
        // The targetColumn should be the same object with the one in the state
        // Way1: Directly modify this object, but not sure whether part of the
        // PARI can be modified
        // Way2: Create a new column type (name, type), remove the original one
        // and add this one

        // Try way1
        // Can use setValue()?
        Parameter targetCol = params.get(2);
        String oriName = ((Pair) targetCol.getValue()).left.toString();
        String newName = params.get(3).toString();

        List<Parameter> cols = ((CassandraState) state).getTable(
                this.params.get(0).toString(),
                this.params.get(1).toString()).colName2Type;

        List<Parameter> priCols = ((CassandraState) state).getTable(
                this.params.get(0).toString(),
                this.params.get(1).toString()).primaryColName2Type;

        // Update the cols
        for (Parameter col : cols) {
            if (((Parameter) ((Pair) col.getValue()).left).getValue().toString()
                    .equals(oriName)) {
                ((Parameter) ((Pair) col.getValue()).left).setValue(newName);
            }
        }
        // Update the primary cols
        for (Parameter col : priCols) {
            if (((Parameter) ((Pair) col.getValue()).left).getValue().toString()
                    .equals(oriName)) {
                ((Parameter) ((Pair) col.getValue()).left).setValue(newName);
            }
        }

    }
}
