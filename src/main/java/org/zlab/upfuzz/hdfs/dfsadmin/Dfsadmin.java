package org.zlab.upfuzz.hdfs.dfsadmin;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.hdfs.HdfsCommand;

public abstract class Dfsadmin extends HdfsCommand {

    String type = "dfsadmin";

    public Dfsadmin(String subdir) {
        super(subdir);
    }

    @Override
    public String constructCommandString() {
        StringBuilder ret = new StringBuilder();
        ret.append("dfsadmin");
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
