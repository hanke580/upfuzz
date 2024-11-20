package org.zlab.upfuzz.ozone.key;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.ozone.Sh;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.ozone.OzoneParameterType.OzoneKeyType;

public class KeyInfo extends Sh {

    public KeyInfo(OzoneState state) {
        super(state.key);

        Parameter keyInfoCmd = new CONSTANTSTRINGType("info")
                .generateRandomParameter(null, null);
        params.add(keyInfoCmd);

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
