package org.zlab.upfuzz.hbase.DataManipulationCommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.utils.Pair;

import java.util.Collection;

public class PUT extends HBaseCommand {

    /*
    * init0: 'key space name'
    * init1: 'table name'
    * init2: 'columnClusterName'
    * init3: 'columnName'
    * init3: 'value'
    * */
    // PUT 'tableName' 'columnCluster:Column' 'value'
    // keyspace,
    // primary key, index
    // PUT 'STUDENT' 'Address:City/Street' 'value'
    public PUT(HBaseState state, Object init0, Object init1, Object init2, Object init3, Object init4) {

        // 0. get key space name
        Parameter keyspaceName = chooseKeyspace(state, this, init0);
        this.params.add(keyspaceName); // [0]

        // 1. get table name
        Parameter TableName = chooseTable(state, this, init1);
        this.params.add(TableName); // [1]

        // 2. get column cluster name
        ParameterType.ConcreteType columnsClusterType = new ParameterType.SuperSetType(
                new ParameterType.SubsetType(null,
                        (s, c) -> ((HBaseState) s).getTable(
                                c.params.get(0).toString(),
                                c.params.get(1).toString()).colName2Type,
                        null),
                (s, c) -> ((HBaseState) s).getTable(
                        c.params.get(0).toString(),
                        c.params.get(1).toString()).primaryColName2Type,
                null);
        Parameter columnClusters = columnsClusterType
                .generateRandomParameter(state, this, init2);
        this.params.add(columnClusters); // [2]

        // 3. get column cluster name
        ParameterType.ConcreteType columnsType = new ParameterType.SuperSetType(
                new ParameterType.SubsetType(null,
                        (s, c) -> ((HBaseState) s).getTable(
                                c.params.get(0).toString(),
                                c.params.get(1).toString()).colName2Type,
                        null),
                (s, c) -> ((HBaseState) s).getTable(
                        c.params.get(0).toString(),
                        c.params.get(1).toString()).primaryColName2Type,
                null);
        Parameter columns = columnsType
                .generateRandomParameter(state, this, init2);
        this.params.add(columns); // [2]

        ParameterType.ConcreteType insertValuesType = new ParameterType.Type2ValueType(
                null, (s, c) -> (Collection) c.params.get(2).getValue(), // columns
                p -> ((Pair) ((Parameter) p).value).right);
        Parameter insertValues = insertValuesType
                .generateRandomParameter(state, this, init3);
        this.params.add(insertValues); // [3]
    }

    @Override
    public String constructCommandString() {
        return null;
    }

    @Override
    public void updateState(State state) {

    }
}
