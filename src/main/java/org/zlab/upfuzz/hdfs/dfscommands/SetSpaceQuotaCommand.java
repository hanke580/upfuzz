package org.zlab.upfuzz.hdfs.dfscommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.RandomHadoopPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.INTType;
import org.zlab.upfuzz.utils.Utilities;

import java.util.LinkedList;
import java.util.List;

public class SetSpaceQuotaCommand extends DfsadminCommand {

    /**
     * hdfs dfsadmin -setSpaceQuota <N> -storageType <storagetype> <directory>...<directory>
     */
    public static List<String> storageTypeOptions = new LinkedList<>();

    static {
        storageTypeOptions.add("SSD");
        storageTypeOptions.add("DISK");
        storageTypeOptions.add("ARCHIVE");
    }

    public SetSpaceQuotaCommand(HdfsState hdfsState) {
        Parameter setSpaceQuotaCmd = new CONSTANTSTRINGType("-setSpaceQuota")
                .generateRandomParameter(null, null);

        Parameter quota = new INTType(0, 100).generateRandomParameter(null,
                null);

        Parameter storage = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities.strings2Parameters(
                        (((SetSpaceQuotaCommand) c).storageTypeOptions)),
                null).generateRandomParameter(null, null);

        Parameter dir = new RandomHadoopPathType()
                .generateRandomParameter(hdfsState, null);

        params.add(setSpaceQuotaCmd);
        params.add(quota);
        params.add(storage);
        params.add(dir);
    }

    @Override
    public void updateState(State state) {

    }
}
