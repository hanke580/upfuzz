package org.zlab.upfuzz.hdfs.dfs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.*;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class Cp extends Dfs {

    /*
     * Copy files from source to destination. This command allows multiple
     * sources as well in which case the destination must be a directory.
     */
    public Cp(HdfsState state) {
        super(state.subdir);

        Parameter cpcmd = new CONSTANTSTRINGType("-cp")
                .generateRandomParameter(null, null);

        // -f : Overwrites the destination if it already exists.
        Parameter fOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-f"), null)
                        .generateRandomParameter(null, null);

        Parameter pOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-p"), null)
                        .generateRandomParameter(null, null);

        // -d : Skip creation of temporary file with the suffix ._COPYING_.
        Parameter dOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-d"), null)
                        .generateRandomParameter(null, null);

        Parameter srcParameter = new HDFSRandomPathType()
                .generateRandomParameter(state, null);

        Parameter dstParameter = new HDFSDirPathType()
                .generateRandomParameter(state, null);

        params.add(cpcmd);
        params.add(fOption);
        params.add(pOption);
        params.add(dOption);
        params.add(srcParameter);
        params.add(dstParameter);
    }

    @Override
    public String constructCommandString() {
        return "dfs" + " " +
                params.get(0) + " " +
                params.get(1) + " " +
                params.get(2) + " " +
                params.get(3) + " " +
                subdir +
                params.get(4) + " " +
                subdir +
                params.get(5);
    }

    @Override
    public void updateState(State state) {
        HdfsState hdfsState = (HdfsState) state;
        for (String dir : hdfsState.dfs.getDirs(params.get(6).toString())) {
            String newDir = dir.replace(params.get(6).toString(),
                    params.get(7).toString());
            hdfsState.dfs.createDir(newDir);
        }
        for (String file : hdfsState.dfs.getFiles(params.get(6).toString())) {
            String newFile = file.replace(params.get(6).toString(),
                    params.get(7).toString());
            hdfsState.dfs.createFile(newFile);
        }
    }
}
