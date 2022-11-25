package org.zlab.upfuzz.hdfs.dfscommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraTypes;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSDirPathType;
import org.zlab.upfuzz.hdfs.HDFSParameterType.RandomHadoopPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.Utilities;

public class CountCommand extends DfsCommand {

    /**
     * bin/hdfs dfs -count -q -h -t ARCHIVE /dir
     */
    public CountCommand(HdfsState state) {
        super(state.subdir);

        Parameter countCmd = new CONSTANTSTRINGType("-count")
                .generateRandomParameter(null, null);

        Parameter countOptCmd = new CONSTANTSTRINGType("-q -h -t")
                .generateRandomParameter(null, null);

        Parameter storageType = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities.strings2Parameters(
                        SetSpaceQuotaCommand.storageTypeOptions),
                null).generateRandomParameter(null, null);

        Parameter dir = new HDFSDirPathType()
                .generateRandomParameter(state, null);

        params.add(countCmd);
        params.add(countOptCmd);
        params.add(storageType);
        params.add(dir);
    }

    @Override
    public String constructCommandString() {
        return "dfs" + " " +
                params.get(0) + " " +
                params.get(1) + " " +
                params.get(2) + " " +
                subdir +
                params.get(3);
    }

    @Override
    public void updateState(State state) {

    }
}
