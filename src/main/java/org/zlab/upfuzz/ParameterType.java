package org.zlab.upfuzz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * How a parameter can be generated is only defined in its type.
 * If you want special rules for a parameter, you need to implement a type class for it.
 */
public abstract class ParameterType {


    public static abstract class ConcreteType extends ParameterType {
        /**
         * generateRandomParameter() follows rules to generate a parameter with a random value.
         * @param s // current state
         * @param c // current command
         *  these rules might use the state and other parameters in the current command.
         */
        public abstract Parameter generateRandomParameter(State s, Command c);
        public abstract String generateStringValue(Parameter p); // Maybe this should be in Parameter class? It has the concrete type anyways.
    }

    public static abstract class GenericType extends ParameterType {
        // generic type cannot generate value without concrete types
        public abstract Parameter generateRandomParameter(State s, Command c, List<ConcreteType> types);
        public abstract String generateStringValue(Parameter p, List<ConcreteType> types); // Maybe this should be in Parameter class? It has the concrete type anyways.

    }


    // TODO: Name should be ConcreteGenericType
    public static class TemplatedType extends ConcreteType {

        // Use cache so that we won't construct duplicated types.
        public static HashMap<TemplatedType, TemplatedType> cache = new HashMap<>();

        public static TemplatedType constructTemplatedType(GenericType t, ConcreteType t1) {
            TemplatedType ret = new TemplatedType(t, t1);
            if (!cache.keySet().contains(ret)) {
                cache.put(ret, ret);
            }
            return cache.get(ret);
        }

        public static TemplatedType constructTemplatedType(GenericType t, ConcreteType t1, ConcreteType t2) {
            TemplatedType ret = new TemplatedType(t, t1, t2);
            if (!cache.keySet().contains(ret)) {
                cache.put(ret, ret);
            }
            return cache.get(ret);
        }

        // Support a variable number of templates. E.g., Pair<K, V>.
        public GenericType t;
        public List<ConcreteType> typesInTemplate = new ArrayList<>();

        public TemplatedType(GenericType t, ConcreteType t1) {
            this.t = t;
            this.typesInTemplate.add(t1);
        }

        public TemplatedType(GenericType t, ConcreteType t1, ConcreteType t2) {
            this.t = t;
            this.typesInTemplate.add(t1);
            this.typesInTemplate.add(t2);
        }

        public Parameter generateRandomParameter(State s, Command c) {

            return t.generateRandomParameter(s, c, typesInTemplate);
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof TemplatedType)) return false;
            TemplatedType other = (TemplatedType) obj;
            return Objects.equals(this.t, other.t)
                // TODO: Check: Do we need to compare every item in the list?
                && Objects.equals(this.typesInTemplate, other.typesInTemplate);
        }

        @Override
        public String generateStringValue(Parameter p) {
            return t.generateStringValue(p, typesInTemplate);
        }
    }
}
