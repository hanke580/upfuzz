package org.zlab.upfuzz.hdfs.dfsadmin;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSFilePathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.BOOLType;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class ListOpenFiles extends Dfsadmin {

    public ListOpenFiles(HdfsState state) {
        super(state.subdir);

        Parameter cmd = new CONSTANTSTRINGType("-listOpenFiles")
                .generateRandomParameter(null,
                        null);

        Parameter opt1 = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-blockingDecommission"), null)
                        .generateRandomParameter(null, null);

        Parameter opt2 = new BOOLType().generateRandomParameter(null, null);

        Parameter opt3 = new CONSTANTSTRINGType("-path")
                .generateRandomParameter(null, null);
        Parameter file = new HDFSFilePathType()
                .generateRandomParameter(state, null);

        params.add(cmd);
        params.add(opt1);
        params.add(opt2);
        params.add(opt3);
        params.add(file);
    }

    @Override
    public String constructCommandString() {
        StringBuilder sb = new StringBuilder();
        sb.append("dfsadmin").append(" ");
        sb.append(params.get(0)).append(" ");
        sb.append(params.get(1)).append(" ");
        if ((Boolean) params.get(2).getValue()) {
            sb.append(params.get(3)).append(" ");
            sb.append(subdir).append(params.get(4));
        }
        return sb.toString();
    }

    @Override
    public void updateState(State state) {
    }
}