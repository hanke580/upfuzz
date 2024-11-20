package org.zlab.upfuzz.ozone.bucket;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.STRINGType;
import org.zlab.upfuzz.ozone.Sh;
import org.zlab.upfuzz.ozone.OzoneParameterType.OzoneBucketType;

public class BucketGetAcl extends Sh {

    public BucketGetAcl(OzoneState state) {
        super(state.bucket);

        Parameter bucketLsCmd = new CONSTANTSTRINGType("getacl")
                .generateRandomParameter(null, null);
        params.add(bucketLsCmd);

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
        return "sh bucket" + " " + params.get(0) + " " + p;
    }

}
