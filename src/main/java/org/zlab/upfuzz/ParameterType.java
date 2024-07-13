package org.zlab.upfuzz;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.cassandra.CassandraCommand;
import org.zlab.upfuzz.utils.INTType;
import org.zlab.upfuzz.utils.STRINGType;

/**
 * How a parameter can be generated is only defined in its type.
 * If you want special rules for a parameter, you need to implement a type class
 * for it.
 */
public abstract class ParameterType implements Serializable {
    static Logger logger = LogManager.getLogger(Command.class);
    public static final Random rand = new Random();

    public static abstract class ConcreteType extends ParameterType {
        /**
         * generateRandomParameter() follows rules to generate a parameter with
         * a random value.
         * @param s // current state
         * @param c // current command
         *  these rules might use the state and other parameters in the current
         * command.
         */
        public abstract Parameter generateRandomParameter(State s, Command c,
                Object init);

        public abstract Parameter generateRandomParameter(State s, Command c);

        public abstract String generateStringValue(Parameter p); // Maybe this
                                                                 // should be in
                                                                 // Parameter
                                                                 // class? It
                                                                 // has the
                                                                 // concrete
                                                                 // type
                                                                 // anyways.

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

        @Override
        protected Object clone() {
            Parameter clone = null;
            try {
                clone = (Parameter) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
            return clone;
        }
    }

    public static abstract class BasicConcreteType extends ConcreteType {
        // Basic Concrete Type can generate parameters without
        // ConcreteType field. Like String, Int...
        // They can also contain a Type Pool

        public abstract boolean addToPool(Object val);

        public static void clearPool() {
            STRINGType.clearPool();
            INTType.clearPool();
        }
    }

    public static abstract class GenericType extends ParameterType {
        // generic type cannot generate value without concrete types
        public abstract Parameter generateRandomParameter(State s, Command c,
                List<ConcreteType> types);

        public abstract Parameter generateRandomParameter(
                State s, Command c, List<ConcreteType> types, Object init);

        public abstract String generateStringValue(Parameter p,
                List<ConcreteType> types);
        // Maybe this should be in Parameter
        // class? It has the concrete type
        // anyways.

        public abstract boolean isEmpty(State s, Command c, Parameter p,
                List<ConcreteType> types);

        public abstract boolean mutate(State s, Command c, Parameter p,
                List<ConcreteType> types);

        public abstract boolean isValid(State s, Command c, Parameter p,
                List<ConcreteType> types);
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
        // public final Function mapFunc;

        public ConfigurableType(ConcreteType t,
                FetchCollectionLambda configuration) {
            this.t = t;
            this.configuration = configuration;
            this.predicate = null;
            // this.mapFunc = mapFunc;
        }

        public ConfigurableType(ConcreteType t,
                FetchCollectionLambda configuration,
                Predicate predicate) {
            this.t = t;
            this.configuration = configuration;
            this.predicate = predicate;
            // this.mapFunc = mapFunc;
        }

