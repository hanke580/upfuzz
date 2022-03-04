package org.zlab.upfuzz;

public class Parameter {

    public ParameterType.ConcreteType type;
    public Object value; // Could contain lower-level parameters

    public Parameter(ParameterType.ConcreteType type, Object value) {
        this.type = type;
        this.value = value;
    }

    public void mutate(State s, Command c) {
        type.mutate(c, s, this);
        System.out.println("hello mutation");
    }

    public boolean isValid(State s, Command c) {
        return type.isValid(s, c, this);
    }

    /**
     * Fix if the param does not comply the rule.
     */
    public void regenerate(State s, Command c) {
        type.regenerate(s, c, this);
    }

    @Override
    public String toString() {
        return type.generateStringValue(this);
    }
}
