package org.zlab.upfuzz.ozone.bucket;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.ozone.Sh;
import org.zlab.upfuzz.ozone.OzoneParameterType.*;

public class BucketLs extends Sh {

    public BucketLs(OzoneState state) {
        super(state.bucket);

        Parameter bucketLsCmd = new CONSTANTSTRINGType("ls")
                .generateRandomParameter(null, null);
        params.add(bucketLsCmd);

        Parameter volumeNameParameter = new OzoneVolumeType()
                .generateRandomParameter(state, null);
        params.add(volumeNameParameter);
    }

    @Override
    public void updateState(State state) {
        return;
    }

    @Override
    public String constructCommandString() {
        String p = (params.get(1).toString()).toString().toLowerCase();
        return "sh bucket" + " " + params.get(0) + " " + p;
    }
}
