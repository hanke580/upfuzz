package org.zlab.upfuzz.hdfs.dfs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSFilePathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class Cat extends Dfs {

    /*
     * Moves files from source to destination. This command allows multiple
     * sources as well in which case the destination needs to be a directory.
     * Moving files across file systems is not permitted.
     */
    public Cat(HdfsState hdfsState) {
        super(hdfsState.subdir);

        Parameter catCmd = new CONSTANTSTRINGType("-cat")
                .generateRandomParameter(hdfsState, null);

        // The -ignoreCrc option disables checkshum verification.
        Parameter crcOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-ignoreCrc"), null)
                        .generateRandomParameter(hdfsState, null);

        Parameter pathParameter = new HDFSFilePathType()
                .generateRandomParameter(hdfsState, null);

        params.add(catCmd);
        params.add(crcOption);
        params.add(pathParameter);
    }

    @Override
    public String constructCommandString() {
        return "dfs" + " " +
                params.get(0) + " " +
                params.get(1) + " " +
                subdir +
                params.get(2);
    }

    @Override
    public void updateState(State state) {
    }
}
