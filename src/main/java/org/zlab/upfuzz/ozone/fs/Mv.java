package org.zlab.upfuzz.ozone.fs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneParameterType.*;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class Mv extends Fs {

    /*
     * Moves files from source to destination. This command allows multiple
     * sources as well in which case the destination needs to be a directory.
     * Moving files across file systems is not permitted.
     */
    public Mv(OzoneState state) {
        super(state.subdir);

        Parameter mvcmd = new CONSTANTSTRINGType("-mv")
                .generateRandomParameter(null, null);

        Parameter srcParameter = new OzoneRandomPathType()
                .generateRandomParameter(state, null);

        Parameter dstParameter = new OzoneDirPathType()
                .generateRandomParameter(state, null);

        params.add(mvcmd);
        params.add(srcParameter);
        params.add(dstParameter);
    }

    @Override
    public String constructCommandString() {
        return "fs" + " " +
                params.get(0).toString() + " " +
                subdir +
                params.get(1).toString() + " " +
                subdir +
                params.get(2).toString();
    }

    @Override
    public void updateState(State state) {
        OzoneState ozoneState = (OzoneState) state;

        // move to new path
        for (String dir : ozoneState.dfs.getDirs(params.get(1).toString())) {
            String newDir = dir.replace(params.get(1).toString(),
                    params.get(2).toString());
            ozoneState.dfs.createDir(newDir);
        }
        for (String file : ozoneState.dfs.getFiles(params.get(1).toString())) {
            String newFile = file.replace(params.get(1).toString(),
                    params.get(2).toString());
            ozoneState.dfs.createFile(newFile);
        }

        // remove the original path
        ozoneState.dfs.removeDir(params.get(1).toString());
        ozoneState.dfs.removeFile(params.get(1).toString());

    }
}
