package org.zlab.upfuzz.ozone.key;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.ozone.Sh;
import org.zlab.upfuzz.ozone.OzoneParameterType.*;

public class KeyLs extends Sh {
    public KeyLs(OzoneState state) {
        super(state.key);

        Parameter keyLsCmd = new CONSTANTSTRINGType("ls")
                .generateRandomParameter(null, null);
        params.add(keyLsCmd);

        Parameter bucketPathParameter = new OzoneBucketType()
                .generateRandomParameter(state, null);
        params.add(bucketPathParameter);
    }

    @Override
    public void updateState(State state) {
        return;
    }

    @Override
    public String constructCommandString() {
        String p = (params.get(1).toString()).toString().toLowerCase();
        return "sh key" + " " + params.get(0) + " " + p;
    }
}
