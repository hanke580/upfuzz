package org.zlab.upfuzz;

import java.util.ArrayList;
import java.util.List;

/**
 * How a parameter can be generated is only defined in its type.
 * If you want special rules for a parameter, you need to implement a type class for it.
 */
public abstract class ParameterType {
    /**
     * generateRandomParameter() follows rules to generate a parameter with a random value.
     * @param s // current state
     * @param c // current command
     *  these rules might use the state and other parameters in the current command.
     */
    public abstract Parameter generateRandomParameter(State s, Command c);

    public abstract String generateStringValue(Parameter p);


    public static abstract class NormalType extends ParameterType {
        public NormalType() {}
    }

    public static abstract class TemplatedType extends ParameterType {
        // Support a variable number of templates. E.g., Pair<K, V>.
        public List<ParameterType> typesInTemplate = new ArrayList<>();

        public TemplatedType(ParameterType t) {
            this.typesInTemplate.add(t);
        }

        public TemplatedType(ParameterType t1, ParameterType t2) {
            this.typesInTemplate.add(t1);
            this.typesInTemplate.add(t2);
        }
    }
}
