package org.zlab.upfuzz;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    /**
     * ConfigurableType uses its concrete type to generate values,
     * but it adds extra rules/constraints using configuration.
     * TODO: Need more reasoning to finish this.
     */
    public static abstract class ConfigurableType extends ConcreteType {
        public final ConcreteType t;
        public final Object configuration;

        public ConfigurableType(ConcreteType t, Object configuration) {

            this.t = t;
            this.configuration = configuration;
        }
    }

    // columns; // List<Pair<Text, Type>>
    // NotInCollectionType t = new NotInCollectionType(Pair<Text, Type>, cmd.columns.value, Parameter p -> p.value.left )
    // List<t(Pair<Text, Type>)> columns;

    // t = Pair<TEXT, TYPE>
    // s = List<Pair<TEXT, Type>>
    // t.left not exist in s.stream().map(p -> p.left).collect(Collectors.toSet())

    public static class NotInCollectionType <T,U> extends ConfigurableType {

        public final Function<T, U> mapFunc;

        // Example of mapFunc:
        // Parameter p -> p.value.left // p's ParameterType is Pair<TEXT, TEXTType>
        public NotInCollectionType(ConcreteType t, Collection<T> configuration, Function<T, U> mapFunc) {
            super(t, configuration);
            this.mapFunc = mapFunc;
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c) {
            Parameter ret = t.generateRandomParameter(s, c); // ((Pair<TEXTType, TYPEType>)ret.value).left
            while (((Collection<T>) configuration).stream().map(mapFunc).collect(Collectors.toSet()).contains(ret)) {
                ret = t.generateRandomParameter(s, c);
            }
            return ret;
        }

        @Override
        public String generateStringValue(Parameter p) {
            return null;
        }
    }


    public static abstract class GenericTypeOne extends GenericType { }
    public static abstract class GenericTypeTwo extends GenericType { }

    // ConcreteGenericType: List<Pair<Text, Type>>
    // ConcreteGenericType: (Pair<Text, Type>)
    public abstract static class ConcreteGenericType extends ConcreteType {
        // Support a variable number of templates. E.g., Pair<K, V>.
        public GenericType t;
        public List<ConcreteType> typesInTemplate = new ArrayList<>();


        public Parameter generateRandomParameter(State s, Command c) {
            return t.generateRandomParameter(s, c, typesInTemplate);
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ConcreteGenericTypeOne)) return false;
            ConcreteGenericTypeOne other = (ConcreteGenericTypeOne) obj;
            return Objects.equals(this.t, other.t)
                // TODO: Check: Do we need to compare every item in the list?
                && Objects.equals(this.typesInTemplate, other.typesInTemplate);
        }

        @Override
        public String generateStringValue(Parameter p) {
            return t.generateStringValue(p, typesInTemplate);
        }

        // Use cache so that we won't construct duplicated types.
        public static HashMap<ConcreteGenericType, ConcreteGenericType> cache = new HashMap<>();

        public static ConcreteGenericType constructConcreteGenericType(GenericType t, ConcreteType t1) {
            ConcreteGenericType ret = new ConcreteGenericTypeOne(t, t1);
            if (!cache.keySet().contains(ret)) {
                cache.put(ret, ret);
            }
            return cache.get(ret);
        }

        public static ConcreteGenericType constructConcreteGenericType(GenericType t, ConcreteType t1, ConcreteType t2) {
            ConcreteGenericType ret = new ConcreteGenericTypeTwo(t, t1, t2);
            if (!cache.keySet().contains(ret)) {
                cache.put(ret, ret);
            }
            return cache.get(ret);
        }
    }

    public static class ConcreteGenericTypeOne extends ConcreteGenericType {

        public ConcreteGenericTypeOne(GenericType t, ConcreteType t1) {
            assert t instanceof GenericTypeOne; // This is ugly... Might need to change design... Leave it for now.
            this.t = t;
            this.typesInTemplate.add(t1);
        }

    }

    public static class ConcreteGenericTypeTwo extends ConcreteGenericType {

        public ConcreteGenericTypeTwo(GenericType t, ConcreteType t1, ConcreteType t2) {
            assert t instanceof GenericTypeTwo; // This is ugly...
            this.t = t;
            this.typesInTemplate.add(t1);
            this.typesInTemplate.add(t2);
        }

    }
}
