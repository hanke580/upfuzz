package org.zlab.upfuzz.hdfs.dfsadmin;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class RefreshSuperUserGroupsConfiguration extends Dfsadmin {

    public RefreshSuperUserGroupsConfiguration(HdfsState state) {
        super(state.subdir);

        Parameter cmd = new CONSTANTSTRINGType(
                "-refreshSuperUserGroupsConfiguration")
                        .generateRandomParameter(null, null);

        params.add(cmd);
    }

    @Override
    public void updateState(State state) {
    }
}
