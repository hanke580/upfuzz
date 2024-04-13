package org.zlab.upfuzz.hdfs.dfsadmin;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.STRINGType;

/**
 * Store metadata in a file under log dir in local FS system
 */
public class Metasave extends Dfsadmin {

    public Metasave(HdfsState state) {
        super(state.subdir);

        Parameter cmd = new CONSTANTSTRINGType("-metasave")
                .generateRandomParameter(null,
                        null);
        // filename: should be a random string...
        Parameter file = new ParameterType.NotEmpty(
                new STRINGType(20)).generateRandomParameter(state, null);

        params.add(cmd);
        params.add(file);
    }

    @Override
    public String constructCommandStringWithDirSeparation(String type) {
        StringBuilder sb = new StringBuilder();
        sb.append(type).append(" ");
        int i = 0;
        while (i < params.size() - 1) {
            if (!params.get(i).toString().isEmpty())
                sb.append(params.get(i)).append(" ");
            i++;
        }
        // remove "/" from subdir: use string replace
        String subdirWithoutSlash = this.subdir.replace("/", "");
        sb.append(subdirWithoutSlash).append("_" + params.get(i));
        return sb.toString();
    }

    @Override
    public String constructCommandString() {
        return constructCommandStringWithDirSeparation("dfsadmin");
    }

    @Override
    public void updateState(State state) {
    }
}