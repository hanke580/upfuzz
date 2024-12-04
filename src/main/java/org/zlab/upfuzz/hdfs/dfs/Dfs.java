package org.zlab.upfuzz.hdfs.dfs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.hdfs.HdfsCommand;

import java.util.Arrays;
import java.util.List;

public abstract class Dfs extends HdfsCommand {

    String type = "dfs";

    public static final List<String> opts = Arrays.asList("user.description",
            "user.flag");
    public static final List<String> values = Arrays.asList("this is a desc",
            "this is a flag");

    public Dfs(String subdir) {
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
