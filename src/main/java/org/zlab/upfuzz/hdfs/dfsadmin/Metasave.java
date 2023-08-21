package org.zlab.upfuzz.hdfs.dfsadmin;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSFilePathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class Metasave extends Dfsadmin {

    public Metasave(HdfsState state) {
        super(state.subdir);

        Parameter cmd = new CONSTANTSTRINGType("-metasave")
                .generateRandomParameter(null,
                        null);
        Parameter file = new HDFSFilePathType()
                .generateRandomParameter(state, null);

        params.add(cmd);
        params.add(file);
    }

    @Override
    public String constructCommandString() {
        return constructCommandStringWithDirSeparation();
    }

    @Override
    public void updateState(State state) {
    }
}