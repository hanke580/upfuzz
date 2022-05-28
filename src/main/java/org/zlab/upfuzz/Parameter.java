package org.zlab.upfuzz;

import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;

public class Parameter implements Serializable {

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
         *                      /  \
         *                     /    \
         *             Pair<P1, P2>  Pair<P1, P2>
         *  Sometimes we want to directly get to the low-level values.
         *  It can only be
         *  List<Parameter>
         *  Pair<Parameter>
         *  Primitive type like "String, INT..."
         */
        if (this.value instanceof Parameter) {
            return ((Parameter) this.value).getValue();
        } else {
            return this.value;
        }
    }

    public void setValue(Object val) {
        this.getValue();
    }

    @Override
    public String toString() {
        return type.generateStringValue(this);
    }

    public Parameter clone() {
        return SerializationUtils.clone(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (!(o instanceof Parameter)) {
            return false;
        }

        Parameter p = (Parameter) o;
        return this.toString().equals(p.toString());
    }
}
