package org.zlab.upfuzz.utils;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;

import java.util.Random;

public class INTType extends ParameterType.ConcreteType {

    private final int MAX_VALUE = Integer.MAX_VALUE;

    public final Integer max;
    public final Integer min;

//    public static final INTType instance = new INTType();
    public static final String signature = "java.lang.String";

    public INTType() {
        max = null;
        min = null;
    }

    public INTType(int max) {
        this.min = null;
        this.max = max;
    }

    public INTType(int min, int max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public Parameter generateRandomParameter(State s, Command c, Object init) {
        if (init == null)
            return generateRandomParameter(s, c);
        assert init instanceof Integer;
        Integer initValue = (Integer) init;
        return new Parameter(this, initValue);
    }

    @Override
    public Parameter generateRandomParameter(State s, Command c) {
        Integer value;

        if (max == null && min == null) {
            value = new Random().nextInt();
        } else if (max != null && min == null) {
            value = new Random().nextInt(max);
        } else if (max == null && min != null) {
            value = new Random().nextInt(Integer.MAX_VALUE) + min;
        } else {
            value = new Random().nextInt(max - min) + min;
        }
        return new Parameter(this, (Integer) value);
    }

    @Override
    public String generateStringValue(Parameter p) {
        assert p.type instanceof INTType;
        return String.valueOf((int) p.value);
    }

    @Override
    public boolean isValid(State s, Command c, Parameter p) {
        assert p.value instanceof Integer;
        boolean ret;
        if (max == null && min == null) {
            ret = true;
        } else if (max != null && min == null) {
            ret = (Integer) p.value < max;
        } else if (max == null && min != null) {
            ret = (Integer) p.value >= min;
        } else {
            ret = (Integer) p.value < max && (Integer) p.value >= min;
        }
        return ret;
    }

    @Override
    public void regenerate(State s, Command c, Parameter p) {
        if (!isValid(s, c, p)) {
            p.value = generateRandomParameter(s, c).value;
        }
    }

    @Override
    public boolean isEmpty(State s, Command c, Parameter p) {
        return false;
    }

    @Override
    public boolean mutate(State s, Command c, Parameter p) {
        p.value = generateRandomParameter(s, c).value;
        return true;
    }

    @Override
    public String toString() {
        return "INT";
    }
}
