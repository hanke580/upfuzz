package org.zlab.upfuzz.ozone.fs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneParameterType.*;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class Cp extends Fs {

    /*
     * Copy files from source to destination. This command allows multiple
     * sources as well in which case the destination must be a directory.
     */
    public Cp(OzoneState state) {
        super(state.subdir);

        Parameter cpcmd = new CONSTANTSTRINGType("-cp")
                .generateRandomParameter(null, null);

        // -f : Overwrites the destination if it already exists.
        Parameter fOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-f"), null)
                        .generateRandomParameter(null, null);

        Parameter pOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-p"), null)
                        .generateRandomParameter(null, null);

        // -d : Skip creation of temporary file with the suffix ._COPYING_.
        Parameter dOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-d"), null)
                        .generateRandomParameter(null, null);

        Parameter srcParameter = new OzoneRandomPathType()
                .generateRandomParameter(state, null);

        Parameter dstParameter = new OzoneDirPathType()
                .generateRandomParameter(state, null);

        params.add(cpcmd);
        params.add(fOption);
        params.add(pOption);
        params.add(dOption);
        params.add(srcParameter);
        params.add(dstParameter);
    }

    @Override
    public String constructCommandString() {
        return "fs" + " " +
                params.get(0) + " " +
                params.get(1) + " " +
                params.get(2) + " " +
                params.get(3) + " " +
                subdir +
                params.get(4) + " " +
                subdir +
                params.get(5);
    }

    @Override
    public void updateState(State state) {
        OzoneState ozoneState = (OzoneState) state;
        for (String dir : ozoneState.dfs.getDirs(params.get(4).toString())) {
            String newDir = dir.replaceFirst(params.get(4).toString(),
                    params.get(5).toString());
            ozoneState.dfs.createDir(newDir);
        }
        /**
         * Fixme: if folder name is the same as the file name, the file name will also be replaced
         * From: /QRRcNMCmcmFTZSXg/QRRcNMCmcmFTZSXg.xml
         * TO: //.xml
         */
        for (String file : ozoneState.dfs.getFiles(params.get(4).toString())) {
            // This is not correct!
            String newFile = file.replaceFirst(params.get(4).toString(),
                    params.get(5).toString());
            ozoneState.dfs.createFile(newFile);
        }
    }
}
