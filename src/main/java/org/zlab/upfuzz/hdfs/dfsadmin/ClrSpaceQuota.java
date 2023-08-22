package org.zlab.upfuzz.hdfs.dfsadmin;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSDirPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.Utilities;

public class ClrSpaceQuota extends Dfsadmin {

    public ClrSpaceQuota(HdfsState state) {
        super(state.subdir);

        Parameter cmd = new CONSTANTSTRINGType("-clrSpaceQuota")
                .generateRandomParameter(null,
                        null);
        Parameter storageTypeCmd = new CONSTANTSTRINGType("-storageType")
                .generateRandomParameter(null, null);
        Parameter storage = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities.strings2Parameters(
                        storageTypeOptions),
                null).generateRandomParameter(null, null);
        Parameter dir = new HDFSDirPathType()
                .generateRandomParameter(state, null);

        params.add(cmd);
        params.add(storageTypeCmd);
        params.add(storage);
        params.add(dir);
    }

    @Override
    public String constructCommandString() {
        return constructCommandStringWithDirSeparation("dfsadmin");
    }

    @Override
    public void updateState(State state) {
    }
}
