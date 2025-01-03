package org.zlab.upfuzz.ozone.sh.volume;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.ozone.Sh;
import org.zlab.upfuzz.utils.UUIDType;

public class CreateVolume extends Sh {

    public CreateVolume(OzoneState state) {
        ParameterType.ConcreteType volumeNameType = new ParameterType.LessLikelyMutateType(
                new ParameterType.NotInCollectionType(
                        new ParameterType.NotEmpty(UUIDType.instance),
                        (s, c) -> ((OzoneState) s).getVolumes(), null),
                0.1);
        Parameter volumeName = volumeNameType
                .generateRandomParameter(state, this);
        this.params.add(volumeName);
    }

    @Override
    public void updateState(State state) {
        String p = (params.get(0).toString()).toString();
        ((OzoneState) state).addVolume(p);
    }

    @Override
    public String constructCommandString() {
        String p = (params.get(0).toString()).toString();
        return "sh volume create " + p;
    }

    @Override
    public void separate(State state) {
        this.params.get(0).regenerate(null, this);
    }
}
