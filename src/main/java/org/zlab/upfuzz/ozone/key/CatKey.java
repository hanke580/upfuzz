package org.zlab.upfuzz.ozone.key;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.ozone.Sh;

public class CatKey extends Sh {

    public CatKey(OzoneState state) {
        super();

        Parameter keyCatCmd = new CONSTANTSTRINGType("cat")
                .generateRandomParameter(null, null);
        params.add(keyCatCmd);

        Parameter keyPathParameter = new OzoneKeyType()
                .generateRandomParameter(state, null);
        params.add(keyPathParameter);
    }

    @Override
    public void updateState(State state) {
    }

    @Override
    public String constructCommandString() {
        String p = (params.get(1).toString()).toString();
        return "sh key" + " " + params.get(0) + " " + p;
    }
}
