package org.zlab.upfuzz.hdfs.dfscommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.RandomHadoopPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class CountCommand extends DfsCommand {

    /**
     * bin/hdfs dfs -count -q -h -t ARCHIVE /dir
     */
    public CountCommand(HdfsState hdfsState) {
        Parameter countCmd = new CONSTANTSTRINGType("-count")
                .generateRandomParameter(null, null);

        Parameter countOptCmd = new CONSTANTSTRINGType("-q -h -t")
                .generateRandomParameter(null, null);

        Parameter storageType = new CONSTANTSTRINGType("DISK")
                .generateRandomParameter(null, null);

        Parameter dir = new RandomHadoopPathType()
                .generateRandomParameter(hdfsState, null);

        params.add(countCmd);
        params.add(countOptCmd);
        params.add(storageType);
        params.add(dir);
    }

    @Override
    public void updateState(State state) {

    }
}
