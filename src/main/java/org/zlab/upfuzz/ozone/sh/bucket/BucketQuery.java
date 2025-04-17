package org.zlab.upfuzz.ozone.sh.bucket;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.ozone.sh.Sh;

public abstract class BucketQuery extends Sh {
    String command; // E.g. info, getacl, ls

    public BucketQuery(OzoneState state) {
        super(state.volumePrefix);

        // choose a volume
        Parameter volumeNameParam = chooseVolume(state, this);
        params.add(volumeNameParam);

        // choose a bucket
        Parameter bucketNameParam = chooseBucket(state, this);
        params.add(bucketNameParam);
    }

    @Override
    public void updateState(State state) {
    }

    @Override
    public String constructCommandString() {
        assert command != null;
        String volumeName = volumePrefix + params.get(0).toString();
        String bucketName = params.get(1).toString();
        return "sh bucket " + command + " " + volumeName + "/" + bucketName;
    }
}
