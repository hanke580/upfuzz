package org.zlab.upfuzz.hdfs.dfsadmin;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.hdfs.HdfsCommand;

public abstract class Dfsadmin extends HdfsCommand {

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

    public String constructCommandStringWithDirSeparation() {
        StringBuilder sb = new StringBuilder();
        sb.append("dfsadmin").append(" ");
        int i = 0;
        while (i < params.size() - 1) {
            if (!params.get(i).toString().isEmpty())
                sb.append(params.get(i)).append(" ");
            i++;
        }
        sb.append(subdir).append(params.get(i));
        return sb.toString();
    }

    @Override
    public String toString() {
        return constructCommandString();
    }
}
