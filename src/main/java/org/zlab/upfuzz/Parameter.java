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

    public boolean isValid(State s) {
        return type.isValid(s, value);
    }

    /**
     * Fix if the param does not comply the rule.
     */
    public void fixIfNotValid(State s) {
        type.fixIfNotValid(s, value);
    }

    @Override
    public String toString() {
        return type.generateStringValue(this);
    }
}
