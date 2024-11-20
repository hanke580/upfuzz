package org.zlab.upfuzz.ozone.fs;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneParameterType.OzoneDirPathType;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.STRINGType;

public class CreateSnapshot extends Fs {

    public CreateSnapshot(OzoneState state) {
        super(state.subdir);

        Parameter cmd = new CONSTANTSTRINGType("-createSnapshot")
                .generateRandomParameter(state, null);

        Parameter destParameter = new OzoneDirPathType()
                .generateRandomParameter(state, null);

        Parameter name = new ParameterType.OptionalType(new STRINGType(20),
                null)
                        .generateRandomParameter(state, null);

        params.add(cmd);
        params.add(destParameter);
        params.add(name);
    }

    @Override
    public String constructCommandString() {
        return "fs" + " " +
                params.get(0) + " " +
                subdir +
                params.get(1) + " " +
                params.get(2);
    }

    @Override
    public void updateState(State state) {
        String dir = params.get(1).toString();
        String filename = params.get(2).toString();
        ((OzoneState) state).dfs.createSnapShotFile(dir, filename);
    }
}
