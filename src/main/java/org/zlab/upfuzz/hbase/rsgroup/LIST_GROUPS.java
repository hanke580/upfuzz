package org.zlab.upfuzz.hbase.rsgroup;

import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.hbase.HbaseSimpleCommandHelper;

public class LIST_GROUPS extends HbaseSimpleCommandHelper {
    public LIST_GROUPS(HBaseState state) {
        super(state, "list_rsgroups");
    }
}
