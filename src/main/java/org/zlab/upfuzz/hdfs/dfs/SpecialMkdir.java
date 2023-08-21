package org.zlab.upfuzz.hdfs.dfs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class SpecialMkdir extends Dfs {

    /**
     * THis is a special command, it cannot be mutate, it will always be a
     * hdfs dfs mkdir /UUID/
     */
    public SpecialMkdir(HdfsState state) {
        super(state.subdir);

        Parameter mkdirCmd = new CONSTANTSTRINGType("-mkdir")
                .generateRandomParameter(null, null);
        params.add(mkdirCmd);

        Parameter subFolder = new CONSTANTSTRINGType(state.subdir)
                .generateRandomParameter(null, null);
        params.add(subFolder);
    }

    @Override
    public void separate(State state) {
        subdir = ((HdfsState) state).subdir;
        params.remove(1);
        Parameter subFolder = new CONSTANTSTRINGType(((HdfsState) state).subdir)
                .generateRandomParameter(null, null);
        params.add(subFolder);
    }

    @Override
    public void updateState(State state) {
    }

}
