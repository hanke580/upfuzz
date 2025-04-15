package org.zlab.upfuzz.ozone.sh.bucket;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.STRINGType;
import org.zlab.upfuzz.ozone.sh.Sh;

public class CreateBucket extends Sh {
    public CreateBucket(OzoneState state) {
        super(state.volumePrefix);

        Parameter volumeNameParam = chooseVolume(state, this);
        params.add(volumeNameParam);

        ParameterType.ConcreteType bucketNameType = new ParameterType.NotInCollectionType(
                new ParameterType.NotEmpty(new STRINGType(10, 1, true)),
                (s, c) -> ((OzoneState) s)
                        .getBuckets(volumeNameParam.toString()),
                null);
        Parameter bucketNameParam = bucketNameType
                .generateRandomParameter(state, this);
        this.params.add(bucketNameParam);
    }

    @Override
    public void updateState(State state) {
        String volumeName = params.get(0).toString();
        String bucketName = params.get(1).toString();
        ((OzoneState) state).addBucket(volumeName, bucketName);
    }

    @Override
    public String constructCommandString() {
        String volumeName = volumePrefix + params.get(0).toString();
        String bucketName = params.get(1).toString();
        return "sh bucket create" + " " + (volumeName + "/" + bucketName);
    }
}
