package org.zlab.upfuzz.ozone.sh.bucket;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.ozone.sh.Sh;

public class DeleteBucket extends Sh {
    public DeleteBucket(OzoneState state) {
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
        String volumeName = params.get(0).toString();
        String bucketName = params.get(1).toString();
        ((OzoneState) state).deleteBucket(volumeName, bucketName);
    }

    @Override
    public String constructCommandString() {
        String volumeName = volumePrefix + params.get(0).toString();
        String bucketName = params.get(1).toString();
        return "sh bucket delete " + " " + volumeName + "/" + bucketName;
    }
}
