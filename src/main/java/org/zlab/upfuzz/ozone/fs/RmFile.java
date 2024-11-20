package org.zlab.upfuzz.ozone.fs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneParameterType.*;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class RmFile extends Fs {

    public RmFile(OzoneState ozoneState) {
        super(ozoneState.subdir);

        Parameter rmcmd = new CONSTANTSTRINGType("-rm")
                .generateRandomParameter(null, null);

        // The -f option will not display a diagnostic message or modify the
        // exit status to reflect an error if the file does not exist.
        Parameter fOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-f"), null)
                        .generateRandomParameter(null, null);

        // The -safely option will require safety confirmation before deleting
        // directory with total number of files greater than
        // hadoop.shell.delete.limit.num.files (in core-site.xml, default: 100).
        // It can be used with -skipTrash to prevent accidental deletion of
        // large directories. Delay is expected when walking over large
        // directory recursively to count the number of files to be deleted
        // before the confirmation.
        Parameter saveOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-safely"), null)
                        .generateRandomParameter(null, null);

        // -d : Skip creation of temporary file with the suffix ._COPYING_.
        Parameter skipOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-skipTrash"), null)
                        .generateRandomParameter(null, null);

        Parameter dstParameter = new OzoneFilePathType()
                .generateRandomParameter(ozoneState, null);

        params.add(rmcmd);
        params.add(fOption);
        params.add(saveOption);
        params.add(skipOption);
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
                params.get(4);
    }

    @Override
    public void updateState(State state) {
        ((OzoneState) state).dfs.removeFile(params.get(4).toString());
    }
}
