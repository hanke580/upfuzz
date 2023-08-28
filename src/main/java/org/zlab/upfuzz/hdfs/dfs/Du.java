package org.zlab.upfuzz.hdfs.dfs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSDirPathType;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSFilePathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class Du extends Dfs {
    public Du(HdfsState hdfsState) {
        super(hdfsState.subdir);

        Parameter cmd = new CONSTANTSTRINGType("-du")
                .generateRandomParameter(hdfsState, null);

        Parameter opt1 = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-s"), null)
                        .generateRandomParameter(hdfsState, null);
        Parameter opt2 = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-h"), null)
                        .generateRandomParameter(hdfsState, null);
        Parameter opt3 = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-v"), null)
                        .generateRandomParameter(hdfsState, null);
        Parameter opt4 = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-x"), null)
                        .generateRandomParameter(hdfsState, null);
        Parameter param = new HDFSDirPathType()
                .generateRandomParameter(hdfsState, null);
        params.add(cmd);
        params.add(opt1);
        params.add(opt2);
        if (Config.getConf().support_du_v_opt)
            params.add(opt3);
        params.add(opt4);
        params.add(param);
    }

    @Override
    public String constructCommandString() {
        return constructCommandStringWithDirSeparation("dfs");
    }

    @Override
    public void updateState(State state) {
    }
}