        public void predicateCheck(State s, Command c) {
            if (predicate != null && predicate.operate(s, c) == false) {
                throw new CustomExceptions.PredicateUnSatisfyException(
                        "Predicate is not satisfied in this command", null);
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

    public static class NotInCollectionType extends ConfigurableType {

        public final SerializableFunction mapFunc;

        // Example of mapFunc:
        // Parameter p -> p.value.left // p's ParameterType is Pair<TEXT,
        // TEXTType>
        public NotInCollectionType(ConcreteType t,
                FetchCollectionLambda configuration,
                SerializableFunction mapFunc) {
            super(t, configuration);
            this.mapFunc = mapFunc;
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c,
                Object init) {
            if (init == null) {
                return generateRandomParameter(s, c);
            }
            Parameter ret = t.generateRandomParameter(s, c, init);
            return new Parameter(this, ret);
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c) {
            Parameter ret = t.generateRandomParameter(s, c); // ((Pair<TEXTType,
                                                             // TYPEType>)ret.value).left
                                                             // TODO: Don't
                                                             // compute this
                                                             // every time.
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
            if (s == null)
                return true;
            Collection targetCollection = configuration.operate(s, c);
            if (((Collection) targetCollection).isEmpty())
                return true;
            List<Parameter> targetList;
            if (mapFunc == null) {
                targetList = (List) (targetCollection).stream()
                        .collect(Collectors.toList());
            } else {
                targetList = (List) (targetCollection).stream().map(mapFunc)
                        .collect(Collectors.toList());
            }

            List<String> targetStringList = new LinkedList<>();
            for (Parameter parameter : targetList) {
                targetStringList.add(parameter.toString());
            }

            return !targetStringList.contains(p.value.toString());
        }

        @Override
        public void regenerate(State s, Command c, Parameter p) {
            Parameter ret = generateRandomParameter(s, c); // ((Pair<TEXTType,
                                                           // TYPEType>)ret.value).left
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
        public Parameter generateRandomParameter(State s, Command c,
                Object init) {
            if (init == null) {
                return generateRandomParameter(s, c);
            }
            Parameter ret = t.generateRandomParameter(s, c, init);
            return new Parameter(this, ret);
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
            if (t.isEmpty(s, c, (Parameter) p.value) == true) {
                return false;
            } else {
                return ((Parameter) p.value).isValid(s, c);
            }
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

    public static class NotStartWithNumber extends ConfigurableType {

        public NotStartWithNumber(ConcreteType t) {
            super(t, null);
        }

        public NotStartWithNumber(ConcreteType t, Object configuration) {
            super(t, null);
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c,
                Object init) {
            if (init == null) {
                return generateRandomParameter(s, c);
            }
            Parameter ret = t.generateRandomParameter(s, c, init);
            return new Parameter(this, ret);
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c) {
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
            assert p.getValue() instanceof String;
            String value = (String) p.getValue();
            return !Character.isDigit(value.charAt(0));
        }

        @Override
        public void regenerate(State s, Command c, Parameter p) {
            Parameter ret = generateRandomParameter(s, c);
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

    public static class InCollectionType extends ConfigurableType {
        /**
         * For conflict options, not test yet.
         */

        @Override
        protected Object clone() {
            InCollectionType clone;
            clone = (InCollectionType) super.clone();
            return clone;
        }

        public int idx;
        public final SerializableFunction mapFunc;

        public InCollectionType(ConcreteType t,
                FetchCollectionLambda configuration,
                SerializableFunction mapFunc) {
            super(t, configuration);
            this.mapFunc = mapFunc;
        }

        public InCollectionType(ConcreteType t,
                FetchCollectionLambda configuration,
                SerializableFunction mapFunc,
                Predicate predicate) {
            super(t, configuration, predicate);
            this.mapFunc = mapFunc;
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c,
                Object init) {
            /**
             * InCollectionType
             * - List<>   {col1, col2, co3}
             *
             * return col2
             *
             * Add init value col2 (initValue)
             * for (p : Collection)
             *      if (initValue.equals(p.toString))
             *          return p;
             *
             *
             *
             */
            if (init == null) {
                return generateRandomParameter(s, c);
            }
            predicateCheck(s, c);

            // Pick one parameter from the collection
            Object targetCollection = configuration.operate(s, c);

            if (((Collection) targetCollection).isEmpty()) {
                throw new CustomExceptions.EmptyCollectionException(
                        "InCollection Type got empty Collection", null);
            }

            List l;
            if (mapFunc == null) {
                l = (List) (((Collection) targetCollection)
                        .stream()
                        .collect(Collectors.toList()));
            } else {
                l = (List) (((Collection) targetCollection)
                        .stream()
                        .map(mapFunc)
                        .collect(Collectors.toList()));
            }

            assert init instanceof String;
            String pString = (String) init;

            int idx = 0;
            for (; idx < l.size(); idx++) {
                if (pString.equals(l.get(idx).toString())) {
                    break;
                }
            }
            assert idx != l.size();

            if (l.get(idx) instanceof Parameter) {
                return new Parameter(this, l.get(idx));
            } else {
                assert t != null;
                Parameter ret = new Parameter(t, l.get(idx));
                return new Parameter(this, ret);
            }
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c) {
            predicateCheck(s, c);

            // Pick one parameter from the collection
            Object targetCollection = configuration.operate(s, c);

            if (((Collection) targetCollection).isEmpty() == true) {

                // FIXME: Enable this to check the buggy mutation
                // logger.info("command: " + c);

                // for (StackTraceElement ste : Thread.currentThread()
                // .getStackTrace()) {
                // System.out.println(ste.toString());
                // }
                // logger.info("\n");

                throw new CustomExceptions.EmptyCollectionException(
                        "InCollection Type got empty Collection", null);
            }
            Random rand = new Random();

            List l;

            if (mapFunc == null) {
                l = (List) (((Collection) targetCollection)
                        .stream()
                        .collect(Collectors.toList()));
            } else {
                l = (List) (((Collection) targetCollection)
                        .stream()
                        .map(mapFunc)
                        .collect(Collectors.toList()));
            }
            idx = rand.nextInt(l.size());

            if (l.get(idx) instanceof Parameter) {
                return new Parameter(this,
                        SerializationUtils.clone((Parameter) l.get(idx)));
            } else {
                // assert t != null;
                // Parameter ret = new Parameter(t, l.get(idx));
                // return new Parameter(this, ret);
                logger.error(
                        "InCollectionType: The collection should contain Parameter, however it got "
                                + l.get(idx).getClass() + ", value = "
                                + l.get(idx));
                throw new RuntimeException(
                        "InCollectionType: The collection should contain Parameter");
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
                l = (List) (((Collection) targetCollection)
                        .stream()
                        .collect(Collectors.toList()));
            } else {
                l = (List) (((Collection) targetCollection)
                        .stream()
                        .map(mapFunc)
                        .collect(Collectors.toList()));
            }
            /**
             * Since we are basically generating the string.
             * When comparing the parameter, we only care about the value.
             * Then, what if we directly compare the toString() between
             * two parameter?
             */
            if (l.contains(p) || l.contains(p.getValue())) {
                // l could be List<Parameter> or List<Object>...
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
            return ((Parameter) p.value).type.isEmpty(s, c,
                    (Parameter) p.value);
        }

        @Override
        public boolean mutate(State s, Command c, Parameter p) {
            /**
             * Repick one from the set.
             * If there is only one in the target collection, it means
             * we cannot pick a different item.
             * - Return false
             */
            predicateCheck(s, c);

            // Pick one parameter from the collection
            Object targetCollection = configuration.operate(s, c);
            Random rand = new Random();
            List l;
            if (mapFunc == null) {
                l = (List) (((Collection) targetCollection)
                        .stream()
                        .collect(Collectors.toList()));
            } else {
                l = (List) (((Collection) targetCollection)
                        .stream()
                        .map(mapFunc)
                        .collect(Collectors.toList()));
            }
            if (l.isEmpty()) {
                // Throw an exception, since the input sequence is already not
                // correct!
                throw new RuntimeException(
                        "[InCollectionType] Mutation: The input collection is already not valid"
                                + "Run check() before the mutation!");
            }

            if (l.size() == 1)
                return false; // Cannot mutate

            List<String> collectionStringList = new LinkedList<>();
            for (Object item : l) {
                collectionStringList.add(item.toString());
            }
            // pick one that is not the same as the one before
            List<Integer> idxList = new LinkedList<>();
            for (int i = 0; i < l.size(); i++) {
                idxList.add(i);
            }
            if (collectionStringList.contains(p.value.toString())) {
                idxList.remove(
                        collectionStringList.indexOf(p.value.toString()));
            }
            int idx = rand.nextInt(idxList.size());
            if (l.get(idx) instanceof Parameter) {
                p.value = l.get(idx);
            } else {
                assert t != null;
                p.value = new Parameter(t, l.get(idx));
            }
            return true;
        }
    }

    public static class SubsetType<T, U> extends ConfigurableType {

        public SerializableFunction<T, U> mapFunc;

        public SubsetType(
                ConcreteType t, FetchCollectionLambda configuration,
                SerializableFunction<T, U> mapFunc) { // change to
                                                      // concreteGenericType
            /**
             * In this case, the ConcreteType should be List<Pair<xxx>>.
             * The set we select from will be the target set targetSet. Let's
             * suppose it's also List<Pair<TEXT, TYPEType>>
             * - we rewrite the generateRandomParameter() in this
             * concreteGenericType
             * - Instead of calling t.generateValue function, it should now
             * construct values by select from targetSet.
             * - Do it by anonymous class extends List, and we write the subset
             * generateRandomParameter() for it.
             *
             * TODO: This type can actually be removed. It can be used for
             * checking.
             */
            super(t, configuration);
            this.mapFunc = mapFunc;
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c,
                Object init) {
            if (init == null) {
                return generateRandomParameter(s, c);
            }
            assert init instanceof List;
            List<String> l = (List<String>) init;

            Object targetCollection = configuration.operate(s, c);

            if (mapFunc != null) {
                targetCollection = ((Collection<T>) targetCollection)
                        .stream()
                        .map(mapFunc)
                        .collect(Collectors.toList());
            }

            // TODO: Make all the collection contain the parameter
            List<Parameter> targetSet = new ArrayList<Parameter>(
                    (Collection<Parameter>) targetCollection);
            List<Parameter> value = new ArrayList<>();

            List<String> targetSetString = new LinkedList<>();
            for (Parameter p : targetSet)
                targetSetString.add(p.toString());

            if (targetSet.size() > 0) {
                for (int i = 0; i < l.size(); i++) {
                    int idx = targetSetString.indexOf(l.get(i));
                    assert idx != -1;
                    value.add(targetSet.get(idx));
                }
            }

            return new Parameter(this, value);
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c) {
            /**
             * Current t should be concrete generic type List<xxx>
             * - Select from collection set
             */

            Object targetCollection = configuration.operate(s, c);

            if (mapFunc != null) {
                targetCollection = ((Collection<T>) targetCollection)
                        .stream()
                        .map(mapFunc)
                        .collect(Collectors.toList());
            }

            /**
             * Pick a subset from the configuration, it will also be a list of
             * parameters Return new Parameter(SubsetType, value)
             */

            // TODO: Make all the collection contain the parameter

            if (targetCollection == null) {
                return new Parameter(this, new ArrayList<>());
            }

            // Must be parameter type
            List<Parameter> targetSet = new ArrayList<>(
                    (Collection<Parameter>) targetCollection);
            List<Object> value = new ArrayList<>();

            if (targetSet.size() > 0) {
                Random rand = new Random();
                int setSize = rand.nextInt(targetSet.size() + 1); // specified
                                                                  // by user
                List<Integer> indexArray = new ArrayList<>();
                for (int i = 0; i < targetSet.size(); i++) {
                    indexArray.add(i);
                }
                Collections.shuffle(indexArray);

                for (int i = 0; i < setSize; i++) {
                    value.add(SerializationUtils
                            .clone(targetSet.get(indexArray.get(i))));
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
                    sb.append(",");
                }
            }
            return sb.toString();
        }

        @Override
        public boolean isValid(State s, Command c, Parameter p) {
            List<Parameter> valueList = (List<Parameter>) p.value;

            Object targetCollection = configuration.operate(s, c);
            if (mapFunc != null) {
                targetCollection = ((Collection<T>) targetCollection)
                        .stream()
                        .map(mapFunc)
                        .collect(Collectors.toList());
            }
            List<Parameter> targetList = (List<Parameter>) targetCollection;
            List<String> targetStrings = new LinkedList<>();
            for (Parameter m : targetList) {
                targetStrings.add(m.toString());
            }

            for (Parameter m : valueList) {
                if (!targetStrings.contains(m.toString()))
                    return false;
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
            return ((List<Object>) p.value).isEmpty();
        }

        @Override
        public boolean mutate(State s, Command c, Parameter p) {
            Parameter ret = generateRandomParameter(s, c);
            List<Object> retValue = (List<Object>) ret.value;
            Set<String> retList = new HashSet<>();
            for (Object o : retValue) {
                retList.add(o.toString());
            }

            List<Object> pValue = (List<Object>) p.value;
            Set<String> pList = new HashSet<>();
            for (Object o : pValue) {
                retList.add(o.toString());
            }

            boolean equal;
            if (pList.size() == retList.size()) {
                equal = true;
                for (String str : pList) {
                    if (!retList.contains(str)) {
                        equal = false;
                        break;
                    }
                }
            } else {
                equal = false;
            }

            if (!equal) {
                p.value = ret.value;
                return true;
            } else {
                return false;
            }
        }
    }

    public static class FrontSubsetType<T, U> extends SubsetType {

        public FrontSubsetType(ConcreteType t,
                FetchCollectionLambda configuration,
                SerializableFunction mapFunc) {
            super(t, configuration, mapFunc);
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c) {
            /**
             * Current t should be concrete generic type List<xxx>
             * - Select from collection set
             */

            Object targetCollection = configuration.operate(s, c);

            if (mapFunc != null) {
                targetCollection = ((Collection<T>) targetCollection)
                        .stream()
                        .map(mapFunc)
                        .collect(Collectors.toList());
            }

            List<Parameter> targetSet = new ArrayList<Parameter>(
                    (Collection<Parameter>) targetCollection);
            List<Object> value = new ArrayList<>();

            if (targetSet.size() > 0) {
                Random rand = new Random();
                int setSize = rand.nextInt(targetSet.size() + 1); // specified
                                                                  // by user
                List<Integer> indexArray = new ArrayList<>();
                for (int i = 0; i < targetSet.size(); i++) {
                    indexArray.add(i);
                }
                // Collections.shuffle(indexArray);

                for (int i = 0; i < setSize; i++) {
                    value.add(SerializationUtils
                            .clone(targetSet.get(indexArray.get(i)))); // The
                    // targetSet
                    // should also
                    // store
                    // Parameter
                }
            }

            return new Parameter(this, value);
        }

        @Override
        public boolean isValid(State s, Command c, Parameter p) {

            Object targetCollection = configuration.operate(s, c);
            if (mapFunc != null) {
                targetCollection = ((Collection<T>) targetCollection)
                        .stream()
                        .map(mapFunc)
                        .collect(Collectors.toList());
            }
            List<Parameter> targetList = (List<Parameter>) targetCollection;
            List<String> targetStrings = new LinkedList<>();
            for (Parameter m : targetList) {
                targetStrings.add(m.toString());
            }

            List<Parameter> valueList = (List<Parameter>) p.value;

            if (valueList.size() >= targetList.size())
                return false;

            for (int i = 0; i < valueList.size(); i++) {
                if (!targetStrings.get(i).equals(valueList.get(i).toString())) {
                    return false;
                }
            }

            return true;
        }
    }

    public static class SuperSetType extends ConfigurableType {
        /**
         * For conflict options, not test yet.
         */
        public int idx;
        public final SerializableFunction mapFunc;

        public SuperSetType(ConcreteType t, FetchCollectionLambda configuration,
                SerializableFunction mapFunc) {
            super(t, configuration);
            this.mapFunc = mapFunc;
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c,
                Object init) {
            if (init == null) {
                return generateRandomParameter(s, c);
            }
            assert init instanceof List;

            Parameter p = t.generateRandomParameter(s, c, init);
            return new Parameter(this, p);
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c) {
            /**
             * use ConcreteType t to generate a parameter, it should be a
             * concreteGenericType. Check whether this parameter contains
             * everything in the collection. If not, add the rest to the
             * collection.
             */
            Parameter p = t.generateRandomParameter(s, c);
            // assert p.type instanceof ConcreteGenericTypeOne;
            List<String> curStrings = new LinkedList<>();
            List<Parameter> l = (List<Parameter>) p.value;
            for (Parameter m : l) {
                curStrings.add(m.toString());
            }

            Collection targetCollection = configuration.operate(s, c);
            List<Parameter> targetSet;
            if (mapFunc != null) {
                targetSet = (List) (((Collection) targetCollection)
                        .stream()
                        .map(mapFunc)
                        .collect(Collectors.toList()));
            } else {
                targetSet = (List) (((Collection) targetCollection));
            }

            for (Parameter m : targetSet) {
                if (!curStrings.contains(m.toString())) {
                    l.add(SerializationUtils.clone(m));
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
            // Make sure it satisfies the super set relation
            List<String> curStrings = new LinkedList<>();
            List<Parameter> l = (List<Parameter>) p.getValue();
            for (Parameter m : l) {
                curStrings.add(m.toString());
            }

            Collection targetCollection = configuration.operate(s, c);
            List<Parameter> targetSet;
            if (mapFunc != null) {
                targetSet = (List) (((Collection) targetCollection)
                        .stream()
                        .map(mapFunc)
                        .collect(Collectors.toList()));
            } else {
                targetSet = (List) (((Collection) targetCollection));
            }

            for (Parameter m : targetSet) {
                if (!curStrings.contains(m.toString())) {
                    return false;
                }
            }
            return ((Parameter) p.value).isValid(s, c);
        }

        @Override
        public void regenerate(State s, Command c, Parameter p) {
            Parameter ret = generateRandomParameter(s, c);
            p.value = ret.value;
        }

        @Override
        public boolean isEmpty(State s, Command c, Parameter p) {
            return ((Parameter) p.value).type.isEmpty(s, c,
                    (Parameter) p.value);
        }

        @Override
        public boolean mutate(State s, Command c, Parameter p) {
            /**
             * 1. Call value.mutate
             * 2. Make sure it's still valid
             */
            // TODO: Call inside mutate function
            p.value = generateRandomParameter(s, c).value;
            return true;
        }
    }

    /**
     * TODO: StreamMapType might not be a configurable type.
     */
    public static class StreamMapType extends ConfigurableType {

        SerializableFunction mapFunc;

        public StreamMapType(
                ConcreteType t, FetchCollectionLambda configuration,
                SerializableFunction mapFunc) { // change to concreteGenericType
            /**
             * In this case, the ConcreteType should be List<Pair<xxx>>.
             * The set we select from will be the target set targetSet. Let's
             * suppose it's also List<Pair<TEXT, TYPEType>>
             * - we rewrite the generateRandomParameter() in this
             * concreteGenericType
             * - Instead of calling t.generateValue function, it should now
             * construct values by select from targetSet.
             * - Do it by anonymous class extends List, and we write the subset
             * generateRandomParameter() for it.
             */
            super(t, configuration);
            this.mapFunc = mapFunc;
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c,
                Object init) {
            // This type doesn't need initial value.
            // Since it's manipulating other values, and the map function is
            // fixed.
            return null;
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
                tmpCollection = ((Collection) targetCollection)
                        .stream()
                        .map(mapFunc)
                        .collect(Collectors.toList());
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

    public static class LessLikelyMutateType extends ConfigurableType {
        final double probability;

        public LessLikelyMutateType(ConcreteType t, double probability) {
            super(t, null);
            this.probability = probability;
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c,
                Object init) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c) {
            return new Parameter(this, t.generateRandomParameter(s, c));
        }

        @Override
        public String generateStringValue(Parameter p) {
            return ((Parameter) p.value).toString();
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
            return t.isEmpty(s, c, p);
        }

        @Override
        public boolean mutate(State s, Command c, Parameter p) {
            if (rand.nextDouble() < probability) {
                return t.mutate(s, c, (Parameter) p.value);
            } else {
                return false;
            }
        }
    }

    public static class OptionalType extends ConfigurableType {

        boolean isEmpty;

        public OptionalType(ConcreteType t,
                FetchCollectionLambda configuration) {
            super(t, null);
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c,
                Object init) {
            if (init == null) {
                return generateRandomParameter(s, c);
            }
            assert init instanceof Boolean;
            isEmpty = (Boolean) init;
            return new Parameter(this, t.generateRandomParameter(s, c));
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c) {
            Random rand = new Random();
            isEmpty = rand.nextBoolean();
            return new Parameter(this, t.generateRandomParameter(s, c));
        }

        @Override
        public String generateStringValue(Parameter p) {
            return isEmpty ? "" : ((Parameter) p.value).toString();
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
            if (isEmpty)
                return true;
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

    public static class Type2ValueType extends ConfigurableType {

        SerializableFunction mapFunc;

        public Type2ValueType(ConcreteType t,
                FetchCollectionLambda configuration,
                SerializableFunction mapFunc) {
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
        public Parameter generateRandomParameter(State s, Command c,
                Object init) {
            if (init == null) {
                return generateRandomParameter(s, c);
            }
            Collection targetCollection = configuration.operate(s, c);

            assert targetCollection instanceof List;

            List<Parameter> l;
            if (mapFunc != null) {
                l = (List<Parameter>) ((Collection) targetCollection)
                        .stream()
                        .map(mapFunc)
                        .collect(Collectors.toList());
            } else {
                l = (List<Parameter>) targetCollection;
            }

            List<Parameter> ret = new LinkedList<>();

            assert init instanceof List;
            List<Object> initValues = (List<Object>) init;
            assert initValues.size() == l.size();

            for (int i = 0; i < l.size(); i++) {
                assert l.get(i).getValue() instanceof ConcreteType;
                ConcreteType concreteType = (ConcreteType) l.get(i).getValue();
                ret.add(concreteType.generateRandomParameter(
                        s, c, initValues.get(i)));
            }
            return new Parameter(this, ret);
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c) {

            Collection targetCollection = configuration.operate(s, c);

            assert targetCollection instanceof List;

            List<Parameter> l;
            if (mapFunc != null) {
                l = (List<Parameter>) ((Collection) targetCollection)
                        .stream()
                        .map(mapFunc)
                        .collect(Collectors.toList());
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
                    sb.append(",");
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
                typeList = (List<Parameter>) ((Collection) targetCollection)
                        .stream()
                        .map(mapFunc)
                        .collect(Collectors.toList());
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
            if (typeList.size() != valueList.size())
                return false;

            /**
             * Make sure each concrete type have overrided the toString()
             * Now we only add text, int, string
             * TODO: But for the List Type comparing, we also need to add that.
             */
            for (int i = 0; i < typeList.size(); i++) {
                if (!typeList.get(i).getValue().toString().equals(
                        valueList.get(i).type.toString())) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void regenerate(State s, Command c, Parameter p) {
            // TODO: Make minor change

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
            // TODO: Multiple level mutation
            // Now only regenerate everything
            Random rand = new Random();

            int i = rand.nextInt(1);
            switch (i) {
            case 0:
                // Pick one
                List<Parameter> values = (List<Parameter>) p.value;
                if (values.isEmpty()) {
                    logger.info("there's no values for p currently" +
                            " Problem Command: " + c.toString());
                    return false;
                } else {
                    int mutateIdx = rand.nextInt(values.size());
                    if (CassandraCommand.DEBUG) {
                        mutateIdx = 3;
                        System.out.println("\t[Type2Value] Mutate Idx = " +
                                mutateIdx);
                    }
                    values.get(mutateIdx).mutate(s, c);
                }
            }
            // Parameter ret = generateRandomParameter(s, c);
            // p.value = ret.value;
            return true;
        }
    }

    public static abstract class GenericTypeOne extends GenericType {
    }

    public static abstract class GenericTypeTwo extends GenericType {
    }

    // ConcreteGenericType: List<Pair<Text, Type>>
    // ConcreteGenericType: (Pair<Text, Type>)
    public abstract static class ConcreteGenericType extends ConcreteType {
        // Support a variable number of templates. E.g., Pair<K, V>.
        public GenericType t;
        public List<ConcreteType> typesInTemplate = new ArrayList<>();

        @Override
        public Parameter generateRandomParameter(State s, Command c) {
            return t.generateRandomParameter(s, c, typesInTemplate);
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c,
                Object init) {
            if (init == null) {
                return generateRandomParameter(s, c);
            }
            return t.generateRandomParameter(s, c, typesInTemplate, init);
        }

        @Override
        public void regenerate(State s, Command c, Parameter p) {
            Parameter tmp = t.generateRandomParameter(s, c, typesInTemplate);
            p.value = tmp.value;
        }

        @Override
        public boolean isEmpty(State s, Command c, Parameter p) {
            return t.isEmpty(s, c, p, this.typesInTemplate);
        }

        @Override
        public boolean isValid(State s, Command c, Parameter p) {
            return t.isValid(s, c, p, typesInTemplate);
        }

        @Override
        public boolean mutate(State s, Command c, Parameter p) {
            return t.mutate(s, c, p, typesInTemplate);
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ConcreteGenericTypeOne))
                return false;
            ConcreteGenericTypeOne other = (ConcreteGenericTypeOne) obj;
            return Objects.equals(this.t, other.t)
                    // TODO: Check: Do we need to compare every item in the
                    // list?
                    && Objects.equals(this.typesInTemplate,
                            other.typesInTemplate);
        }

        @Override
        public String generateStringValue(Parameter p) {
            return t.generateStringValue(p, typesInTemplate);
        }

        // Use cache so that we won't construct duplicated types.
        public static HashMap<ConcreteGenericType, ConcreteGenericType> cache = new HashMap<>();

        public static ConcreteGenericType constructConcreteGenericType(
                GenericType t, ConcreteType t1) {
            ConcreteGenericType ret = new ConcreteGenericTypeOne(t, t1);
            if (!cache.keySet().contains(ret)) {
                cache.put(ret, ret);
            }
            return cache.get(ret);
        }

        public static ConcreteGenericType constructConcreteGenericType(
                GenericType t, ConcreteType t1,
                ConcreteType t2) {
            ConcreteGenericType ret = new ConcreteGenericTypeTwo(t, t1, t2);
            if (!cache.keySet().contains(ret)) {
                cache.put(ret, ret);
            }
            return cache.get(ret);
        }

        @Override
        public String toString() {
            // List<Int>
            // Pair<Int,TEXT>
            // List<Pair<Int,Text>
            StringBuilder sb = new StringBuilder();
            sb.append(t.toString());
            sb.append("<");
            for (int i = 0; i < typesInTemplate.size(); i++) {
                sb.append(typesInTemplate.get(i).toString());
                if (i != typesInTemplate.size() - 1)
                    sb.append(",");
            }
            sb.append(">");
            return sb.toString();
        }
    }

    public static class ConcreteGenericTypeOne extends ConcreteGenericType {

        public ConcreteGenericTypeOne(GenericType t, ConcreteType t1) {
            // TODO: This is ugly... Might need to change design... Leave it for
            // now.
            assert t instanceof GenericTypeOne;
            this.t = t;
            this.typesInTemplate.add(t1);
        }
    }

    public static class ConcreteGenericTypeTwo extends ConcreteGenericType {

        public ConcreteGenericTypeTwo(GenericType t, ConcreteType t1,
                ConcreteType t2) {
            assert t instanceof GenericTypeTwo; // This is ugly...
            this.t = t;
            this.typesInTemplate.add(t1);
            this.typesInTemplate.add(t2);
        }
    }
}
