package org.zlab.upfuzz.hbase.tools;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.utils.BOOLType;

public class BALANCE_SWITCH_W extends HBaseCommand {
    // read the status
    public BALANCE_SWITCH_W(HBaseState state) {
        super(state);
        Parameter enableOpt = new BOOLType().generateRandomParameter(state,
                this, null);
        this.params.add(enableOpt);
    }

    @Override
    public String constructCommandString() {
        return "balance_switch" + " " + params.get(0);
    }

    @Override
    public void updateState(State state) {
    }
}
