package org.zlab.upfuzz.ozone.fs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class Expunge extends Fs {

    public Expunge(OzoneState ozoneState) {
        super(ozoneState.subdir);

        Parameter cmd = new CONSTANTSTRINGType("-expunge")
                .generateRandomParameter(ozoneState, null);

        Parameter opt = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-immediate"), null)
                        .generateRandomParameter(ozoneState, null);
        params.add(cmd);
        params.add(opt);
    }

    @Override
    public void updateState(State state) {
    }
}
