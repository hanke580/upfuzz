package org.zlab.upfuzz.ozone.OzoneParameterType;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ParameterType.ConcreteType;
import org.zlab.upfuzz.ozone.OzoneState;

public class RandomLocalPathType extends ConcreteType {

    String file;

    @Override
    public Parameter generateRandomParameter(State s, Command c, Object init) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Parameter generateRandomParameter(State s, Command c) {
        OzoneState ozoneState = (OzoneState) s;
        file = ozoneState.getRandomLocalPathString();
        return new Parameter(this, file);
    }

    public Parameter generateRandomFileParameter(State s, Command c) {
        OzoneState ozoneState = (OzoneState) s;
        file = ozoneState.getRandomLocalFilePathString();
        return new Parameter(this, file);
    }

    @Override
    public String generateStringValue(Parameter p) {
        return (String) p.getValue();
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
