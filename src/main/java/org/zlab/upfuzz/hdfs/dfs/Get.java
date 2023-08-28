package org.zlab.upfuzz.hdfs.dfs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSParameterType.HDFSFilePathType;
import org.zlab.upfuzz.hdfs.HDFSParameterType.RandomLocalPathType;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class Get extends Dfs {

    /*
     * Copy single src, or multiple srcs from local file system to the
     * destination file system. Also reads input from stdin and writes to
     * destination file system if the source is set to “-” Copying fails if the
     * file already exists, unless the -f flag is given.
     */
    public Get(HdfsState hdfsState) {
        super(hdfsState.subdir);

        Parameter getCmd = new CONSTANTSTRINGType("-get")
                .generateRandomParameter(null, null);

        // -f : Overwrites the destination if it already exists.
        Parameter fOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-f"), null)
                        .generateRandomParameter(null, null);

        // -p : Preserves access and modification times, ownership and the
        // permissions. (assuming the permissions can be propagated across
        // filesystems)
        Parameter pOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-p"), null)
                        .generateRandomParameter(null, null);

        // The -ignoreCrc option disables checkshum verification.
        Parameter igCrcOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-ignoreCrc"), null)
                        .generateRandomParameter(null, null);

        Parameter crcOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-crc"), null)
                        .generateRandomParameter(null, null);

        Parameter srcParameter = new HDFSFilePathType()
                .generateRandomParameter(hdfsState, null);

        Parameter dstParameter = new RandomLocalPathType()
                .generateRandomParameter(hdfsState, null);

        params.add(getCmd);
        params.add(fOption);
        params.add(pOption);
        params.add(igCrcOption);
        params.add(crcOption);
        params.add(srcParameter);
        params.add(dstParameter);
    }

    @Override
    public String constructCommandString() {
        return "dfs" + " " +
                params.get(0) + " " +
                params.get(1) + " " +
                params.get(2) + " " +
                params.get(3) + " " +
                params.get(4) + " " +
                subdir +
                params.get(5) + " " +
                params.get(6);
    }

    @Override
    public void updateState(State state) {
        HdfsState hdfsState = (HdfsState) state;
        Parameter srcParameter = params.get(5), dstParameter = params.get(6);
        String srcPath = srcParameter.toString(),
                dstPath = dstParameter.toString();
        hdfsState.lfs.createFile(dstPath, true);
    }
}
