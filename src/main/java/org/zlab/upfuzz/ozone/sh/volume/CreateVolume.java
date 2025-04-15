package org.zlab.upfuzz.ozone.sh.volume;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.ozone.sh.Sh;
import org.zlab.upfuzz.utils.STRINGType;

public class CreateVolume extends Sh {
    public CreateVolume(OzoneState state) {
        super(state.volumePrefix);

        ParameterType.ConcreteType volumeNameType = new ParameterType.LessLikelyMutateType(
                new ParameterType.NotInCollectionType(
                        new ParameterType.NotEmpty(new STRINGType(10, 1, true)),
                        (s, c) -> ((OzoneState) s).getVolumes(), null),
                0.1);
        Parameter volumeName = volumeNameType
                .generateRandomParameter(state, this);
        this.params.add(volumeName);
    }

    @Override
    public void updateState(State state) {
        String p = params.get(0).toString();
        ((OzoneState) state).addVolume(p);
    }

    @Override
    public String constructCommandString() {
        String volumeName = volumePrefix + params.get(0).toString();
        return "sh volume create " + volumeName;
    }
}
