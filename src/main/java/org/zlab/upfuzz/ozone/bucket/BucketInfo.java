package org.zlab.upfuzz.ozone.bucket;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.ozone.Sh;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.ozone.OzoneParameterType.OzoneBucketType;

public class BucketInfo extends Sh {

    public BucketInfo(OzoneState state) {
        super(state.bucket);

        Parameter bucketInfoCmd = new CONSTANTSTRINGType("info")
                .generateRandomParameter(null, null);
        params.add(bucketInfoCmd);

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
