package org.zlab.upfuzz.ozone;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ozone.OzoneCommand;

public abstract class Sh extends OzoneCommand {

    public Sh(String subdir) {
        super(subdir);
    }

    public Sh(String subdir, String bucket, String volume) {
        super(subdir, bucket, volume);
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
