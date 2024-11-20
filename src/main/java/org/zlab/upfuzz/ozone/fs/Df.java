package org.zlab.upfuzz.ozone.fs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.ozone.OzoneParameterType.*;
import org.zlab.upfuzz.ozone.OzoneCommand;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class Df extends Fs {

    public Df(OzoneState ozoneState) {
        super(ozoneState.subdir);

        Parameter cmd = new CONSTANTSTRINGType("-df")
                .generateRandomParameter(ozoneState, null);

        Parameter opt = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-h"), null)
                        .generateRandomParameter(ozoneState, null);

        Parameter param = new OzoneDirPathType()
                .generateRandomParameter(ozoneState, null);

        params.add(cmd);
        params.add(opt);
        params.add(param);
    }

    @Override
    public String constructCommandString() {
        return constructCommandStringWithDirSeparation("fs");
    }

    @Override
    public void updateState(State state) {
    }
}
