package org.zlab.upfuzz.ozone.sh.key;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.ozone.sh.Sh;
import org.zlab.upfuzz.utils.Utilities;

public abstract class KeyQuery extends Sh {
    String command;

    public KeyQuery(OzoneState state) {
        super(state.volumePrefix);

        // choose a volume
        Parameter volumeNameParam = chooseVolume(state, this);
        params.add(volumeNameParam);

        // choose a bucket
        Parameter bucketNameParam = chooseBucket(state, this);
        params.add(bucketNameParam);

        // choose a key
        Parameter keyNameParam = chooseKey(state, this,
                volumeNameParam.toString(), bucketNameParam.toString());
        params.add(keyNameParam);
    }

    @Override
    public void updateState(State state) {
    }

    @Override
    public String constructCommandString() {
        assert command != null;
        String volumeName = volumePrefix + params.get(0).toString();
        String bucketName = params.get(1).toString();
        String keyName = params.get(2).toString();
        String path = volumeName + "/" + bucketName + "/" + keyName;
        return Utilities.concat("sh key", command, path);
    }
}
