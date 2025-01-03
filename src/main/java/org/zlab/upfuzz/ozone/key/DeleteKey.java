package org.zlab.upfuzz.ozone.key;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.ozone.Sh;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.ozone.OzoneParameterType.OzoneKeyType;

public class DeleteKey extends Sh {

    public DeleteKey(OzoneState state) {
        super(state.key);

        Parameter deleteKeyCmd = new CONSTANTSTRINGType("delete")
                .generateRandomParameter(null, null);
        params.add(deleteKeyCmd);

        Parameter keyPathParameter = new OzoneKeyType()
                .generateRandomParameter(state, null);
        params.add(keyPathParameter);
    }

    @Override
    public void updateState(State state) {
        // ((OzoneState) state).oos.removeKey(params.get(1).toString());
    }

    @Override
    public String constructCommandString() {
        String p = (params.get(1).toString()).toString();
        return "sh key" + " " + params.get(0) + " " + p;
    }

}
