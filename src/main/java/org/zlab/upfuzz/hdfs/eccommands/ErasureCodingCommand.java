package org.zlab.upfuzz.hdfs.eccommands;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;

public abstract class ErasureCodingCommand extends Command {

    @Override
    public String constructCommandString() {
        StringBuilder ret = new StringBuilder();
        ret.append("ec");
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
