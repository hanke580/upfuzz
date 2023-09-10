package org.zlab.upfuzz.hbase.tools;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class BALANCER extends HBaseCommand {
    // read the status
    public BALANCER(HBaseState state) {
        super(state);
        Parameter opt = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("force"), null)
                        .generateRandomParameter(state, this, null);
        this.params.add(opt);
    }

    @Override
    public String constructCommandString() {
        String opt = params.get(0).toString().isEmpty() ? ""
                : " " + "'" + params.get(0) + "'";
        return "balancer" + opt;
    }

    @Override
    public void updateState(State state) {
    }
}
