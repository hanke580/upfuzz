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

//    public abstract void mutate(State state, Object value);

    public static abstract class ConcreteType extends ParameterType {
        /**
         * generateRandomParameter() follows rules to generate a parameter with a random value.
         * @param s // current state
         * @param c // current command
         *  these rules might use the state and other parameters in the current command.
         */
        public abstract Parameter generateRandomParameter(State s, Command c);
        public abstract String generateStringValue(Parameter p); // Maybe this should be in Parameter class? It has the concrete type anyways.

        public abstract boolean isValid(State s, Command c, Parameter p);
        public abstract void regenerate(State s, Command c, Parameter p);
        public abstract boolean isEmpty(State s, Command c, Parameter p);
        public abstract void mutate(Command c, State s, Parameter p);
    }

    public static abstract class GenericType extends ParameterType {
        // generic type cannot generate value without concrete types
        public abstract Parameter generateRandomParameter(State s, Command c, List<ConcreteType> types);
        public abstract String generateStringValue(Parameter p, List<ConcreteType> types); // Maybe this should be in Parameter class? It has the concrete type anyways.
        public abstract boolean isEmpty(State s, Command c, Parameter p, List<ConcreteType> types);
    }

    /**
     * ConfigurableType uses its concrete type to generate values,
     * but it adds extra rules/constraints using configuration.
     * TODO: Need more reasoning to finish this.
     */
    public static abstract class ConfigurableType extends ConcreteType {
        public final ConcreteType t;
        public final Object configuration;
//        public final Function mapFunc;

        public ConfigurableType(ConcreteType t, Object configuration) {
            this.t = t;
            this.configuration = configuration;
//            this.mapFunc = mapFunc;
        }
    }

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
            while (!isValid(s, c, ret)) {
                ret = t.generateRandomParameter(s, c);
            }
            return new Parameter(this, ret);
        }

        @Override
        public String generateStringValue(Parameter p) {
            return t.generateStringValue((Parameter) p.value);
        }

        @Override
        public boolean isValid(State s, Command c, Parameter p) {
            if (((Collection<T>) configuration).isEmpty())
                return true;
            if (mapFunc == null) {
                return !((Collection<T>) configuration).stream().collect(Collectors.toSet()).contains(p.value);
            } else {
                return !((Collection<T>) configuration).stream().map(mapFunc).collect(Collectors.toSet()).contains(p.value);
            }
        }

        @Override
        public void regenerate(State s, Command c, Parameter p) {
            Parameter ret = generateRandomParameter(s, c); // ((Pair<TEXTType, TYPEType>)ret.value).left
            p.value = ret.value;
        }

        @Override
        public boolean isEmpty(State s, Command c, Parameter p) {
            return t.isEmpty(s, c, (Parameter) p.value);
        }

        @Override
        public void mutate(Command c, State s, Parameter p) {
            t.mutate(c, s, (Parameter) p.value);
        }
    }

    public static class NotEmpty extends ConfigurableType {

        public NotEmpty(ConcreteType t) {
            super(t, null);
        }

        public NotEmpty(ConcreteType t, Object configuration) {
            super(t, configuration);
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c) {
            /**
             *      p
             *    /  \
             *  this  ret
             */
            Parameter ret = t.generateRandomParameter(s, c);
            while (t.isEmpty(s, c, ret)) {
                ret = t.generateRandomParameter(s, c);
            }
            return new Parameter(this, ret);
        }

        @Override
        public String generateStringValue(Parameter p) {
            return t.generateStringValue((Parameter) p.value);
        }

        @Override
        public boolean isValid(State s, Command c, Parameter p) {
            /**
             *      p
             *    /  \
             *  this  value2check
             */
            return !t.isEmpty(s, c, (Parameter) p.value);
        }

        @Override
        public void regenerate(State s, Command c, Parameter p) {
            Parameter ret = generateRandomParameter(s, c);
            p.value = ret.value;
        }

        @Override
        public boolean isEmpty(State s, Command c, Parameter p) {
            return false;
        }

        @Override
        public void mutate(Command c, State s, Parameter p) {
            t.mutate(c, s, (Parameter) p.value);
        }
    }

    public static class SubsetType <T,U> extends ConfigurableType {

        Function mapFunc;

        public SubsetType(ConcreteType t, Collection<T> configuration, Function mapFunc) { // change to concreteGenericType
            /**
             * In this case, the ConcreteType should be List<Pair<xxx>>.
             * The set we select from will be the target set targetSet. Let's suppose it's also List<Pair<TEXT, TYPEType>>
             * - we rewrite the generateRandomParameter() in this concreteGenericType
             * - Instead of calling t.generateValue function, it should now construct values by select from targetSet.
             * - Do it by anonymous class extends List, and we write the subset generateRandomParameter() for it.
             */
            super(t, configuration);
            this.mapFunc = mapFunc;
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c) {
            /**
             * Current t should be concrete generic type List<xxx>
             * - Select from collection set
             */

            Object tmpCollection;

            if (mapFunc != null) {
                tmpCollection = ((Collection<T>) configuration).stream().map(mapFunc).collect(Collectors.toList());
            } else {
                tmpCollection = (Collection) configuration;
            }

            /**
             * Pick a subset from the configuration, it will also be a list of parameters
             * Return new Parameter(SubsetType, value)
             */

            List<Object> targetSet = new ArrayList<Object>((Collection<Object>) tmpCollection);
            List<Object> value = new ArrayList<>();

            if (targetSet.size() > 0) {
                Random rand = new Random();
                int setSize = rand.nextInt(targetSet.size() + 1); // specified by user

                List<Integer> indexArray = new ArrayList<>();
                for (int i = 0; i < targetSet.size(); i++) {
                    indexArray.add(i);
                }
                Collections.shuffle(indexArray);

                for (int i = 0; i < setSize; i++) {
                    value.add(targetSet.get(indexArray.get(i))); // The targetSet should also store Parameter
                }
            }

            return new Parameter(this, value);
        }

        @Override
        public String generateStringValue(Parameter p) {
            // TODO Auto-generated method stub
            StringBuilder sb = new StringBuilder();
            List<Parameter> l = (List<Parameter>) p.value;
            for (int i = 0; i < l.size(); i++) {
                sb.append(l.get(i).toString());
                if (i < l.size() - 1) {
                    sb.append(", ");
                }
            }
            return sb.toString();
        }

        @Override
        public boolean isValid(State s, Command c, Parameter p) {
            return false;
        }

        @Override
        public void regenerate(State s, Command c, Parameter p) {

        }

        @Override
        public boolean isEmpty(State s, Command c, Parameter p) {
            // TODO: Impl
            return  ((List<Object>) p.value).isEmpty();
        }

        @Override
        public void mutate(Command c, State s, Parameter p) {

        }
    }

    /**
     * TODO: StreamMapType might not be a configurable type.
     */
    public static class StreamMapType extends ConfigurableType {

        Function mapFunc;

        public StreamMapType(ConcreteType t, Collection configuration, Function mapFunc) { // change to concreteGenericType
            /**
             * In this case, the ConcreteType should be List<Pair<xxx>>.
             * The set we select from will be the target set targetSet. Let's suppose it's also List<Pair<TEXT, TYPEType>>
             * - we rewrite the generateRandomParameter() in this concreteGenericType
             * - Instead of calling t.generateValue function, it should now construct values by select from targetSet.
             * - Do it by anonymous class extends List, and we write the subset generateRandomParameter() for it.
             */
            super(t, configuration);
            this.mapFunc = mapFunc;
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c) {
            /**
             * Current t should be concrete generic type List<xxx>
             * - Select from collection set
             */

            Object tmpCollection;

            if (mapFunc != null) {
                tmpCollection = ((Collection) configuration).stream().map(mapFunc).collect(Collectors.toList());
            } else {
                tmpCollection = (Collection) configuration;
            }

            return new Parameter(this, tmpCollection);
        }

        @Override
        public String generateStringValue(Parameter p) {
            // TODO: This method is not general
            StringBuilder sb = new StringBuilder();
            List<Parameter> l = (List<Parameter>) p.value;
            for (int i = 0; i < l.size(); i++) {
                sb.append(l.get(i).toString());
                if (i < l.size() - 1)
                    sb.append(", ");
            }
            return sb.toString();
        }

        @Override
        public boolean isValid(State s, Command c, Parameter p) {
            // TODO: Impl
            return false;
        }

        @Override
        public void regenerate(State s, Command c, Parameter p) {
            // TODO: Impl
        }

        @Override
        public boolean isEmpty(State s, Command c, Parameter p) {
            // TODO: Impl
            return false;
        }

        @Override
        public void mutate(Command c, State s, Parameter p) {
            // TODO: Impl
        }
    }

    public static class OptionalType extends ConfigurableType {

        boolean isEmpty;

        public OptionalType(ConcreteType t, Collection configuration) {
            super(t, configuration);
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c) {
            Random rand = new Random();
            isEmpty = rand.nextBoolean();
            if (isEmpty) {
                return new Parameter(this, null);
            } else {
                Parameter ret = t.generateRandomParameter(s, c);
                return new Parameter(this, ret.value);
            }
        }

        @Override
        public String generateStringValue(Parameter p) {
            return isEmpty? "": t.generateStringValue(p);
        }

        @Override
        public boolean isValid(State s, Command c, Parameter p) {
            assert t != null;
            return true;
        }

        @Override
        public void regenerate(State s, Command c, Parameter p) {
            Parameter ret = generateRandomParameter(s, c);
            p.value = ret.value;
        }

        @Override
        public boolean isEmpty(State s, Command c, Parameter p) {
            if (isEmpty) return true;
            else
                return t.isEmpty(s, c, p);
        }

        @Override
        public void mutate(Command c, State s, Parameter p) {
            /**
             * There should be two choices.
             * 1. Mutate current state.
             * 2. Mutate the subvalue.
             * Since the optional parameters are likely to be a constant,
             * we only mutate current isEmpty for now.
             */
            Random rand = new Random();
            assert p.type instanceof OptionalType;
            ((OptionalType) p.type).isEmpty = rand.nextBoolean();
        }
    }

    public static class InCollectionType extends ConfigurableType {
        /**
         * For conflict options, not test yet.
         */

        public int idx;
        public final Function mapFunc;

        public InCollectionType(ConcreteType t, Collection configuration, Function mapFunc) {
            super(t, configuration);
            this.mapFunc = mapFunc;
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c) {
            // Pick one parameter from the collection
            assert ((Collection) configuration).isEmpty() == false;
            Random rand = new Random();

            List l;

            if (mapFunc == null) {
                l = (List) (((Collection)configuration).stream().collect(Collectors.toList()));
            } else {
                l = (List) (((Collection)configuration).stream().map(mapFunc).collect(Collectors.toList()));
            }
            idx = rand.nextInt(l.size());

            Parameter ret = new Parameter(t, l.get(idx));

            return new Parameter(this, ret);
        }

        @Override
        public String generateStringValue(Parameter p) {
            return p.value.toString();
        }

        @Override
        public boolean isValid(State s, Command c, Parameter p) {
            assert p.value instanceof Parameter;
            return ((Parameter) p.value).isValid(s, c);
        }

        @Override
        public void regenerate(State s, Command c, Parameter p) {
            Parameter ret = generateRandomParameter(s, c);
            p.value = ret.value;
        }

        @Override
        public boolean isEmpty(State s, Command c, Parameter p) {
            return ((Parameter) p.value).type.isEmpty(s, c, (Parameter) p.value);
        }

        @Override
        public void mutate(Command c, State s, Parameter p) {
            /**
             * 1. Repick one from the set.
             * 2. Mutate the current picked one
             */
            Random rand = new Random();
            boolean choice = rand.nextBoolean();
            choice = true; // Only pick the first one
            if (choice) {
                // Repick one
                Parameter ret = generateRandomParameter(s, c);
                p.value = ret.value;
            } else {
                // Mutate current picked one. (But usually it will be picked from
                // a specific fixed set. Set this to null for now.
                assert false;
            }
        }
    }


    public static class MustContainType extends ConfigurableType {
        /**
         * For conflict options, not test yet.
         */

        public int idx;
        public final Function mapFunc;

        public MustContainType(ConcreteType t, Collection configuration, Function mapFunc) {
            super(t, configuration);
            this.mapFunc = mapFunc;
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c) {
            /**
             * use ConcreteType t to generate a parameter, it should be a concreteGenericType
             * Check whether this parameter contains everything in the collection
             * If not, add the rest to the collection
             */
            Parameter p = t.generateRandomParameter(s, c);
            assert p.type instanceof ConcreteGenericTypeOne;
            List<Parameter> l = (List<Parameter>) p.value;

            List<Parameter> targetSet = (List) (((Collection)configuration).stream().map(mapFunc).collect(Collectors.toList()));

            for (Parameter m : targetSet) {
                if (!l.contains(m)) {
                    l.add(m);
                }
            }

            return new Parameter(this, p);
        }

        @Override
        public String generateStringValue(Parameter p) {
            return p.value.toString();
        }

        @Override
        public boolean isValid(State s, Command c, Parameter p) {
            assert p.value instanceof Parameter;
            return ((Parameter) p.value).isValid(s, c);
        }

        @Override
        public void regenerate(State s, Command c, Parameter p) {
            Parameter ret = generateRandomParameter(s, c);
            p.value = ret.value;
        }

        @Override
        public boolean isEmpty(State s, Command c, Parameter p) {
            return ((Parameter) p.value).type.isEmpty(s, c, (Parameter) p.value);
        }

        @Override
        public void mutate(Command c, State s, Parameter p) {
            /**
             * 1. Call value.mutate
             * 2. Make sure it's still valid
             */

            Parameter subParameter = (Parameter) p.value;

            subParameter.mutate(s, c);
            assert subParameter.type instanceof ConcreteGenericTypeOne;


            List<Parameter> l = (List<Parameter>) subParameter.value;
            List<Parameter> targetSet = (List) (((Collection)configuration).stream().map(mapFunc).collect(Collectors.toList()));

            for (Parameter m : targetSet) {
                if (!l.contains(m)) {
                    l.add(m);
                }
            }
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
            // TODO: This is ugly... Might need to change design... Leave it for now.
            assert t instanceof GenericTypeOne;
            this.t = t;
            this.typesInTemplate.add(t1);
        }

        @Override
        public boolean isValid(State s, Command c, Parameter p) {
            // TODO: Impl
            assert false;
            return false;
        }

        @Override
        public void regenerate(State s, Command c, Parameter p) {
            // TODO: Impl
        }

        @Override
        public boolean isEmpty(State s, Command c, Parameter p) {
            return t.isEmpty(s, c, p, this.typesInTemplate);
        }

        @Override
        public void mutate(Command c, State s, Parameter p) {

        }
    }

    public static class ConcreteGenericTypeTwo extends ConcreteGenericType {

        public ConcreteGenericTypeTwo(GenericType t, ConcreteType t1, ConcreteType t2) {
            assert t instanceof GenericTypeTwo; // This is ugly...
            this.t = t;
            this.typesInTemplate.add(t1);
            this.typesInTemplate.add(t2);
        }

        @Override
        public boolean isValid(State s, Command c, Parameter p) {
            return false;
        }

        @Override
        public void regenerate(State s, Command c, Parameter p) {

        }

        @Override
        public boolean isEmpty(State s, Command c, Parameter p) {
            return false;
        }

        @Override
        public void mutate(Command c, State s, Parameter p) {

        }
    }
}
