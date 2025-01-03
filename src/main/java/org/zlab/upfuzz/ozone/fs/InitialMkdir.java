package org.zlab.upfuzz.ozone.fs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class InitialMkdir extends Fs {

    /**
     * A special command, it cannot be mutated, it will always be a
     * bin/ozone fs -mkdir /UUID/
     */
    public InitialMkdir(OzoneState state) {
        super(state.subdir);

        Parameter mkdirCmd = new CONSTANTSTRINGType("-mkdir")
                .generateRandomParameter(null, null);
        params.add(mkdirCmd);

        Parameter subFolder = new CONSTANTSTRINGType(state.subdir)
                .generateRandomParameter(null, null);
        params.add(subFolder);
    }

    @Override
    public void separate(State state) {
        subdir = ((OzoneState) state).subdir;
        params.remove(1);
        Parameter subFolder = new CONSTANTSTRINGType(
                ((OzoneState) state).subdir)
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