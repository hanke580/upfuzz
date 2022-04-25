package org.zlab.upfuzz.hdfs.dfscommands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.ParameterType.ConcreteType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.ConcatenateType;
// import org.zlab.upfuzz.ParameterType.ConcatenateType;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public abstract class DfsCommands extends Command {

    @Override
    public String constructCommandString() {
        StringBuilder ret = new StringBuilder();
        boolean first = true;
        for (Parameter p : params) {
            String ps = p.toString();
            if (!ps.isEmpty() && !first) {
                ret.append(" ");
            }
            ret.append(ps);
            if (first) {
                first = false;
            }
        }
        return ret.toString();
    }

    @Override
    public String toString()
    {
        return constructCommandString();
    }
}
