package org.zlab.upfuzz.hdfs.dfscommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.RandomHadoopPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.INTType;

public class SetSpaceQuotaCommand extends  DfsadminCommand {

    /**
     * hdfs dfsadmin -setSpaceQuota <N> -storageType <storagetype> <directory>...<directory>
     */

    public SetSpaceQuotaCommand(HdfsState hdfsState) {
        Parameter setSpaceQuotaCmd = new CONSTANTSTRINGType("-setSpaceQuota")
                .generateRandomParameter(null, null);

        Parameter quota = new INTType(0, 100).generateRandomParameter(null, null);

        Parameter storageType = new CONSTANTSTRINGType("DISK")
                .generateRandomParameter(null, null);

        Parameter dir = new RandomHadoopPathType()
                .generateRandomParameter(hdfsState, null);

        params.add(setSpaceQuotaCmd);
        params.add(quota);
        params.add(storageType);
        params.add(dir);
    }

    @Override
    public void updateState(State state) {

    }
}
