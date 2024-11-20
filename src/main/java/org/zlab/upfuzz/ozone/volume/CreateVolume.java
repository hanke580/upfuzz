package org.zlab.upfuzz.ozone.volume;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.STRINGType;
import org.zlab.upfuzz.ozone.Sh;

public class CreateVolume extends Sh {

    public CreateVolume(OzoneState state) {
        super(state.volume);

        Parameter createVolumeCmd = new CONSTANTSTRINGType("create")
                .generateRandomParameter(null, null);
        params.add(createVolumeCmd);

        Parameter volumeNameParameter = new STRINGType(20, 3)
                .generateRandomParameter(null, null);
        params.add(volumeNameParameter);
    }

    @Override
    public void updateState(State state) {
        // Add a real obj node to state
        String p = (params.get(1).toString()).toString().toLowerCase();

        ((OzoneState) state).oos.createVolume(p);
    }

    @Override
    public String constructCommandString() {
        String p = (params.get(1).toString()).toString().toLowerCase();
        return "sh volume" + " " + params.get(0) +
                " " + p;
    }

    @Override
    public void separate(State state) {
        params.remove(1);
        Parameter volumeNameParameter = new STRINGType(20, 3)
                .generateRandomParameter(null, null);
        params.add(volumeNameParameter);
    }
}
