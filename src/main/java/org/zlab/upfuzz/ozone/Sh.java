package org.zlab.upfuzz.ozone;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;

public abstract class Sh extends OzoneCommand {

    public Sh() {
    }

    @Override
    public void separate(State state) {
        // TODO: implement separation for SH
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
