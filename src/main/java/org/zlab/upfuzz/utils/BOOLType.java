package org.zlab.upfuzz.utils;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;

import java.util.Random;

public class BOOLType extends ParameterType.ConcreteType {

    public static final String signature = "java.lang.Boolean";

    @Override
    public Parameter generateRandomParameter(State s, Command c, Object init) {
        if (init == null)
            return generateRandomParameter(s, c);
        assert init instanceof Boolean;
        Boolean initValue = (Boolean) init;
        return new Parameter(this, initValue);
    }

    @Override
    public Parameter generateRandomParameter(State s, Command c) {
        return new Parameter(this, new Random().nextBoolean());
    }

    @Override
    public String generateStringValue(Parameter p) {
        assert p.type instanceof BOOLType;
        return String.valueOf((boolean) p.value);
    }

    @Override
    public boolean isValid(State s, Command c, Parameter p) {
        if (!(p.type instanceof BOOLType))
            return false;
        return true;
    }

    @Override
    public void regenerate(State s, Command c, Parameter p) {
        if (isValid(s, c, p)) {
            p.value = generateRandomParameter(s, c).value;
        }
    }

    @Override
    public boolean isEmpty(State s, Command c, Parameter p) {
        return false;
    }

    @Override
    public boolean mutate(State s, Command c, Parameter p) {
        p.value = !(boolean) p.value;
        return true;
    }

    @Override
    public String toString() {
        return "BOOLEAN";
    }
}
