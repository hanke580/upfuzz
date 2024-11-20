package org.zlab.upfuzz.ozone.fs;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneParameterType.OzoneDirPathType;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.hdfs.MockFS.HadoopFileSystem;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.STRINGType;
import org.zlab.upfuzz.utils.Utilities;

public class Touchz extends Fs {
    public Touchz(OzoneState state) {
        super(state.subdir);

        Parameter mkdirCmd = new CONSTANTSTRINGType("-touchz")
                .generateRandomParameter(null, null);
        params.add(mkdirCmd);

        Parameter pathParameter = new OzoneDirPathType()
                .generateRandomParameter(state, this);
        params.add(pathParameter);

        Parameter fileNameParameter = new STRINGType(20)
                .generateRandomParameter(state, null);
        params.add(fileNameParameter);

        Parameter fileTypeParameter = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities.strings2Parameters(
                        HadoopFileSystem.fileType),
                null).generateRandomParameter(state, this);

        params.add(fileTypeParameter);

        constructCommandString();
    }

    @Override
    public void updateState(State state) {
        // Remove a real objnode to state
        Path dir = Paths.get(params.get(1).toString())
                .resolve(params.get(2).toString());
        String p = dir.toString() + params.get(3).toString();

        ((OzoneState) state).dfs.createFile(p);
    }

    @Override
    public String constructCommandString() {

        Path dir = Paths.get(params.get(1).toString())
                .resolve(params.get(2).toString());
        String p = dir.toString() + params.get(3).toString();
        return "fs" +
                " " + params.get(0) +
                " " + subdir + p;
    }

}
