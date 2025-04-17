package org.zlab.upfuzz.ozone.sh.key;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.ozone.sh.Sh;
import org.zlab.upfuzz.utils.Utilities;

public class KeyLs extends Sh {

    public KeyLs(OzoneState state) {
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
        String volumeName = volumePrefix + params.get(0).toString();
        String bucketName = params.get(1).toString();
        return Utilities.concat("sh key ls", volumeName + "/" + bucketName);
    }
}
