package org.zlab.upfuzz.ozone.sh;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneCommand;
import org.zlab.upfuzz.ozone.OzoneState;

public abstract class Sh extends OzoneCommand {
    protected String volumePrefix;

    public Sh(String volumePrefix) {
        this.volumePrefix = volumePrefix;
    }

    @Override
    public void separate(State state) {
        this.volumePrefix = ((OzoneState) state).volumePrefix;
    }

    @Override
    public String constructCommandString() {
        StringBuilder ret = new StringBuilder();
        ret.append("sh");
        for (Parameter p : params) {
            String ps = p.toString();
            ret.append(" ");
            ret.append(ps);
        }
        return ret.toString();
    }

    @Override
    public String toString() {
        return constructCommandString();
    }
}
