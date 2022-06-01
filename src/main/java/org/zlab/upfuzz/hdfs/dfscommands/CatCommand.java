package org.zlab.upfuzz.hdfs.dfscommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.hdfs.HDFSParameterType.RandomHadoopPathType;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class CatCommand extends DfsCommand {

    /*
     * Moves files from source to destination. This command allows multiple sources as well in which case the destination needs to be a directory. Moving files across file systems is not permitted.
     */
    public CatCommand(HdfsState hdfsState) {
        Parameter catCmd = new CONSTANTSTRINGType("-cat")
                .generateRandomParameter(null, null);

        Parameter pathParameter = new RandomHadoopPathType()
                .generateRandomParameter(hdfsState, null);

        // The -ignoreCrc option disables checkshum verification.
        Parameter crcOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-ignoreCrc"), null)
                        .generateRandomParameter(null, null);

        params.add(catCmd);
        params.add(crcOption);
        params.add(pathParameter);
    }

    @Override
    public void updateState(State state) {
    }
}
