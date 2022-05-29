package org.zlab.upfuzz.hdfs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
// import org.zlab.upfuzz.ParameterType.ConcatenateType;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class HDFSCommands extends Command {
    List<Parameter> parameters = new ArrayList<>();
    // public static class setacl extends HDFSCommands {

    // public setacl() {
    // // -setfacl [-R] [{-b|-k} {-m|-x <acl_spec>} <path>]|[--set <acl_spec>
    // <path>]

    // // -setacl
    // Parameter setaclcmd = new
    // CONSTANTSTRINGType("-setacl").generateRandomParameter(null, null);

    // // [-R]
    // Parameter hyphenR = new ParameterType.OptionalType(new
    // CONSTANTSTRINGType("-R"), null)
    // .generateRandomParameter(null, null);

    // // -b|-k
    // Parameter BandK = new
    // ParameterType.InCollectionType(CONSTANTSTRINGType.instance,
    // Arrays.asList(new CONSTANTSTRINGType("-b").generateRandomParameter(null,
    // null), new CONSTANTSTRINGType("-k").generateRandomParameter(null, null)),
    // null)
    // .generateRandomParameter(null, null);

    // // -m|-x
    // Parameter MandX = new
    // ParameterType.InCollectionType(CONSTANTSTRINGType.instance,
    // Arrays.asList(new CONSTANTSTRINGType("-m").generateRandomParameter(null,
    // null), new CONSTANTSTRINGType("-x").generateRandomParameter(null, null)),
    // null)
    // .generateRandomParameter(null, null);

    // // acl_spec
    // Parameter aclSpec = new
    // CONSTANTSTRINGType("acl").generateRandomParameter(null, null);

    // // {-m | -x <acl_spec>}
    // Parameter mxaclSpec = new ParameterType.ConcatenateType(null,
    // Arrays.asList(MandX, aclSpec))
    // .generateRandomParameter(null, null);

    // // <path>
    // Parameter path1 = new
    // CONSTANTSTRINGType("path1").generateRandomParameter(null, null);

    // // [{-b|-k} {-m|-x <acl_spec>} <path>]
    // Parameter bkmxaclPath = new ParameterType.ConcatenateType(null,
    // Arrays.asList(BandK, mxaclSpec, path1))
    // .generateRandomParameter(null, null);

    // // --set <acl_spec> <path>
    // Parameter hyphenSet = new
    // CONSTANTSTRINGType("--set").generateRandomParameter(null, null);
    // Parameter aclSpec1 = new
    // CONSTANTSTRINGType("acl2").generateRandomParameter(null, null);
    // Parameter path2 = new
    // CONSTANTSTRINGType("path2").generateRandomParameter(null, null);
    // Parameter setACLPATH = new ParameterType.ConcatenateType(null,
    // Arrays.asList(hyphenSet, aclSpec1, path2))
    // .generateRandomParameter(null, null);

    // // [{-b|-k} {-m|-x <acl_spec>} <path>]|[--set <acl_spec> <path>]
    // Parameter fullACLBKMXPATH = new ParameterType.ConcatenateType<>(null,
    // Arrays.asList(bkmxaclPath, setACLPATH))
    // .generateRandomParameter(null, null);

    // parameters.add(setaclcmd);
    // parameters.add(hyphenR);
    // parameters.add(fullACLBKMXPATH);
    // }

    // }

    @Override
    public String constructCommandString() {
        String ret = "";
        for (Parameter p : parameters) {
            ret += p.toString() + " ";
        }
        return ret;
    }

    @Override
    public void updateState(State state) {
        // TODO Auto-generated method stub

    }
}
