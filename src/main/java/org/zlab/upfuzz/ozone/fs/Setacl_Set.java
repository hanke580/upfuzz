package org.zlab.upfuzz.ozone.fs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.ConcatenateType;
import org.zlab.upfuzz.ozone.OzoneParameterType.OzoneDirPathType;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

// -setfacl [--set <acl_spec> <path>]
public class Setacl_Set extends Fs {

    public Setacl_Set(OzoneState state) {
        super(state.subdir);
        // -setfacl [-R] [{-b|-k} {-m|-x <acl_spec>} <path>]
        // -setfacl [--set <acl_spec> <path>]

        // <acl_spec>: [user|group|other|mask]:[name]:[rwx]

        // -setacl
        Parameter setaclcmd = new CONSTANTSTRINGType("-setfacl")
                .generateRandomParameter(state, this);

        // --set <acl_spec> <path>
        Parameter hyphenSet = new CONSTANTSTRINGType("--set")
                .generateRandomParameter(state, this);
        Parameter aclSpec = new CONSTANTSTRINGType("user:alice:rwx")
                .generateRandomParameter(state, this);
        Parameter path2 = new OzoneDirPathType()
                .generateRandomParameter(state, this);
        Parameter setACLPATH = new ConcatenateType(hyphenSet, aclSpec, path2)
                .generateRandomParameter(state, this);

        params.add(setaclcmd);
        params.add(setACLPATH);
    }

    @Override
    public void updateState(State state) {
    }
}
