package org.zlab.upfuzz.hdfs.HDFSParameterType;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HdfsState;

public class HDFSSnapshotPathType extends ParameterType.ConcreteType {
    // Return path to a snapshot

    @Override
    public Parameter generateRandomParameter(State s, Command c, Object init) {
        return null;
    }

    @Override
    public Parameter generateRandomParameter(State s, Command c) {
        assert s instanceof HdfsState;
        HdfsState hdfsState = (HdfsState) s;
        String filePath = hdfsState.dfs.getRandomSnapshotPath();
        if (filePath == null) {
            throw new RuntimeException(
                    "cannot generate hdfs file path, there's no file in hdfs");
        }
        return new Parameter(this, filePath);
    }

    @Override
    public String generateStringValue(Parameter p) {
        assert p != null && p.type instanceof HDFSSnapshotPathType;
        return (String) p.value;
    }

    @Override
    public boolean isValid(State s, Command c, Parameter p) {
        return ((HdfsState) s).dfs.containsFile((String) p.getValue());
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
