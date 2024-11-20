package org.zlab.upfuzz.ozone.volume;

import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.ozone.Sh;

public abstract class Volume extends Sh {

    public Volume(OzoneState ozoneState) {
        super(ozoneState.volume);
    }

}
