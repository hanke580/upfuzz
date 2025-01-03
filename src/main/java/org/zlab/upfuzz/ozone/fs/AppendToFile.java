package org.zlab.upfuzz.ozone.fs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneParameterType.OzoneFilePathType;
import org.zlab.upfuzz.ozone.OzoneParameterType.RandomLocalPathType;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class AppendToFile extends Fs {

    public AppendToFile(OzoneState state) {
        super(state.subdir);

        Parameter catCmd = new CONSTANTSTRINGType("-appendToFile")
                .generateRandomParameter(state, null);

        Parameter srcParameter = new RandomLocalPathType()
                .generateRandomParameter(state, null);
        Parameter destParameter = new OzoneFilePathType()
                .generateRandomParameter(state, null);

        params.add(catCmd);
        params.add(srcParameter);
        params.add(destParameter);
    }

    @Override
    public String constructCommandString() {
        return constructCommandStringWithDirSeparation("fs");
    }

    @Override
    public void updateState(State state) {
    }
}