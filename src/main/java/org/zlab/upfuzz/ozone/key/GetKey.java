package org.zlab.upfuzz.ozone.key;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.ozone.Sh;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.ozone.OzoneParameterType.OzoneKeyType;
import org.zlab.upfuzz.ozone.OzoneParameterType.RandomLocalPathType;

public class GetKey extends Sh {

    public GetKey(OzoneState state) {
        super(state.key);

        Parameter keyGetCmd = new CONSTANTSTRINGType("get")
                .generateRandomParameter(null, null);
        params.add(keyGetCmd);

        Parameter keyPathParameter = new OzoneKeyType()
                .generateRandomParameter(state, null);
        params.add(keyPathParameter);

        Parameter dstParameter = new RandomLocalPathType()
                .generateRandomParameter(state, null);
    }

    @Override
    public void updateState(State state) {
        return;
    }

    @Override
    public String constructCommandString() {
        String p1 = (params.get(1).toString()).toString();
        String p2 = (params.get(2).toString()).toString();
        return "sh key" + " " + params.get(0) + " " + p1 + " " + p2;
    }

}
