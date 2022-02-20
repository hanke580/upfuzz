package org.zlab.upfuzz;

public interface ParameterType {
    String generateStringValue(Object value);

    public static interface NormalType extends ParameterType {
        Object constructRandomValue();
    }

    public static interface TemplatedType extends ParameterType {
        /* The types in the template are stored in the corresponding parameter. */
        Object constructRandomValue(Parameter.TemplatedParameter parameter);
        Object constructRandomValue(ParameterType typeInTemplate);
    }

}
