package org.zlab.upfuzz.cassandra;

import org.zlab.upfuzz.ParameterType;

public class TEXTType implements ParameterType {

    public static final TEXTType instance = new TEXTType();
    public static final String signature = "java.lang.String";

    private TEXTType() {}

    @Override
    public Object constructRandomValue() {
        Object ret = "RandomString"; // TODO: Generate a random string.
        // assert ret.type == TEXTType.instance;
        return ret;
    }

    @Override
    public String generateStringValue(Object value) {
        return (String) value;
    }

}
