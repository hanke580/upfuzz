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

        /**
         *                         p     <- input parameter p
         *                        / \
         *                       /   \
         *    this (type) ->   type value
         */
        public abstract boolean isValid(State s, Command c, Parameter p);
        public abstract void regenerate(State s, Command c, Parameter p);
        public abstract boolean isEmpty(State s, Command c, Parameter p);
        public abstract boolean mutate(State s, Command c, Parameter p);

    }

    public static abstract class GenericType extends ParameterType {
        // generic type cannot generate value without concrete types
        public abstract Parameter generateRandomParameter(State s, Command c, List<ConcreteType> types);
        public abstract String generateStringValue(Parameter p, List<ConcreteType> types); // Maybe this should be in Parameter class? It has the concrete type anyways.
        public abstract boolean isEmpty(State s, Command c, Parameter p, List<ConcreteType> types);
        public abstract boolean mutate(State s, Command c, Parameter p, List<ConcreteType> types);
    }

    /**
     * ConfigurableType uses its concrete type to generate values,
     * but it adds extra rules/constraints using configuration.
     * TODO: Need more reasoning to finish this.
     */
    public static abstract class ConfigurableType extends ConcreteType {
        public final ConcreteType t;
        public final FetchCollectionLambda configuration;
        public final Predicate predicate;
//        public final Function mapFunc;

        public ConfigurableType(ConcreteType t, FetchCollectionLambda configuration) {
            this.t = t;
            this.configuration = configuration;
            this.predicate = null;
//            this.mapFunc = mapFunc;
        }


        public ConfigurableType(ConcreteType t, FetchCollectionLambda configuration, Predicate predicate) {
            this.t = t;
            this.configuration = configuration;
            this.predicate = predicate;
//            this.mapFunc = mapFunc;
        }

        public void predicateCheck(State s, Command c) {
            if (predicate != null && predicate.operate(s, c) == false) {
                throw new CustomExceptions.PredicateUnSatisfyException(
                        "Predicate is not satisfied in this command",
                        null
                );
            }
        }

        /**
         * Problem:
         * There could be multiple constraints added in one parameters.
         * But when we want to store them, we might want directly get
         * access to the "real values" instead of those constraints
         * Now if take off all the constraints, we will have the pure values
         * Which will be
         * List<String, CassandraTypes>
         * If we only save the pure values, if will be very easy for use.
         * But this value will need to be updated if it's mutated.
         */

    }

    public static class NotInCollectionType <T,U> extends ConfigurableType {

        public final Function<T, U> mapFunc;

        // Example of mapFunc:
        // Parameter p -> p.value.left // p's ParameterType is Pair<TEXT, TEXTType>
        public NotInCollectionType(ConcreteType t, FetchCollectionLambda configuration, Function<T, U> mapFunc) {
            super(t, configuration);
            this.mapFunc = mapFunc;
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c) {
            Parameter ret = t.generateRandomParameter(s, c); // ((Pair<TEXTType, TYPEType>)ret.value).left
            // TODO: Don't compute this every time.
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
            Collection<T> targetCollection = configuration.operate(s, c);
            if (((Collection<T>) targetCollection).isEmpty())
                return true;
            if (mapFunc == null) {
                return !((Collection<T>) targetCollection).stream().collect(Collectors.toSet()).contains(p.value);
            } else {
                return !((Collection<T>) targetCollection).stream().map(mapFunc).collect(Collectors.toSet()).contains(p.value);
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
        public boolean mutate(State s, Command c, Parameter p) {
            return t.mutate(s, c, (Parameter) p.value);
        }
    }

    public static class NotEmpty extends ConfigurableType {

        public NotEmpty(ConcreteType t) {
            super(t, null);
        }

        public NotEmpty(ConcreteType t, Object configuration) {
            super(t, null);
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
        public boolean mutate(State s, Command c, Parameter p) {
            return t.mutate(s, c, (Parameter) p.value);
        }
    }

    public static class SubsetType <T,U> extends ConfigurableType {

        Function mapFunc;


        public SubsetType(ConcreteType t, FetchCollectionLambda configuration, Function mapFunc) { // change to concreteGenericType
            /**
             * In this case, the ConcreteType should be List<Pair<xxx>>.
             * The set we select from will be the target set targetSet. Let's suppose it's also List<Pair<TEXT, TYPEType>>
             * - we rewrite the generateRandomParameter() in this concreteGenericType
             * - Instead of calling t.generateValue function, it should now construct values by select from targetSet.
             * - Do it by anonymous class extends List, and we write the subset generateRandomParameter() for it.
             *
             * TODO: This type can actually be removed. It can be used for checking.
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

            Object targetCollection = configuration.operate(s, c);

            if (mapFunc != null) {
                targetCollection = ((Collection<T>) targetCollection).stream().map(mapFunc).collect(Collectors.toList());
            }

            /**
             * Pick a subset from the configuration, it will also be a list of parameters
             * Return new Parameter(SubsetType, value)
             */

            // TODO: Make all the collection contain the parameter

            List<Object> targetSet = new ArrayList<Object>((Collection<Object>) targetCollection);
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
            List<Parameter> valueList = (List<Parameter>) p.value;
            Object targetCollection = configuration.operate(s, c);
            if (mapFunc != null) {
                targetCollection = ((Collection<T>) targetCollection).stream().map(mapFunc).collect(Collectors.toList());
            }
            List<Parameter> targetList = (List<Parameter>) targetCollection;

            for (int i = 0; i < valueList.size(); i++) {
                if (!targetList.contains(valueList.get(i))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void regenerate(State s, Command c, Parameter p) {
            Parameter ret = generateRandomParameter(s, c);
            p.value = ret.value;
        }

        @Override
        public boolean isEmpty(State s, Command c, Parameter p) {
            return  ((List<Object>) p.value).isEmpty();
        }

        @Override
        public boolean mutate(State s, Command c, Parameter p) {
            Parameter ret = generateRandomParameter(s, c);
            p.value = ret.value;
            return true;
        }
    }

    /**
     * TODO: StreamMapType might not be a configurable type.
     */
    public static class StreamMapType extends ConfigurableType {

        Function mapFunc;

        public StreamMapType(ConcreteType t, FetchCollectionLambda configuration, Function mapFunc) { // change to concreteGenericType
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
            Object targetCollection = configuration.operate(s, c);
            Object tmpCollection;

            if (mapFunc != null) {
                tmpCollection = ((Collection) targetCollection).stream().map(mapFunc).collect(Collectors.toList());
            } else {
                tmpCollection = (Collection) targetCollection;
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
        public boolean mutate(State s, Command c, Parameter p) {
            // TODO: Impl
            return false;
        }
    }

    public static class OptionalType extends ConfigurableType {

        boolean isEmpty;

        public OptionalType(ConcreteType t, FetchCollectionLambda configuration) {
            super(t, null);
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c) {
            Random rand = new Random();
            isEmpty = rand.nextBoolean();
            return new Parameter(this, t.generateRandomParameter(s, c));
        }

        @Override
        public String generateStringValue(Parameter p) {
            return isEmpty? "": ((Parameter) p.value).toString();
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
        public boolean mutate(State s, Command c, Parameter p) {
            /**
             * There should be two choices.
             * 1. Mutate current state.
             * 2. Mutate the subvalue.
             * Since the optional parameters are likely to be a constant,
             * we only mutate current isEmpty for now.
             */
            assert p.type instanceof OptionalType;
            ((OptionalType) p.type).isEmpty = !((OptionalType) p.type).isEmpty;
            return true;
        }
    }

    public static class InCollectionType extends ConfigurableType {
        /**
         * For conflict options, not test yet.
         */

        public int idx;
        public final Function mapFunc;

        public InCollectionType(ConcreteType t, FetchCollectionLambda configuration, Function mapFunc) {
            super(t, configuration);
            this.mapFunc = mapFunc;
        }

        public InCollectionType(ConcreteType t, FetchCollectionLambda configuration, Function mapFunc, Predicate predicate) {
            super(t, configuration, predicate);
            this.mapFunc = mapFunc;
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c) {
            predicateCheck(s, c);

            // Pick one parameter from the collection
            Object targetCollection = configuration.operate(s, c);

            if (((Collection) targetCollection).isEmpty() == true) {
                throw new CustomExceptions.EmptyCollectionException(
                        "InCollection Type got empty Collection",
                        null
                );
            }
            Random rand = new Random();

            List l;

            if (mapFunc == null) {
                l = (List) (((Collection) targetCollection).stream().collect(Collectors.toList()));
            } else {
                l = (List) (((Collection) targetCollection).stream().map(mapFunc).collect(Collectors.toList()));
            }
            idx = rand.nextInt(l.size());

            if (l.get(idx) instanceof Parameter) {
                return new Parameter(this, l.get(idx));
            } else {
                assert t != null;
                Parameter ret = new Parameter(t, l.get(idx));
                return new Parameter(this, ret);
            }

        }

        @Override
        public String generateStringValue(Parameter p) {
            return p.value.toString();
        }

        @Override
        public boolean isValid(State s, Command c, Parameter p) {
            predicateCheck(s, c);

            assert p.value instanceof Parameter;
            List l;
            Object targetCollection = configuration.operate(s, c);

            if (mapFunc == null) {
                l = (List) (((Collection) targetCollection).stream().collect(Collectors.toList()));
            } else {
                l = (List) (((Collection) targetCollection).stream().map(mapFunc).collect(Collectors.toList()));
            }
            if (l.contains(p.getValue())) {
                return ((Parameter) p.value).isValid(s, c);
            } else {
                return false;
            }
        }

        @Override
        public void regenerate(State s, Command c, Parameter p) {
            p.value = generateRandomParameter(s, c);
        }

        @Override
        public boolean isEmpty(State s, Command c, Parameter p) {
            return ((Parameter) p.value).type.isEmpty(s, c, (Parameter) p.value);
        }

        @Override
        public boolean mutate(State s, Command c, Parameter p) {
            /**
             * Repick one from the set.
             * TODO: Make sure it's not the same one as before?
             */
            Parameter ret = generateRandomParameter(s, c);
            p.value = ret.value;
            return true;
        }
    }

    public static class Type2ValueType extends ConfigurableType {

        Function mapFunc;

        public Type2ValueType(ConcreteType t, FetchCollectionLambda configuration, Function mapFunc) {
            super(t, configuration);
            this.mapFunc = mapFunc;
            // TODO: Make sure that configuration must be List<ConcreteTypes>
            /**
             * If the value is not corrected, regenerate or add some minor
             * changes
             * Do we need a notEmpty function?
             */
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c) {

            Collection targetCollection = configuration.operate(s, c);

            assert targetCollection instanceof List;

            List<Parameter> l;
            if (mapFunc != null) {
                l = (List<Parameter>) ((Collection) targetCollection).stream().map(mapFunc).collect(Collectors.toList());
            } else {
                l = (List<Parameter>) targetCollection;
            }

            List<Parameter> ret = new LinkedList<>();

            for (Parameter p : l) {
                assert p.getValue() instanceof ConcreteType;
                ConcreteType concreteType = (ConcreteType) p.getValue();
                ret.add(concreteType.generateRandomParameter(s, c));
            }

            return new Parameter(this, ret);
        }

        @Override
        public String generateStringValue(Parameter p) {
            List<Parameter> l = (List<Parameter>) p.value;
            StringBuilder sb = new StringBuilder();
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
            /**
             * TODO: Check each slot to see whether it's still valid.
             * Check whether the list size is change
             */

            Collection targetCollection = configuration.operate(s, c);

            assert targetCollection instanceof List;
            assert p.value instanceof List;

            /**
             *          p
             *         / \
             *        /   \
             *   TYPEType TEXTType
             */
            List<Parameter> typeList;
            if (mapFunc != null) {
                typeList = (List<Parameter>) ((Collection) targetCollection).stream().map(mapFunc).collect(Collectors.toList());
            } else {
                typeList = (List<Parameter>) targetCollection;
            }


            /**
             *          p
             *         / \
             *        /   \
             *   TEXTType "HelloWorld"
             */
            List<Parameter> valueList = (List<Parameter>) p.value;
            if (typeList.size() != valueList.size()) return false;

            for (int i = 0; i < typeList.size(); i++) {
                // TODO: Do we need to make the type comparable?
                // Same t, predicate, class?
                if (!typeList.get(i).getValue().equals(valueList.get(i).type)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void regenerate(State s, Command c, Parameter p) {
            Parameter ret = generateRandomParameter(s, c);
            p.value = ret.value;
        }

        @Override
        public boolean isEmpty(State s, Command c, Parameter p) {
            /**
             * Return whether the current list is empty or not
             */
            return ((Collection) configuration).isEmpty();
        }

        @Override
        public boolean mutate(State s, Command c, Parameter p) {
            // TODO: Multiple level mutation!
            // Now only regenerate everything
            Parameter ret = generateRandomParameter(s, c);
            p.value = ret.value;
            return true;
        }
    }


    public static class SuperSetType extends ConfigurableType {
        /**
         * For conflict options, not test yet.
         */
        public int idx;
        public final Function mapFunc;


        public SuperSetType(ConcreteType t, FetchCollectionLambda configuration, Function mapFunc) {
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
//            assert p.type instanceof ConcreteGenericTypeOne;
            List<Parameter> l = (List<Parameter>) p.value;

            Collection targetCollection = configuration.operate(s, c);

            List<Parameter> targetSet;
            if (mapFunc != null) {
                targetSet = (List) (((Collection) targetCollection).stream().map(mapFunc).collect(Collectors.toList()));
            } else {
                targetSet = (List) (((Collection) targetCollection));
            }

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
        public boolean mutate(State s, Command c, Parameter p) {
            /**
             * 1. Call value.mutate
             * 2. Make sure it's still valid
             */
            p.value = generateRandomParameter(s, c).value;
            return true;
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
        public boolean mutate(State s, Command c, Parameter p) {
            return t.mutate(s, c, p, typesInTemplate);
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
        public boolean mutate(State s, Command c, Parameter p) {
            // TODO: impl
            return false;
        }
    }
}
