package org.zlab.upfuzz.hbase.rsgroup;

import org.zlab.upfuzz.hbase.HbaseSingleTableCommandHelper;
import org.zlab.upfuzz.hbase.HBaseState;

public class GET_TABLE_RSGROUP extends HbaseSingleTableCommandHelper {
    public GET_TABLE_RSGROUP(HBaseState state) {
        super(state, "get_table_rsgroup");
    }
}
