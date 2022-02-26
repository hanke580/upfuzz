package org.zlab.upfuzz;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.zlab.upfuzz.cassandra.CassandraTypes.LISTType;

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

    /**
     * Case: If for columns, there are constraints for both text and type, 
     * e.g. both of them cannot be from a specific set, can this impl do that?
     * 
     * columns; // List<Pair<Text, Type>>
     * - NotInCollectionType t = new NotInCollectionType(Pair<Text, Type>, cmd.columns.value, Parameter p -> p.value.left )
     * - NotInCollectionType tt = new NotInCollectionType(t, cmd.columns.value_, Parameter p -> p.value.left )
     * 
     * 2 problems
     * 1. Nested Collection Type
     * - If there are multiple constraints on one parameter, can it handle it?
     * - Yes.
     * 
     * Case: If for columns, there are constraints for both text and type, 
     * e.g. both of them cannot be from a specific set, can this impl do that?
     * columns; // List<Pair<Text, Type>>
     * - NotInCollectionType t = new NotInCollectionType(Pair<Text, Type>, cmd.columns.value, Parameter p -> p.value.left )
     * - NotInCollectionType tt = new NotInCollectionType(t, cmd.columns.value_, Parameter p -> p.value.left )
     */

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

    public static class SubsetType <T,U> extends ConfigurableType {

        public SubsetType(ConcreteType t, Collection<T> configuration) { // change to concreteGenericType
            /**
             * In this case, the ConcreteType should be List<Pair<xxx>>.
             * The set we select from will be the target set targetSet. Let's suppose it's also List<Pair<TEXT, TYPEType>>
             * - we rewrite the generateRandomParameter() in this concreteGenericType
             * - Instead of calling t.generateValue function, it should now construct values by select from targetSet.
             * - Do it by anonymous class extends List, and we write the subset generateRandomParameter() for it.
             */
            super(t, configuration);
            //TODO Auto-generated constructor stub
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c) {
            /**
             * Current t should be concrete generic type List<xxx>
             * - Select from collection set
             */
            assert t instanceof ConcreteGenericTypeOne;

            if (t instanceof ConcreteGenericTypeOne) {
                ConcreteGenericType cGenericType = (ConcreteGenericType) t;
                if (cGenericType.t instanceof LISTType) {

                    LISTType lType = new LISTType() {

                        @Override
                        public Parameter generateRandomParameter(State s, Command c, java.util.List<ConcreteType> typesInTemplate) {
                            List<T> targetSet = new ArrayList<>((Collection<T>) configuration);
                            List<Parameter> value = new ArrayList<>();

                            Random rand = new Random();
                            int setSize = rand.nextInt(targetSet.size()); // specified by user

                            List<Integer> indexArray = new ArrayList<>();
                            for (int i = 0; i < targetSet.size(); i++) {
                                indexArray.add(i);
                            }
                            Collections.shuffle(indexArray);
                      
                            for (int i = 0; i < setSize; i++) {
                              value.add((Parameter) targetSet.get(indexArray.get(i))); // The targetSet should also store Parameter
                            }
                      
                            ConcreteType type = ConcreteGenericType.constructConcreteGenericType(instance, t); // LIST<WhateverType>
                            Parameter ret = new Parameter(type, value);
                      
                            return ret;
                        };
                    };

                    return lType.generateRandomParameter(s, c, null);
                }
            }
            return null;
        }

        @Override
        public String generateStringValue(Parameter p) {
            // TODO Auto-generated method stub
            return null;
        }

    }


    public static class UniqueType <T,U> extends ConfigurableType {

        public UniqueType(ConcreteType t, Collection<T> configuration) {
            /**
             * In this case, the ConcreteType should be List<Pair<xxx>>.
             * The set we select from will be the target set targetSet. Let's suppose it's also List<Pair<TEXT, TYPEType>>
             * - we rewrite the generateRandomParameter() in this concreteGenericType
             * - Instead of calling t.generateValue function, it should now construct values by select from targetSet.
             * - Do it by anonymous class extends List, and we write the subset generateRandomParameter() for it.
             */
            super(t, configuration);
            //TODO Auto-generated constructor stub
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c) {
            // TODO Auto-generated method stub
            // An option to check which value to keep unique (Using map function)

            Parameter ret = t.generateRandomParameter(s, c);
            while() {
                ret = t.generateRandomParameter(s, c);
            }

            /**
             * If value is List type, we want to make sure there are not duplicated values.
             */

            

            return null;
        }

        @Override
        public String generateStringValue(Parameter p) {
            // TODO Auto-generated method stub
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
