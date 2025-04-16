package org.zlab.upfuzz.ozone.fs;

import java.util.Arrays;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.ParameterType.ConcreteType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.ConcatenateType;
import org.zlab.upfuzz.ozone.OzoneParameterType.OzoneDirPathType;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

// -setfacl [-R] [{-b|-k} {-m|-x <acl_spec>} <path>]
public class Setacl_RM extends Fs {

    public Setacl_RM(OzoneState state) {
        super(state.subdir);

        // -setfacl [-R] [{-b|-k} {-m|-x <acl_spec>} <path>]|[--set <acl_spec>
        // <path>]

        // -setfacl [--set <acl_spec> <path>]

        // -setacl
        Parameter setaclcmd = new CONSTANTSTRINGType("-setfacl")
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
        Parameter aclSpec = new CONSTANTSTRINGType("user:alice:rwx")
                .generateRandomParameter(state, this);

        // {-m | -x <acl_spec>}
        Parameter mxaclSpec = new ConcatenateType(MandX, aclSpec)
                .generateRandomParameter(state, this);

        // <path>
        Parameter path = new OzoneDirPathType()
                .generateRandomParameter(state, null);

        // [{-b|-k} {-m|-x <acl_spec>} <path>]
        Parameter bkmxaclPath = new ConcatenateType(BandK, mxaclSpec, path)
                .generateRandomParameter(state, this);

        params.add(setaclcmd);
        params.add(hyphenR);
        params.add(bkmxaclPath);
    }

    @Override
    public void updateState(State state) {
    }
}
