package org.zlab.upfuzz.ozone.fs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.ozone.OzoneParameterType.OzoneFilePathType;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class Checksum extends Fs {

    public Checksum(OzoneState state) {
        super(state.subdir);

        Parameter catCmd = new CONSTANTSTRINGType("-checksum")
                .generateRandomParameter(state, null);

        Parameter opt = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-v"), null)
                        .generateRandomParameter(null, null);

        Parameter destParameter = new OzoneFilePathType()
                .generateRandomParameter(state, null);

        params.add(catCmd);
        if (Config.getConf().support_checksum_v_opt)
            params.add(opt);
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