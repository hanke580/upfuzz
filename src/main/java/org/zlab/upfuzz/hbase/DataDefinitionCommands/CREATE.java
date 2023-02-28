package org.zlab.upfuzz.hbase.DataDefinitionCommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseTable;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.hbase.HBaseTypes;
import org.zlab.upfuzz.utils.*;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class CREATE extends HBaseCommand {

    public static List<String> speculative_retryOptions = new LinkedList<>();

    static {
        speculative_retryOptions.add("50ms");
        speculative_retryOptions.add("90MS");
        speculative_retryOptions.add("99PERCENTILE");
        speculative_retryOptions.add("40percentile");
        speculative_retryOptions.add("ALWAYS");
        speculative_retryOptions.add("always");
        speculative_retryOptions.add("NONE");
        speculative_retryOptions.add("none");
    }

    public CREATE(HBaseState state, Object init0, Object init1, Object init2){

        // 0. get keyspace name
        Parameter keyspaceName = chooseKeyspace(state, this, init0);
        params.add(keyspaceName); // [0]

        // 1. get table name
        ParameterType.ConcreteType tableNameType = new ParameterType.NotInCollectionType(
                new ParameterType.NotEmpty(new STRINGType(10)),
                (s, c) -> Utilities
                        .strings2Parameters(((HBaseState) s).keyspace2tables
                                .get(this.params.get(0).toString()).keySet()),
                null);
        Parameter tableName = tableNameType
                .generateRandomParameter(state, this, init1);
        params.add(tableName); // [1]

        // 2. get columns name
        ParameterType.ConcreteType columnsType = // LIST<PAIR<String,TYPEType>>
                new ParameterType.NotEmpty(ParameterType.ConcreteGenericType
                        .constructConcreteGenericType(
                                HBaseTypes.MapLikeListType.instance,
                                ParameterType.ConcreteGenericType
                                        .constructConcreteGenericType(
                                                PAIRType.instance,
                                                new ParameterType.NotEmpty(
                                                        new STRINGType(20)),
                                                HBaseTypes.TYPEType.instance)));
        Parameter columns = columnsType
                .generateRandomParameter(state, this, init2);
        params.add(columns); // [2]
    }

    @Override
    public String constructCommandString() {
        // TODO: Need a helper function, add space between all strings
        Parameter keyspaceName = params.get(0);
        Parameter tableName = params.get(1);
        Parameter columns = params.get(2); // LIST<PAIR<TEXTType,TYPE>>

        return "CREATE TABLE "
                + keyspaceName.toString() + "." + tableName.toString()
                + columns.toString();
    }

    @Override
    public void updateState(State state) {
        Parameter keyspaceName = params.get(0);
        Parameter tableName = params.get(1);
        Parameter columns = params.get(2); // LIST<PAIR<TEXTType,TYPE>>

        HBaseTable table = new HBaseTable(tableName, columns);
        ((HBaseState) state).addTable(keyspaceName.toString(),
                tableName.toString(), table);
    }
}
