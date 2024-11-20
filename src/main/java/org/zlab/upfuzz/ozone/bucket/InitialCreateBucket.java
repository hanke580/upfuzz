package org.zlab.upfuzz.ozone.bucket;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.STRINGType;
import org.zlab.upfuzz.ozone.Sh;
import org.zlab.upfuzz.ozone.OzoneParameterType.OzoneVolumeType;
import org.zlab.upfuzz.ozone.OzoneParameterType.OzoneBucketType;

public class InitialCreateBucket extends Sh {

    public InitialCreateBucket(OzoneState state) {
        super(state.subdir, state.bucket, state.volume);

        Parameter createBucketCmd = new CONSTANTSTRINGType("create")
                .generateRandomParameter(null, null);
        params.add(createBucketCmd);

        Parameter volumeOnly = new CONSTANTSTRINGType(
                ((OzoneState) state).volume).generateRandomParameter(null,
                        null);
        Parameter bucketOnly = new CONSTANTSTRINGType(
                ((OzoneState) state).bucket).generateRandomParameter(null,
                        null);
        Parameter volumeBucketPath = new CONSTANTSTRINGType(
                (volumeOnly.toString()).toString().toLowerCase() + "/"
                        + (bucketOnly.toString()).toString().toLowerCase())
                                .generateRandomParameter(null, null);
        params.add(volumeBucketPath);
    }

    @Override
    public void updateState(State state) {
        String p = (params.get(1).toString()).toString().toLowerCase();
        ((OzoneState) state).oos.createVolume(p);
        return;
    }

    @Override
    public void separate(State state) {
        subdir = ((OzoneState) state).subdir;
        bucket = ((OzoneState) state).bucket;
        volume = ((OzoneState) state).volume;

        params.remove(1);

        Parameter volumeOnly = new CONSTANTSTRINGType(
                ((OzoneState) state).volume).generateRandomParameter(null,
                        null);
        Parameter bucketOnly = new CONSTANTSTRINGType(
                ((OzoneState) state).bucket).generateRandomParameter(null,
                        null);
        Parameter volumeBucketPath = new CONSTANTSTRINGType(
                (volumeOnly.toString()).toString().toLowerCase() + "/"
                        + (bucketOnly.toString()).toString().toLowerCase())
                                .generateRandomParameter(null, null);

        params.add(volumeBucketPath);
    }

    @Override
    public String constructCommandString() {
        String p = (params.get(1).toString()).toString().toLowerCase();
        return "sh bucket" + " " + params.get(0) +
                " " + p;
    }
}
