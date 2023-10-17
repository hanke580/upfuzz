package org.zlab.upfuzz.hbase.namespace;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.utils.UUIDType;

public class CREATE_NAMESPACE extends HBaseCommand {

    public CREATE_NAMESPACE(HBaseState state) {
        super(state);
        ParameterType.ConcreteType nsNameType = new ParameterType.NotInCollectionType(
                new ParameterType.NotEmpty(UUIDType.instance),
                (s, c) -> ((HBaseState) s).getNamespaces(), null);
        Parameter nsName = nsNameType
                .generateRandomParameter(state, this);
        this.params.add(nsName);
    }

    @Override
    public String constructCommandString() {
        // create_namespace 'my_namespace'
        return "create_namespace " + "'" + params.get(0).getValue() + "'";
    }

    @Override
    public void updateState(State state) {
        ((HBaseState) state).addNamespace(params.get(0).getValue().toString());
    }

    @Override
    public void separate(State state) {
        this.params.get(0).regenerate(null, this);
    }

}
