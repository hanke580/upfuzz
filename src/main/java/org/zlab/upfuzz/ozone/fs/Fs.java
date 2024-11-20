package org.zlab.upfuzz.ozone.fs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ozone.OzoneCommand;

public abstract class Fs extends OzoneCommand {

    public Fs(String subdir) {
        super(subdir);
    }

    @Override
    public String constructCommandString() {
        StringBuilder ret = new StringBuilder();
        ret.append("fs");
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
