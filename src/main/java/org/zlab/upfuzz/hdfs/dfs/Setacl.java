package org.zlab.upfuzz.hdfs.dfs;

import java.util.Arrays;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.ParameterType.ConcreteType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.ConcatenateType;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSDirPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class Setacl extends Dfs {

    public Setacl(HdfsState state) {
        super(state.subdir);

        // -setfacl [-R] [{-b|-k} {-m|-x <acl_spec>} <path>]|[--set <acl_spec>
        // <path>]

        // -setacl
        Parameter setaclcmd = new CONSTANTSTRINGType("-setacl")
                .generateRandomParameter(state, this);

        // [-R]
        Parameter hyphenR = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-R"), null)
                        .generateRandomParameter(state, this);

        // -b|-k
        Parameter BandK = new ParameterType.InCollectionType(
                (ConcreteType) CONSTANTSTRINGType.instance,
                (s, c) -> Arrays.asList(
                        new CONSTANTSTRINGType("-b")
                                .generateRandomParameter(s, c),
                        new CONSTANTSTRINGType("-k")
                                .generateRandomParameter(s, c)),
                null)
                        .generateRandomParameter(state, this);

        // -m|-x
        Parameter MandX = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Arrays.asList(
                        new CONSTANTSTRINGType("-m")
                                .generateRandomParameter(s, c),
                        new CONSTANTSTRINGType("-x")
                                .generateRandomParameter(s, c)),
                null)
                        .generateRandomParameter(state, this);

        // acl_spec
        Parameter aclSpec = new CONSTANTSTRINGType("acl")
                .generateRandomParameter(state, this);

        // {-m | -x <acl_spec>}
        Parameter mxaclSpec = new ConcatenateType(MandX, aclSpec)
                .generateRandomParameter(state, this);

        // <path>
        Parameter path1 = new HDFSDirPathType()
                .generateRandomParameter(state, null);

        // [{-b|-k} {-m|-x <acl_spec>} <path>]
        Parameter bkmxaclPath = new ConcatenateType(BandK, mxaclSpec, path1)
                .generateRandomParameter(state, this);

        // --set <acl_spec> <path>
        Parameter hyphenSet = new CONSTANTSTRINGType("--set")
                .generateRandomParameter(state, this);
        Parameter aclSpec1 = new CONSTANTSTRINGType("acl2")
                .generateRandomParameter(state, this);
        Parameter path2 = new HDFSDirPathType()
                .generateRandomParameter(state, this);
        Parameter setACLPATH = new ConcatenateType(hyphenSet, aclSpec1, path2)
                .generateRandomParameter(state, this);

        // [{-b|-k} {-m|-x <acl_spec>} <path>]|[--set <acl_spec> <path>]
        Parameter fullACLBKMXPATH = new ConcatenateType(bkmxaclPath, setACLPATH)
                .generateRandomParameter(state, this);

        params.add(setaclcmd);
        params.add(hyphenR);
        params.add(fullACLBKMXPATH);
    }

    @Override
    public void updateState(State state) {
    }
}
