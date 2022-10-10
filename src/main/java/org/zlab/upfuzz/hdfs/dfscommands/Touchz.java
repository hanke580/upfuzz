package org.zlab.upfuzz.hdfs.dfscommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.RandomHadoopPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class Touchz extends DfsCommand {
    public Touchz(HdfsState hdfsState) {
        Parameter mkdirCmd = new CONSTANTSTRINGType("-touchz")
                .generateRandomParameter(null, null);
        params.add(mkdirCmd);

        Parameter pathParameter = new RandomHadoopPathType()
                .generateRandomParameter(hdfsState, null);

        params.add(pathParameter);
    }

    @Override
    public void updateState(State state) {
        // Add a real inode to state

    }
}
