package org.zlab.upfuzz.hdfs.dfs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.ConcatenateType;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSRandomPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.Utilities;

public class Setfattr1 extends Dfs {
    // hdfs dfs -setfattr {-n name [-v value] | -x name} <path>
    // hdfs dfs -setfattr -n name [-v value] <path>

    public Setfattr1(HdfsState state) {
        super(state.subdir);

        // -setacl
        Parameter cmd = new CONSTANTSTRINGType("-setfattr")
                .generateRandomParameter(state, this);

        // -n
        Parameter nParameter = new CONSTANTSTRINGType("-n")
                .generateRandomParameter(state, this);

        // name
        Parameter nameParameter = Utilities.createInStringCollectionType(opts)
                .generateRandomParameter(state, this);

        Parameter vParameter = new CONSTANTSTRINGType("-v")
                .generateRandomParameter(state, this);
        Parameter valueParameter = Utilities
                .createInStringCollectionType(values)
                .generateRandomParameter(state, this);

        Parameter v_value = new ParameterType.OptionalType(
                new ConcatenateType(vParameter, valueParameter), null)
                        .generateRandomParameter(state, this);

        // <path>
        Parameter path = new HDFSRandomPathType()
                .generateRandomParameter(state, null);

        params.add(cmd);
        params.add(nParameter);
        params.add(nameParameter);
        params.add(v_value);
        params.add(path);
    }

    @Override
    public void updateState(State state) {
    }

    @Override
    public String constructCommandString() {
        return constructCommandStringWithDirSeparation(type);
    }
}
