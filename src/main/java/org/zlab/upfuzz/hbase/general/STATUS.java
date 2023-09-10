package org.zlab.upfuzz.hbase.general;

import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.hbase.HbaseSimpleCommandHelper;

public class STATUS extends HbaseSimpleCommandHelper {
    public STATUS(HBaseState state) {
        super(state, "status");
    }
}
