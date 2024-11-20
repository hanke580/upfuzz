package org.zlab.upfuzz.ozone.fs;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;

public class Chgrp extends Fs {
    /**
     * TODO
     */
    public Chgrp(OzoneState state) {
        super(state.subdir);
    }

    @Override
    public void updateState(State state) {
    }
}