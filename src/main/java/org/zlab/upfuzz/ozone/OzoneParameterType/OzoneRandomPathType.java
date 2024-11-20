package org.zlab.upfuzz.ozone.OzoneParameterType;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;

import java.util.Random;

public class OzoneRandomPathType extends ParameterType.ConcreteType {
    // Return an existing file or dir

    @Override
    public Parameter generateRandomParameter(State s, Command c, Object init) {
        return null;
    }

    @Override
    public Parameter generateRandomParameter(State s, Command c) {
        OzoneState ozoneState = (OzoneState) s;
        boolean retFile = new Random().nextBoolean();
        String path;
        if (retFile) {
            path = ozoneState.dfs.getRandomFilePath();
        } else {
            path = ozoneState.dfs.getRandomDirPath();
        }
        if (path == null) {
            throw new RuntimeException(
                    "cannot generate a valid path, there's no inode");
        }
        return new Parameter(this, path);
    }

    @Override
    public String generateStringValue(Parameter p) {
        assert p != null && p.type instanceof OzoneRandomPathType;
        return (String) p.value;
    }

    @Override
    public boolean isValid(State s, Command c, Parameter p) {
        OzoneState ozoneState = (OzoneState) s;
        String path = (String) p.getValue();
        return ozoneState.dfs.containsDir(path)
                || ozoneState.dfs.containsFile(path);
    }

    @Override
    public void regenerate(State s, Command c, Parameter p) {

    }

    @Override
    public boolean isEmpty(State s, Command c, Parameter p) {
        return false;
    }

    @Override
    public boolean mutate(State s, Command c, Parameter p) {
        return false;
    }
}
