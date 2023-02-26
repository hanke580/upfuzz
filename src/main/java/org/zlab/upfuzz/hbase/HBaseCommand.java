package org.zlab.upfuzz.hbase;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.Utilities;

public abstract class HBaseCommand extends Command {
    public static final boolean DEBUG = false;

    public static HBaseCommandPool hBaseCommandPool = new HBaseCommandPool();


    public static Parameter chooseKeyspace(State state, Command command,
                                           Object init) {

        ParameterType.ConcreteType keyspaceNameType = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities.strings2Parameters(
                        ((HBaseState) s).keyspace2tables.keySet()),
                null);
        return keyspaceNameType.generateRandomParameter(state, command, init);
    }

    public static Parameter chooseTable(State state, Command command,
                                        Object init) {

        ParameterType.ConcreteType tableNameType = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities
                        .strings2Parameters(((HBaseState) s).keyspace2tables
                                .get(c.params.get(0).toString())
                                .keySet()),
                null);
        return tableNameType.generateRandomParameter(state, command, init);
    }

    @Override
    public void separate(State state) {
    }
}
