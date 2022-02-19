package org.zlab.upfuzz;

public interface ParameterType {
    String generateStringValue(Object value);

    public static interface NormalType extends ParameterType {
        Object constructRandomValue();
    }

    public static interface TemplatedType extends ParameterType {
        Object constructRandomValue(ParameterType typeInTemplate);
    }
}
