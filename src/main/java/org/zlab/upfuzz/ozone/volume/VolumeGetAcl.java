package org.zlab.upfuzz.ozone.volume;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.ozone.OzoneParameterType.OzoneVolumeType;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.ozone.Sh;

public class VolumeGetAcl extends Sh {

    public VolumeGetAcl(OzoneState state) {
        super(state.volume);

        Parameter volumeGetAclCmd = new CONSTANTSTRINGType("getacl")
                .generateRandomParameter(null, null);
        params.add(volumeGetAclCmd);

        Parameter volumeNameParameter = new OzoneVolumeType()
                .generateRandomParameter(state, null);
        params.add(volumeNameParameter);
    }

    @Override
    public void updateState(State state) {
        return;
    }

    @Override
    public String constructCommandString() {
        String p = (params.get(1).toString()).toString();
        return "sh volume" + " " + params.get(0) +
                " " + p.toLowerCase();
    }

}
