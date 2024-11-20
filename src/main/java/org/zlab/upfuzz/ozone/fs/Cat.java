package org.zlab.upfuzz.ozone.fs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneParameterType.OzoneFilePathType;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class Cat extends Fs {

    /*
     * Moves files from source to destination. This command allows multiple
     * sources as well in which case the destination needs to be a directory.
     * Moving files across file systems is not permitted.
     */
    public Cat(OzoneState ozoneState) {
        super(ozoneState.subdir);

        Parameter catCmd = new CONSTANTSTRINGType("-cat")
                .generateRandomParameter(ozoneState, null);

        // The -ignoreCrc option disables checkshum verification.
        Parameter crcOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-ignoreCrc"), null)
                        .generateRandomParameter(ozoneState, null);

        Parameter pathParameter = new OzoneFilePathType()
                .generateRandomParameter(ozoneState, null);

        params.add(catCmd);
        params.add(crcOption);
        params.add(pathParameter);
    }

    @Override
    public String constructCommandString() {
        return "fs" + " " +
                params.get(0) + " " +
                params.get(1) + " " +
                subdir +
                params.get(2);
    }

    @Override
    public void updateState(State state) {
    }
}
