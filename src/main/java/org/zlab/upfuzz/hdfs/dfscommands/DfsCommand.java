package org.zlab.upfuzz.hdfs.dfscommands;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HdfsCommand;
import org.zlab.upfuzz.hdfs.HdfsState;

public abstract class DfsCommand extends HdfsCommand {

    public DfsCommand(String subdir) {
        super(subdir);
    }

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
