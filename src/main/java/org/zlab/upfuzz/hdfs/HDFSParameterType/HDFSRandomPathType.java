package org.zlab.upfuzz.hdfs.HDFSParameterType;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HdfsState;

import java.util.Random;

public class HDFSRandomPathType extends ParameterType.ConcreteType {
    // Return an existing file or dir

    @Override
    public Parameter generateRandomParameter(State s, Command c, Object init) {
        return null;
    }

    @Override
    public Parameter generateRandomParameter(State s, Command c) {
        HdfsState hdfsState = (HdfsState) s;
        boolean retFile = new Random().nextBoolean();
        String path;
        if (retFile) {
            path = hdfsState.dfs.getRandomFilePath();
        } else {
            path = hdfsState.dfs.getRandomDirPath();
        }
        if (path == null) {
            throw new RuntimeException(
                    "cannot generate a valid path, there's no inode");
        }
        return new Parameter(this, path);
    }

    @Override
    public String generateStringValue(Parameter p) {
        assert p != null && p.type instanceof HDFSRandomPathType;
        return (String) p.value;
    }

    @Override
    public boolean isValid(State s, Command c, Parameter p) {
        HdfsState hdfsState = (HdfsState) s;
        String path = (String) p.getValue();
        return hdfsState.dfs.containsDir(path)
                || hdfsState.dfs.containsFile(path);
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
