package org.zlab.upfuzz.hdfs.ec;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class ListCodecs extends ErasureCoding {

    public ListCodecs(HdfsState state) {
        super(state.subdir);

        Parameter listCodecsCmd = new CONSTANTSTRINGType("-listCodecs")
                .generateRandomParameter(null, null);

        params.add(listCodecsCmd);
    }

    @Override
    public void updateState(State state) {

    }
}
