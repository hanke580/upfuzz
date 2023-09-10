package org.zlab.upfuzz.hbase.rsgroup;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.utils.UUIDType;

public class ADD_RSGROUP extends HBaseCommand {

    public ADD_RSGROUP(HBaseState state) {
        super(state);
        ParameterType.ConcreteType nameType = new ParameterType.NotInCollectionType(
                new ParameterType.NotEmpty(UUIDType.instance),
                (s, c) -> ((HBaseState) s).getNamespaces(), null);
        Parameter name = nameType
                .generateRandomParameter(state, this);
        this.params.add(name);
    }

    @Override
    public String constructCommandString() {
        // create_namespace 'my_namespace'
        return "add_rsgroup " + "'" + params.get(0).getValue() + "'";
    }

    @Override
    public void updateState(State state) {
    }
}
