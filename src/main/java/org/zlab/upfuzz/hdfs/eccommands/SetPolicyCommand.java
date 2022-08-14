package org.zlab.upfuzz.hdfs.eccommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.RandomHadoopPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.hdfs.dfscommands.SetSpaceQuotaCommand;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

import java.util.LinkedList;
import java.util.List;

public class SetPolicyCommand extends ErasureCodingCommand {

    public static List<String> policies = new LinkedList<>();

    static {
        policies.add("RS-3-2-1024k");
        policies.add("RS-6-3-1024k");
        policies.add("RS-LEGACY-6-3-1024k");
        policies.add("XOR-2-1-1024k");
    }

    public SetPolicyCommand(HdfsState hdfsState) {
        Parameter setPolicyCmd = new CONSTANTSTRINGType("-setPolicy")
                .generateRandomParameter(null, null);

        Parameter pathOpt = new CONSTANTSTRINGType("-path")
                .generateRandomParameter(null, null);

        Parameter path = new RandomHadoopPathType()
                .generateRandomParameter(hdfsState, null);

        Parameter policyOpt = new CONSTANTSTRINGType("-policy")
                .generateRandomParameter(null, null);

        Parameter policy = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> (((SetPolicyCommand) c).policies),
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
    public void updateState(State state) {

    }
}
