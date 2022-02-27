package org.zlab.upfuzz.utils;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;

public class STRINGType extends ParameterType.ConcreteType {

    public static final STRINGType instance = new STRINGType();
    public static final String signature = "java.lang.String";


    @Override
    public Parameter generateRandomParameter(State s, Command c) {
        // TODO: generate a random string.
        return null;
    }

    @Override
    public String generateStringValue(Parameter p) {
        return null;
    }

}

