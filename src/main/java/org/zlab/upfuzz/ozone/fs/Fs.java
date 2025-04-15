package org.zlab.upfuzz.ozone.fs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneCommand;
import org.zlab.upfuzz.ozone.OzoneState;

public abstract class Fs extends OzoneCommand {
    String subdir;

    public Fs(String subdir) {
        this.subdir = subdir;
    }

    @Override
    public void separate(State state) {
        subdir = ((OzoneState) state).subdir;
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

    public String constructCommandStringWithDirSeparation(String type) {
        StringBuilder sb = new StringBuilder();
        sb.append(type).append(" ");
        int i = 0;
        while (i < params.size() - 1) {
            if (!params.get(i).toString().isEmpty())
                sb.append(params.get(i)).append(" ");
            i++;
        }
        sb.append(subdir).append(params.get(i));
        return sb.toString();
    }
}
