package org.zlab.upfuzz.ozone.OzoneParameterType;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;

public class OzoneKeyType extends ParameterType.ConcreteType {

    @Override
    public Parameter generateRandomParameter(State s, Command c, Object init) {
        return null;
    }

    @Override
    public Parameter generateRandomParameter(State s, Command c) {
        assert s instanceof OzoneState;
        OzoneState ozoneState = (OzoneState) s;
        String keyPath = ozoneState.oos.getRandomKeyPath();
        if (keyPath == null) {
            throw new RuntimeException(
                    "cannot generate ozone file path, there's no file in ozone");
        }
        return new Parameter(this, keyPath);
    }

    @Override
    public String generateStringValue(Parameter p) {
        assert p != null && p.type instanceof OzoneKeyType;
        return (String) p.value;
    }

    @Override
    public boolean isValid(State s, Command c, Parameter p) {
        return ((OzoneState) s).oos.containsKey((String) p.getValue());
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
