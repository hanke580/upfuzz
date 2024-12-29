package org.zlab.upfuzz.ozone.fs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class InitialLs extends Fs {

    /**
     * THis is a special command, it cannot be mutated, it will always be a
     * bin/ozone fs -mkdir /UUID/
     */
    public InitialLs(OzoneState state) {
        super(state.subdir);

        Parameter mkdirCmd = new CONSTANTSTRINGType("-mkdir")
                .generateRandomParameter(null, null);
        params.add(mkdirCmd);

        Parameter subFolder = new CONSTANTSTRINGType("/")
                .generateRandomParameter(null, null);
        params.add(subFolder);
    }

    @Override
    public void updateState(State state) {
    }

    @Override
    public String constructCommandString() {
        String p = (params.get(1).toString()).toString();
        return "fs" + " " + params.get(0) +
                " " + p;
    }
}