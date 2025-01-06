package org.zlab.upfuzz.utils;

import java.util.UUID;
import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;

public class UUIDType extends ParameterType.ConcreteType {

    public static final UUIDType instance = new UUIDType();

    public static String generateUUID() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "uuid" + uuid;
    }

    @Override
    public Parameter generateRandomParameter(State s, Command c, Object init) {
        if (init == null) {
            return generateRandomParameter(s, c);
        }
        assert init instanceof String;
        String initValue = (String) init;
        return new Parameter(UUIDType.instance, initValue);
    }

    @Override
    public Parameter generateRandomParameter(State s, Command c) {
        Parameter ret = new Parameter(UUIDType.instance, generateUUID());
        while (!isValid(s, c, ret)) {
            ret = new Parameter(this, generateUUID());
        }
        // Original Codes:
        return ret;
    }

    @Override
    public String generateStringValue(Parameter p) {
        return (String) p.value;
    }

    @Override
    public boolean isValid(State s, Command c, Parameter p) {
        if (p == null || !(p.type instanceof UUIDType))
            return false;
        return true;
    }

    @Override
    public void regenerate(State s, Command c, Parameter p) {
        Parameter ret = generateRandomParameter(s, c);
        p.value = ret.value;
    }

    @Override
    public boolean isEmpty(State s, Command c, Parameter p) {
        return ((String) p.value).isEmpty();
    }

    @Override
    public boolean mutate(State s, Command c, Parameter p) {
        regenerate(s, c, p);
        return true;
    }
}
