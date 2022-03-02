package org.zlab.upfuzz;

public class Parameter {

    public ParameterType.ConcreteType type;
    public Object value; // Could contain lower-level parameters

    public Parameter(ParameterType.ConcreteType type, Object value) {
        this.type = type;
        this.value = value;
    }

    public void mutate() {
        System.out.println("hello mutation");
    }

    public boolean isValid(State state) {
        return type.isValid(state, value);
    }

    /**
     * Fix if the param does not comply the rule.
     */
    public void fixIfNotValid(State state) {
        type.fixIfNotValid(state, value);
    }

    @Override
    public String toString() {
        return type.generateStringValue(this);
    }
}
