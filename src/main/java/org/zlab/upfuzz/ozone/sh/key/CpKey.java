package org.zlab.upfuzz.ozone.sh.key;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.STRINGType;
import org.zlab.upfuzz.utils.Utilities;

public class CpKey extends KeyQuery {

    public CpKey(OzoneState state) {
        super(state);

        // 3: choose a volume
        Parameter volumeNameParam = chooseVolume(state, this);
        params.add(volumeNameParam);

        // 4: choose a bucket
        Parameter bucketNameParam = chooseBucket(state, this,
                volumeNameParam.toString());
        params.add(bucketNameParam);

        // 5: choose a keyName
        Parameter dstKeyNameParam = new STRINGType(10, 1)
                .generateRandomParameter(null, null);
        params.add(dstKeyNameParam);

        this.command = "cp";
    }

    @Override
    public void updateState(State state) {
        String volumeName = params.get(3).toString();
        String bucketName = params.get(4).toString();
        String keyName = params.get(5).toString();
        ((OzoneState) state).addKey(volumeName, bucketName, keyName);
    }

    @Override
    public String constructCommandString() {
        String srcVolumeName = params.get(0).toString();
        String srcBucketName = params.get(1).toString();
        String srcKeyName = params.get(2).toString();
        String srcPath = srcVolumeName + "/" + srcBucketName + "/" + srcKeyName;

        String dstVolumeName = params.get(3).toString();
        String dstBucketName = params.get(4).toString();
        String dstKeyName = params.get(5).toString();
        String dstPath = dstVolumeName + "/" + dstBucketName + "/" + dstKeyName;

        return Utilities.concat("sh key", command, srcPath, dstPath);
    }
}
