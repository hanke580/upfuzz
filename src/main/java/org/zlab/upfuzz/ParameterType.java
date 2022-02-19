package org.zlab.upfuzz;

public interface ParameterType {
    Object constructRandomValue();
    String generateStringValue(Object value);
}
