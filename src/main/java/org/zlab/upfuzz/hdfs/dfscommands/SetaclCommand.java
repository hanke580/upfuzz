package org.zlab.upfuzz.hdfs.dfscommands;

import java.util.Arrays;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.ParameterType.ConcreteType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.ConcatenateType;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class SetaclCommand extends DfsCommands {

    public SetaclCommand() {
        // -setfacl [-R] [{-b|-k} {-m|-x <acl_spec>} <path>]|[--set <acl_spec> <path>]

        // -setacl
        Parameter setaclcmd = new CONSTANTSTRINGType("-setacl").generateRandomParameter(null, null);

        // [-R]
        Parameter hyphenR = new ParameterType.OptionalType(new CONSTANTSTRINGType("-R"), null)
                .generateRandomParameter(null, null);

        // -b|-k
        Parameter BandK = new ParameterType.InCollectionType((ConcreteType) CONSTANTSTRINGType.instance,
                (s, c) -> Arrays.asList(new CONSTANTSTRINGType("-b").generateRandomParameter(null, null),
                        new CONSTANTSTRINGType("-k").generateRandomParameter(null, null)),
                null).generateRandomParameter(null, null);

        // -m|-x
        Parameter MandX = new ParameterType.InCollectionType(CONSTANTSTRINGType.instance,
                (s, c) -> Arrays.asList(new CONSTANTSTRINGType("-m").generateRandomParameter(null, null),
                        new CONSTANTSTRINGType("-x").generateRandomParameter(null, null)),
                null).generateRandomParameter(null, null);

        // acl_spec
        Parameter aclSpec = new CONSTANTSTRINGType("acl").generateRandomParameter(null, null);

        // {-m | -x <acl_spec>}
        Parameter mxaclSpec = new ConcatenateType(MandX, aclSpec).generateRandomParameter(null, null);

        // <path>
        Parameter path1 = new CONSTANTSTRINGType("path1").generateRandomParameter(null, null);

        // [{-b|-k} {-m|-x <acl_spec>} <path>]
        Parameter bkmxaclPath = new ConcatenateType(BandK, mxaclSpec, path1).generateRandomParameter(null, null);

        // --set <acl_spec> <path>
        Parameter hyphenSet = new CONSTANTSTRINGType("--set").generateRandomParameter(null, null);
        Parameter aclSpec1 = new CONSTANTSTRINGType("acl2").generateRandomParameter(null, null);
        Parameter path2 = new CONSTANTSTRINGType("path2").generateRandomParameter(null, null);
        Parameter setACLPATH = new ConcatenateType(hyphenSet, aclSpec1, path2).generateRandomParameter(null, null);

        // [{-b|-k} {-m|-x <acl_spec>} <path>]|[--set <acl_spec> <path>]
        Parameter fullACLBKMXPATH = new ConcatenateType(bkmxaclPath, setACLPATH).generateRandomParameter(null, null);

        params.add(setaclcmd);
        params.add(hyphenR);
        params.add(fullACLBKMXPATH);
    }

    @Override
    public void updateState(State state) {
    }
}
