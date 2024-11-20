package org.zlab.upfuzz.ozone.fs;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneParameterType.OzoneDirPathType;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.STRINGType;

public class Mkdir extends Fs {

    public Mkdir(OzoneState state) {
        super(state.subdir);

        Parameter mkdirCmd = new CONSTANTSTRINGType("-mkdir")
                .generateRandomParameter(null, null);
        params.add(mkdirCmd);

        Parameter parentPathParameter = new OzoneDirPathType()
                .generateRandomParameter(state, null);
        params.add(parentPathParameter);

        Parameter dirNameParameter = new STRINGType(20)
                .generateRandomParameter(state, null);
        params.add(dirNameParameter);
    }

    @Override
    public void updateState(State state) {
        // Add a real objnode to state
        Path dir = Paths.get(params.get(1).toString());
        String p = dir.resolve(params.get(2).toString()).toString();
        ((OzoneState) state).dfs.createDir(p);
    }

    @Override
    public String constructCommandString() {
        Path dir = Paths.get(params.get(1).toString());
        String p = dir.resolve(params.get(2).toString()).toString();
        return "fs" + " " + params.get(0) +
                " " + subdir + p;
    }

}
