package org.zlab.upfuzz.ozone.volume;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.ozone.Sh;
import org.zlab.upfuzz.ozone.OzoneParameterType.OzoneVolumeType;

public class DeleteVolume extends Sh {

    public DeleteVolume(OzoneState state) {
        super(state.volume);

        Parameter volumeInfoCmd = new CONSTANTSTRINGType("delete")
                .generateRandomParameter(null, null);
        params.add(volumeInfoCmd);

        Parameter volumeNameParameter = new OzoneVolumeType()
                .generateRandomParameter(state, null);
        params.add(volumeNameParameter);
    }

    @Override
    public void updateState(State state) {
        // Remove a real obj node from state
        ((OzoneState) state).oos.removeVolume(params.get(1).toString());
        return;
    }

    @Override
    public String constructCommandString() {
        String p = (params.get(1).toString()).toString();
        return "sh volume" + " " + params.get(0) +
                " " + p.toLowerCase();
    }
}
