package org.zlab.upfuzz.hdfs.dfscommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSDirPathType;
import org.zlab.upfuzz.hdfs.HDFSParameterType.RandomHadoopPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.STRINGType;

public class Mkdir extends DfsCommand {

    public Mkdir(HdfsState state) {
        super(state.subdir);

        Parameter mkdirCmd = new CONSTANTSTRINGType("-mkdir")
                .generateRandomParameter(null, null);
        params.add(mkdirCmd);

        Parameter parentPathParameter = new HDFSDirPathType()
                .generateRandomParameter(state, null);
        params.add(parentPathParameter);

        Parameter dirNameParameter = new STRINGType(20)
                .generateRandomParameter(state, null);
        params.add(dirNameParameter);
    }

    @Override
    public void updateState(State state) {
        // Add a real inode to state
        ((HdfsState) state).dfs.createDir(resolvePath());
    }

    @Override
    public String constructCommandString() {
        return "dfs" + " " + params.get(0) +
                " " + subdir + resolvePath();
    }

    private String resolvePath() {
        StringBuilder ret = new StringBuilder();
        if (params.get(1).toString().equals("/")) {
            ret.append(params.get(1)).append(params.get(2));
        } else {
            ret.append(params.get(1)).append("/").append(params.get(2));
        }
        return ret.toString();
    }

}
