package org.zlab.upfuzz.hdfs.ec;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSDirPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.Utilities;

public class SetPolicy extends ErasureCoding {

    public SetPolicy(HdfsState state) {
        super(state.subdir);

        Parameter setPolicyCmd = new CONSTANTSTRINGType("-setPolicy")
                .generateRandomParameter(null, null);

        Parameter pathOpt = new CONSTANTSTRINGType("-path")
                .generateRandomParameter(null, null);

        Parameter path = new HDFSDirPathType()
                .generateRandomParameter(state, null);

        Parameter policyOpt = new CONSTANTSTRINGType("-policy")
                .generateRandomParameter(null, null);

        Parameter policy = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities.strings2Parameters(
                        (((ErasureCoding) c).policies)),
                null).generateRandomParameter(null, null);

        Parameter replicateOpt = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-replicate"), null)
                        .generateRandomParameter(null, null);

        params.add(setPolicyCmd);
        params.add(pathOpt);
        params.add(path);
        params.add(policyOpt);
        params.add(policy);
        params.add(replicateOpt);
    }

    @Override
    public String constructCommandString() {
        return "ec" + " " +
                params.get(0) + " " +
                params.get(1) + " " +
                subdir +
                params.get(2) + " " +
                params.get(3) + " " +
                params.get(4) + " " +
                params.get(5);
    }

    @Override
    public void updateState(State state) {
    }
}
