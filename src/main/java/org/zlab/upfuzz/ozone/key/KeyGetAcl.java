package org.zlab.upfuzz.ozone.key;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.ozone.Sh;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.ozone.OzoneParameterType.OzoneKeyType;

public class KeyGetAcl extends Sh {

    public KeyGetAcl(OzoneState state) {
        super(state.key);

        Parameter keyGetAclCmd = new CONSTANTSTRINGType("getacl")
                .generateRandomParameter(null, null);
        params.add(keyGetAclCmd);

        Parameter keyPathParameter = new OzoneKeyType()
                .generateRandomParameter(state, null);
        params.add(keyPathParameter);
    }

    @Override
    public void updateState(State state) {
        return;
    }

    @Override
    public String constructCommandString() {
        String p = (params.get(1).toString()).toString();
        return "sh key" + " " + params.get(0) + " " + p;
    }

}
