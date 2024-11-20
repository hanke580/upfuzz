package org.zlab.upfuzz.ozone.bucket;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.STRINGType;
import org.zlab.upfuzz.ozone.Sh;
import org.zlab.upfuzz.ozone.OzoneParameterType.OzoneVolumeType;

public class CreateBucket extends Sh {

    public CreateBucket(OzoneState state) {
        super(state.bucket);

        Parameter createBucketCmd = new CONSTANTSTRINGType("create")
                .generateRandomParameter(null, null);
        params.add(createBucketCmd);

        Parameter volumeNameParameter = new OzoneVolumeType()
                .generateRandomParameter(state, null);
        params.add(volumeNameParameter);

        Parameter bucketNameParameter = new STRINGType(20, 3)
                .generateRandomParameter(null, null);
        params.add(bucketNameParameter);
    }

    @Override
    public void updateState(State state) {
        // Add a real objnode to state
        String p = (params.get(1).toString()).toString().toLowerCase() + "/"
                + (params.get(2).toString()).toString().toLowerCase();
        ((OzoneState) state).oos.createBucket(p);
    }

    @Override
    public String constructCommandString() {
        String p1 = (params.get(1).toString()).toString().toLowerCase();
        String p2 = (params.get(2).toString()).toString().toLowerCase();
        return "sh bucket" + " " + params.get(0) +
                " " + (p1 + "/" + p2);
    }

    @Override
    public void separate(State state) {
    }
}
