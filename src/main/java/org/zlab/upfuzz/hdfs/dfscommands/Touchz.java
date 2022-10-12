package org.zlab.upfuzz.hdfs.dfscommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSDirPathType;
import org.zlab.upfuzz.hdfs.HDFSParameterType.RandomHadoopPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.STRINGType;
import org.zlab.upfuzz.utils.Utilities;

public class Touchz extends DfsCommand {
    public Touchz(HdfsState hdfsState) {
        Parameter mkdirCmd = new CONSTANTSTRINGType("-touchz")
                .generateRandomParameter(null, null);
        params.add(mkdirCmd);

        Parameter pathParameter = new HDFSDirPathType()
                .generateRandomParameter(hdfsState, this);
        params.add(pathParameter);

        Parameter fileNameParameter = new STRINGType(20)
                .generateRandomParameter(hdfsState, null);
        params.add(fileNameParameter);

        Parameter fileTypeParameter = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities.strings2Parameters(
                        ((HdfsState) s).dfs.fileType),
                null).generateRandomParameter(hdfsState, this);

        params.add(fileTypeParameter);

        constructCommandString();
    }

    @Override
    public void updateState(State state) {
        // Add a real inode to state
        ((HdfsState) state).dfs.createFile(resolvePath());
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
            ret.append(params.get(1)).append(params.get(2))
                    .append(params.get(3));
        } else {
            ret.append(params.get(1)).append("/").append(params.get(2))
                    .append(params.get(3));
        }
        return ret.toString();
    }

}
