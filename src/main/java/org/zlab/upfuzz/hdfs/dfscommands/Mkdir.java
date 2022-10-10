package org.zlab.upfuzz.hdfs.dfscommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSDirPathType;
import org.zlab.upfuzz.hdfs.HDFSParameterType.RandomHadoopPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.STRINGType;

public class Mkdir extends DfsCommand {

    public Mkdir(HdfsState hdfsState) {
        Parameter mkdirCmd = new CONSTANTSTRINGType("-mkdir")
                .generateRandomParameter(null, null);
        params.add(mkdirCmd);

        Parameter parentPathParameter = new HDFSDirPathType()
                .generateRandomParameter(hdfsState, null);
        params.add(parentPathParameter);

        Parameter dirNameParameter = new STRINGType(20)
                .generateRandomParameter(hdfsState, null);
        params.add(dirNameParameter);

        constructCommandString();
    }

    @Override
    public void updateState(State state) {
        // Add a real inode to state
        ((HdfsState) state).dfs.createDir(resolvePath());
    }

    @Override
    public String constructCommandString() {
        StringBuilder ret = new StringBuilder();
        ret.append("dfs");
        ret.append(" ").append(params.get(0));
        ret.append(" ").append(resolvePath());
        return ret.toString();
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
