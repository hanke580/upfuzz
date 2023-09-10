package org.zlab.upfuzz.hbase.namespace;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

public class DROP_NAMESPACE extends HBaseCommand {

    public DROP_NAMESPACE(HBaseState state) {
        super(state);
// FIXME: make sure the ns is empty, otherwise error will be thrown
        Parameter nsName = chooseNamespace(state, this, null);
        this.params.add(nsName);
    }

    @Override
    public String constructCommandString() {
        // create_namespace 'my_namespace'
        return "drop_namespace " + "'" + params.get(0).getValue() + "'";
    }

    @Override
    public void updateState(State state) {
        ((HBaseState) state).dropNamespace(params.get(0).getValue().toString());
    }
}
