package org.zlab.upfuzz.hdfs.HDFSParameterType;

import java.io.File;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType.ConcreteType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HDFSState;
import org.zlab.upfuzz.hdfs.MockFS.INode;

public class RandomLocalPathType extends ConcreteType
{
    String file;

    @Override
    public Parameter generateRandomParameter(State s, Command c, Object init) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Parameter generateRandomParameter(State s, Command c) {
        HDFSState hdfsState = (HDFSState)s;
        file = hdfsState.getRandomLocalPathString();
        return new Parameter(this, file);
    }

    @Override
    public String generateStringValue(Parameter p) {
        String filename = (String)p.getValue();
        return filename;
    }

    @Override
    public boolean isValid(State s, Command c, Parameter p) {
        return true;
    }

    @Override
    public void regenerate(State s, Command c, Parameter p) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isEmpty(State s, Command c, Parameter p) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean mutate(State s, Command c, Parameter p) {
        // TODO Auto-generated method stub
        return false;
    }

}
