package org.zlab.upfuzz;

public class Parameter {

    public ParameterType.ConcreteType type;
    public Object value; // Could contain lower-level parameters

    public Parameter(ParameterType.ConcreteType type, Object value) {
        this.type = type;
        this.value = value;
    }

    public boolean isEmpty(State s, Command c) {
        return type.isEmpty(s, c, this);
    }

    public boolean mutate(State s, Command c) {
        System.out.println("hello mutation");
        return type.mutate(s, c, this);
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

    public Object getValue() {
        /**
         * Get non-parameter value
         *         p
         *       /   \
         *      /     \
         *  NotEmpty   p
         *           /   \
         *          /     \
         *     ConcreteGen List<Parameter>
         *
         *  Sometimes we want to directly get to the low-level values
         */
        if (this.value instanceof Parameter) {
            return ((Parameter) this.value).getValue();
        } else {
            return this.value;
        }
    }

    @Override
    public String toString() {
        return type.generateStringValue(this);
    }



}
