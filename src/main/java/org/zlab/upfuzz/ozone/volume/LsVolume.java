package org.zlab.upfuzz.ozone.volume;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.ozone.Sh;

public class LsVolume extends Sh {

    public LsVolume(OzoneState state) {
        super(state.volume);

        Parameter lsVolumeCmd = new CONSTANTSTRINGType("ls")
                .generateRandomParameter(null, null);
        params.add(lsVolumeCmd);
    }

    @Override
    public void updateState(State state) {
        return;
    }

    @Override
    public String constructCommandString() {
        return "sh volume" + " " + params.get(0);
    }
}
