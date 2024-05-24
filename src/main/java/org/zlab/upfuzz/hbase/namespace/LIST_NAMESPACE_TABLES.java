package org.zlab.upfuzz.hbase.namespace;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class LIST_NAMESPACE_TABLES extends HBaseCommand {

    boolean validConstruction;

    public LIST_NAMESPACE_TABLES(HBaseState state) {
        super(state);
        validConstruction = true;
// FIXME: make sure the ns is empty, otherwise error will be thrown
        try {
            Parameter nsName = chooseNamespace(state, this, null);
            this.params.add(nsName);
        } catch (Exception e) {
            validConstruction = false;
        }
    }

    @Override
    public String constructCommandString() {
        if (!validConstruction)
            return "list_namespace_tables ";
        // create_namespace 'my_namespace'
        return "list_namespace_tables " + "'" + params.get(0).getValue() + "'";
    }

    @Override
    public void updateState(State state) {
        if (!validConstruction)
            return;
        ((HBaseState) state).dropNamespace(params.get(0).getValue().toString());
    }
}
