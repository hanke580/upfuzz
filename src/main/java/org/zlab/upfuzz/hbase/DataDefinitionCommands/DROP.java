package org.zlab.upfuzz.hbase.DataDefinitionCommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class DROP extends HBaseCommand {
    public DROP(HBaseState state) {
        Parameter keyspaceName = chooseKeyspace(state, this, null);
        this.params.add(keyspaceName); // 0

        Parameter TableName = chooseTable(state, this, null);
        this.params.add(TableName); // 1

        ParameterType.ConcreteType IF_EXISTType = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("IF EXISTS"), null // TODO: Make a
                // pure
                // CONSTANTType
        );
        Parameter IF_EXIST = IF_EXISTType
                .generateRandomParameter(state, this);
        params.add(IF_EXIST); // 2
    }

    @Override
    public String constructCommandString() {
        return "DROP " + params.get(2) +
                " " + this.params.get(0) + "."
                + this.params.get(1).toString() + ";";
    }

    @Override
    public void updateState(State state) {
        ((HBaseState) state).keyspace2tables
                .get(params.get(0).toString())
                .remove(params.get(1).toString());
    }
}
