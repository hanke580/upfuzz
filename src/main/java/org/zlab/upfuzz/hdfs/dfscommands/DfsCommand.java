package org.zlab.upfuzz.hdfs.dfscommands;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;

public abstract class DfsCommand extends Command {

    @Override
    public String constructCommandString() {
        StringBuilder ret = new StringBuilder();
        ret.append("dfs");
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
