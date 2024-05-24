package org.zlab.upfuzz.hbase.dml;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseState;

public class APPEND extends PUT_MODIFY {

    // append '<table_name>', '<row_key>', '<family:qualifier>', '<value>'
    public APPEND(HBaseState state) {
        super(state);
    }

    @Override
    public String constructCommandString() {
        // the output of the super's method is 'put ....', replace 'put' with
        // 'append'
        return "append" + super.constructCommandString().substring(3);
    }

    @Override
    public void updateState(State state) {
    }
}
