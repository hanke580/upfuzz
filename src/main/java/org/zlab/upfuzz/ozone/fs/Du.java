package org.zlab.upfuzz.ozone.fs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.ozone.OzoneParameterType.OzoneDirPathType;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class Du extends Fs {
    public Du(OzoneState ozoneState) {
        super(ozoneState.subdir);

        Parameter cmd = new CONSTANTSTRINGType("-du")
                .generateRandomParameter(ozoneState, null);

        Parameter opt1 = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-s"), null)
                        .generateRandomParameter(ozoneState, null);
        Parameter opt2 = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-h"), null)
                        .generateRandomParameter(ozoneState, null);
        Parameter opt3 = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-v"), null)
                        .generateRandomParameter(ozoneState, null);
        Parameter opt4 = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-x"), null)
                        .generateRandomParameter(ozoneState, null);
        Parameter param = new OzoneDirPathType()
                .generateRandomParameter(ozoneState, null);
        params.add(cmd);
        params.add(opt1);
        params.add(opt2);
        if (Config.getConf().support_du_v_opt)
            params.add(opt3);
        params.add(opt4);
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
