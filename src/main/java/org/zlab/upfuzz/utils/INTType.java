package org.zlab.upfuzz.utils;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;

import java.util.Random;

public class INTType extends ParameterType.ConcreteType {

    private final int MAX_VALUE = Integer.MAX_VALUE;

    public static final INTType instance = new INTType();
    public static final String signature = "java.lang.String";

    @Override
    public Parameter generateRandomParameter(State s, Command c) {
        return new Parameter(INTType.instance, new Random().nextInt(MAX_VALUE));
    }

    @Override
    public String generateStringValue(Parameter p) {
        assert p.type instanceof INTType;
        return String.valueOf((int) p.value);
    }

    @Override
    public boolean isValid(State s, Command c, Parameter v) {
        return false;
    }

    @Override
    public void regenerate(State s, Command c, Parameter p) {

    }

    @Override
    public boolean isEmpty(State s, Command c, Parameter p) {
        return false;
    }

    @Override
    public void mutate(State s, Command c, Parameter p) {

    }
}
