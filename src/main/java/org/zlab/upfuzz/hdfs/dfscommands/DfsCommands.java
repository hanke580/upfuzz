/* (C)2022 */
package org.zlab.upfuzz.hdfs.dfscommands;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
// import org.zlab.upfuzz.ParameterType.ConcatenateType;

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
    public String toString() {
        return constructCommandString();
    }
}
