package org.zlab.upfuzz.ozone.sh.key;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.ozone.sh.Sh;
import org.zlab.upfuzz.ozone.OzoneParameterType.RandomLocalPathType;
import org.zlab.upfuzz.utils.STRINGType;

public class PutKey extends Sh {
    public PutKey(OzoneState state) {
        super(state.volumePrefix);

        Parameter volumeNameParam = chooseVolume(state, this);
        params.add(volumeNameParam);

        Parameter bucketNameParam = chooseBucket(state, this);
        params.add(bucketNameParam);

        Parameter keyNameParam = new STRINGType(10, 1)
                .generateRandomParameter(null, null);
        params.add(keyNameParam);

        Parameter srcParameter = new RandomLocalPathType()
                .generateRandomFileParameter(state, null);
        params.add(srcParameter);
    }

    @Override
    public void updateState(State state) {
        String volumeName = params.get(0).toString();
        String bucketName = params.get(1).toString();
        String keyName = params.get(2).toString();
        ((OzoneState) state).addKey(volumeName, bucketName, keyName);
    }

    @Override
    public String constructCommandString() {
        String volumeName = volumePrefix + params.get(0).toString();
        String bucketName = params.get(1).toString();
        String keyName = params.get(2).toString();
        String src = params.get(3).toString();
        return "sh key put" + " " + volumeName + "/" + bucketName + "/"
                + keyName + " " + src;
    }
}
