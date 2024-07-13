package org.zlab.upfuzz.cassandra;

import java.util.*;
import java.util.stream.Collectors;

import org.zlab.upfuzz.*;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.utils.*;

public class CassandraTypes {

    private static final Random rand = new Random();

    public static Map<ParameterType, String> type2String = new HashMap<>();
    public static Map<ParameterType, String> complexType2String = new HashMap<>();
    public static Map<ParameterType, String> genericType2String = new HashMap<>();

    // public static List<ParameterType> types = new ArrayList<>();
    // public static List<ParameterType.GenericType> genericTypes = new
    // ArrayList<>();

    static {
        type2String.put(TEXTType.instance, "TEXT");
        type2String.put(new INTType(), "INT");
        type2String.put(
                new ParameterType.ConcreteGenericTypeOne(SETType.instance,
                        TEXTType.instance),
                "set<text>");
        type2String.put(
                new ParameterType.ConcreteGenericTypeOne(SETType.instance,
                        new INTType(0, 1000)),
                "set<int>");

        // complexType2String
        complexType2String.put(
                new ParameterType.ConcreteGenericTypeOne(SETType.instance,
                        TEXTType.instance),
                "set<text>");
        complexType2String.put(
                new ParameterType.ConcreteGenericTypeOne(SETType.instance,
                        new INTType(0, 1000)),
                "set<int>");

        // types.add(LISTType.instance);
        // types.add(PAIRType.instance);
        genericType2String.put(LISTType.instance, "LIST");

        // Because of templated types - template types are dynamically generated
        // -
        // we do not have a fixed list. When generating a TYPEType, we pick
        // among a
        // list of
    }

    private static ParameterType selectRandomType(boolean isComplexType) {
        // Can avoid this transform by storing a separate List
        List<ParameterType> types;
        if (isComplexType)
            types = new ArrayList<ParameterType>(
                    complexType2String.keySet());
        else
            types = new ArrayList<ParameterType>(
                    type2String.keySet());
        int typeIdx = rand.nextInt(types.size());
        return types.get(typeIdx);
    }

    private static ParameterType.ConcreteType generateRandomType(
            ParameterType.GenericType g, boolean isComplexType) {
        if (g instanceof ParameterType.GenericTypeOne) {
            return new ParameterType.ConcreteGenericTypeOne(g,
                    generateRandomType(isComplexType));
        } else if (g instanceof ParameterType.GenericTypeTwo) {
            return new ParameterType.ConcreteGenericTypeTwo(g,
                    generateRandomType(isComplexType),
                    generateRandomType(isComplexType));
        }
        assert false;
        return null; // should not happen.
    }

    private static ParameterType.ConcreteType generateRandomType(
            boolean isComplexType) {
        ParameterType t = selectRandomType(isComplexType);
        if (t instanceof ParameterType.ConcreteType) {
            return (ParameterType.ConcreteType) t;
        } else if (t instanceof ParameterType.GenericType) { // Shouldn't happen
                                                             // for now.
            return generateRandomType((ParameterType.GenericType) t,
                    isComplexType);
        }
        assert false;
        return null; // should not happen.
    }

    public static class TEXTType extends STRINGType {
        /**
         * Differences between TEXTType and STRINGType:
         * (1) TEXT needs to be enclosed by ''.
         * (2) TEXT cannot be empty.
         */
        public static final TEXTType instance = new TEXTType();
        public static final String signature = "";

        public TEXTType() {
            super();
        }

        @Override
        public String generateStringValue(Parameter p) {
            assert p.value instanceof String;
            return "'" + (String) p.value + "'";
        }

        @Override
        public boolean mutate(State s, Command c, Parameter p) {
            Parameter tmpParam = new Parameter(this, p.value);
            super.mutate(s, c, tmpParam);
            // Make sure after the mutation, the value is still not empty
            while (tmpParam.isEmpty(s, c)) {
                tmpParam.value = p.value;
                super.mutate(s, c, tmpParam);
            }
            p.value = tmpParam.value;
            return true;
        }

        @Override
        public boolean isValid(State s, Command c, Parameter p) {
            if (p == null || !(p.type instanceof TEXTType))
                return false;
            String value = (String) p.value;
            if (value == null || value.isEmpty() || value.length() > MAX_LEN)
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "TEXT";
        }
    }

    public static class LISTType extends ParameterType.GenericTypeOne {
        // templated types are not singleton!
        // This is just a hack to be used in TYPEType.
        public static final LISTType instance = new LISTType();

        // TODO: we could optimize it by remembering all templated type.
        public static final String signature = "java.util.List";

        public LISTType() {
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c,
                List<ConcreteType> types) {
            // (Pair<TEXT,TYPE>)
            List<Parameter> value = new ArrayList<>();

            int len = rand
                    .nextInt(Config.getConf().CASSANDRA_LIST_TYPE_MAX_SIZE);

            ConcreteType t = types.get(0);

            for (int i = 0; i < len; i++) {
                value.add(t.generateRandomParameter(s, c));
            }

            ConcreteType type = ConcreteGenericType
                    .constructConcreteGenericType(
                            instance, t); // LIST<WhateverType>
            return new Parameter(type, value);
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c,
                List<ConcreteType> types,
                Object init) {
            assert init instanceof List;
            List<Object> initValues = (List<Object>) init;

            // (Pair<TEXT,TYPE>)
            List<Parameter> value = new ArrayList<>();

            int len = initValues.size();

            ConcreteType t = types.get(0);

            for (int i = 0; i < len; i++) {
                value.add(t.generateRandomParameter(s, c, initValues.get(i)));
            }

            ConcreteType type = ConcreteGenericType
                    .constructConcreteGenericType(
                            instance, t); // LIST<WhateverType>
            return new Parameter(type, value);
        }

        @Override
        public String generateStringValue(Parameter p,
                List<ConcreteType> types) {
            StringBuilder sb = new StringBuilder();

            List<Parameter> value = (List<Parameter>) p.value;
            for (int i = 0; i < value.size(); i++) {
                sb.append(value.get(i).toString());
                if (i < value.size() - 1)
                    sb.append(",");
            }
            return sb.toString();
        }

        @Override
        public boolean isEmpty(State s, Command c, Parameter p,
                List<ConcreteType> types) {
            // Maybe add a isValid() here
            return ((List<Parameter>) p.value).isEmpty();
        }

        @Override
        public boolean mutate(State s, Command c, Parameter p,
                List<ConcreteType> types) {
            ConcreteType t = types.get(0);
            assert p.value instanceof Collection;
            Collection<Parameter> value = (Collection<Parameter>) p.value;
            // mutate a concrete value of this list
            int mutateIdx = rand.nextInt(value.size());
            return t.mutate(s, c, (Parameter) value.toArray()[mutateIdx]);
        }

        @Override
        public boolean isValid(State s, Command c, Parameter p,
                List<ConcreteType> types) {
            // TODO: Make isValid also check the type, now is using the assert,
            // this is bad.
            assert p.value instanceof List;

            List<Parameter> value = (List<Parameter>) p.value;
            /**
             * TODO: The Type should also be the same.
             * Otherwise, if the type is different from the current list
             * List<TYPEType> vs List<STRING>. The latter one should also be
             * inValid! Now only make sure each parameter is correct
             */
            for (Parameter v : value) {
                if (!v.isValid(s, c)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return "LIST";
        }

        // @Override
        // public void mutate(State s, Command c, Parameter p) {
        // /**
        // * 1. [Dramatic Change] Regenerate the list
        // * 2. [Medium Change] Pick some item, call their mutate function
        // * 3. [Small Change] Pick one item, call their mutate function
        // */
        // p.value = generateStringValue(p, types);
        //
        //
        // }
    }

    /**
     * Type will be List<Pair<Parameter, Parameter>>, but it requires the
     * Pair.first must be unique
     */
    public static class MapLikeListType extends LISTType {
        // Example of mapFunc:
        // Parameter p -> p.value.left // p's ParameterType is Pair<TEXT,
        // TEXTType>

        public static final MapLikeListType instance = new MapLikeListType();

        public MapLikeListType() {
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c,
                List<ConcreteType> typesInTemplate) {

            ConcreteType t = typesInTemplate.get(0);
            List<Parameter> value = new ArrayList<>();

            assert t instanceof ConcreteGenericTypeTwo;
            assert ((ConcreteGenericTypeTwo) t).t instanceof PAIRType;

            // Set<Parameter> leftSet = new HashSet<>();

            Set<String> leftSet = new HashSet<>();

            int bound = 10; // specified by user
            int len = rand.nextInt(bound);

            for (int i = 0; i < len; i++) {
                Parameter p = t.generateRandomParameter(s, c);
                Parameter leftParam = ((Pair<Parameter, Parameter>) p.value).left;
                while (leftSet.contains(leftParam.toString())) {
                    p = t.generateRandomParameter(s, c);
                    leftParam = ((Pair<Parameter, Parameter>) p.value).left;
                }
                leftSet.add(leftParam.toString());
                value.add(p);
            }

            ConcreteType type = ConcreteGenericType
                    .constructConcreteGenericType(
                            this.instance, t); // LIST<WhateverType>

            return new Parameter(type, value);
        }
    }

    public static class MapLikeListColumnType extends MapLikeListType {
        // make sure it generate at least one non-complex type
        // speical usage for generation of columns

        public static final MapLikeListColumnType instance = new MapLikeListColumnType();

        @Override
        public Parameter generateRandomParameter(State s, Command c,
                List<ConcreteType> typesInTemplate) {

            ConcreteType t = typesInTemplate.get(0);
            List<Parameter> value = new ArrayList<>();

            assert t instanceof ConcreteGenericTypeTwo;
            assert ((ConcreteGenericTypeTwo) t).t instanceof PAIRType;

            // Set<Parameter> leftSet = new HashSet<>();

            Set<String> leftSet = new HashSet<>();

            int bound = 10; // specified by user
            int len = rand.nextInt(bound);

            // make sure the first 2 are non-complex type

            for (int i = 0; i < len; i++) {
                Parameter p = t.generateRandomParameter(s, c);
                Parameter leftParam = ((Pair<Parameter, Parameter>) p.value).left;
                Parameter rightParam = ((Pair<Parameter, Parameter>) p.value).right;

                // FIXME: might get stuck here
                // At least one non-complex type
                if (i < 1) {
                    while (leftSet.contains(leftParam.toString())
                            || rightParam.toString().contains("set")) {
                        p = t.generateRandomParameter(s, c);
                        leftParam = ((Pair<Parameter, Parameter>) p.value).left;
                        rightParam = ((Pair<Parameter, Parameter>) p.value).right;
                    }
                } else {
                    while (leftSet.contains(leftParam.toString())) {
                        p = t.generateRandomParameter(s, c);
                        leftParam = ((Pair<Parameter, Parameter>) p.value).left;
                    }
                }
                leftSet.add(leftParam.toString());
                value.add(p);
            }

            ConcreteType type = ConcreteGenericType
                    .constructConcreteGenericType(
                            this.instance, t); // LIST<WhateverType>

            return new Parameter(type, value);
        }

    }

    /**
     * TODO: This TYPEType should also be able to enumerate user defined types
     * in Cassandra. It is feasible by using the current state: find the user
     * defined types and use an instance of UnionType to represent them.
     */
    public static class TYPEType extends ParameterType.ConcreteType {
        public static final TYPEType instance = new TYPEType();
        public static final String signature = "org.zlab.upfuzz.TYPE";

        @Override
        public String generateStringValue(Parameter p) {
            /**
             * For now, we first only construct single concrete type here.
             * TODO: Implement the nested type.
             */
            // List<ParameterType> types = (List) type2String.values();
            assert p.value instanceof ConcreteType;
            // Didn't handle the situation when there are multiple nested types
            return ((ConcreteType) p.value).toString();
            // assert value instanceof List;
            // assert !((List) value).isEmpty();
            // assert !(((List) value).get(0) instanceof ParameterType);
            //
            // StringBuilder sb = new StringBuilder(((List)
            // value).get(0).toString());
            // for (int i = 1; i < ((List) value).size(); i++) {
            // sb.append("<");
            // sb.append(((List) value).get(i).toString());
            // }
            // for (int i = 1; i < ((List) value).size(); i++) {
            // sb.append(">");
            // }
            //
            // return sb.toString();
        }

        @Override
        public boolean isValid(State s, Command c, Parameter p) {
            if (p.value instanceof ConcreteType)
                return true;
            return false;
        }

        @Override
        public void regenerate(State s, Command c, Parameter p) {
        }

        @Override
        public boolean isEmpty(State s, Command c, Parameter p) {
            if (p.value == null || !(p.value instanceof ConcreteType))
                return true;
            return false;
        }

        @Override
        public boolean mutate(State s, Command c, Parameter p) {
            p.value = generateRandomParameter(s, c).value;
            return true;
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c,
                Object init) {
            if (init == null) {
                return generateRandomParameter(s, c);
            }
            assert init instanceof ConcreteType;
            return new Parameter(this, (ConcreteType) init);
        }

        /**
         * TYPEType refers to a String that defines a type in Cassandra.
         * E.g., CREATE TABLE command could use a text, or Set<text>, a user
         * defined type. Its value should be a ParameterType! It could either be
         * a normal type or a templated type. If it is a templated type, we need
         * to continue generating generic types in templates.
         */
        @Override
        public Parameter generateRandomParameter(State s, Command c) {

            // Should limit how complicated the type could get...
            // TODO: Limit the number of recursions/iterations
            // Change the recursive method to a iterative loop using stack or
            // queue
            // and limit loop. Or count how deep the recursion is and limit it.
            return new Parameter(this, generateRandomType(false));
        }
    }

    public static class COMPLEXTYPEType extends TYPEType {
        public static final COMPLEXTYPEType instance = new COMPLEXTYPEType();

        @Override
        public Parameter generateRandomParameter(State s, Command c) {

            // Should limit how complicated the type could get...
            // TODO: Limit the number of recursions/iterations
            // Change the recursive method to a iterative loop using stack or
            // queue
            // and limit loop. Or count how deep the recursion is and limit it.
            return new Parameter(this, generateRandomType(true));
        }
    }

    /**
     * A subset of regular columns (could be empty)
     * toString: column1,column2,column3
     */
    public static class RegColumnType extends ParameterType.ConcreteType {

        private Set<String> computeRegColumn(CassandraState s,
                CassandraCommand c) {
            Set<String> regColumns = new HashSet<>();
            CassandraTable table = s.getTable(c.params.get(0).toString(),
                    c.params.get(1).toString());
            List<String> primaryColumns = new LinkedList<>();
            for (Parameter column : table.primaryColName2Type) {
                Object obj = column.getValue();
                assert obj instanceof Pair;
                String columnName = ((Pair<Parameter, Parameter>) obj).left
                        .toString();
                primaryColumns.add(columnName);
            }
            for (Parameter column : table.colName2Type) {
                Object obj = column.getValue();
                assert obj instanceof Pair;
                String columnName = ((Pair<Parameter, Parameter>) obj).left
                        .toString();
                if (!primaryColumns.contains(columnName))
                    regColumns.add(columnName);
            }
            return regColumns;
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c,
                Object init) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c) {
            assert s instanceof CassandraState;
            assert c instanceof CassandraCommand;
            assert c.params.size() >= 2;
            CassandraState cassandraState = (CassandraState) s;
            Set<String> regColumns = computeRegColumn(cassandraState,
                    (CassandraCommand) c);
            Set<String> subSet = Utilities.subSet(regColumns);
            return new Parameter(this, Utilities.strings2Parameters(subSet));
        }

        @Override
        public String generateStringValue(Parameter p) {
            assert p.type instanceof RegColumnType;
            Set<String> regColumns = Utilities
                    .parameters2Strings((Set<Parameter>) p.value);
            StringBuilder sb = new StringBuilder();
            List<String> regColumnsList = new ArrayList<>(regColumns);
            for (int i = 0; i < regColumnsList.size(); i++) {
                sb.append(regColumnsList.get(i));
                if (i < regColumnsList.size() - 1)
                    sb.append(",");
            }
            return sb.toString();
        }

        @Override
        public boolean isValid(State s, Command c, Parameter p) {
            assert p.type instanceof RegColumnType;
            Set<String> regColumns = Utilities
                    .parameters2Strings((Set<Parameter>) p.value);
            // it needs to still be reg column and still exist
            Set<String> newRegColumns = computeRegColumn((CassandraState) s,
                    (CassandraCommand) c);
            for (String column : regColumns) {
                if (!newRegColumns.contains(column))
                    return false;
            }
            return true;
        }

        @Override
        public void regenerate(State s, Command c, Parameter p) {
            if (isValid(s, c, p)) {
                p.value = generateRandomParameter(s, c).value;
            }
        }

        @Override
        public boolean isEmpty(State s, Command c, Parameter p) {
            return ((Set<String>) p.value).isEmpty();
        }

        @Override
        public boolean mutate(State s, Command c, Parameter p) {
            // regenerate, but make sure results are different...
            Set<String> oldValue = (Set<String>) p.value;
            Set<String> newValue = (Set<String>) (generateRandomParameter(s,
                    c).value);
            if (oldValue.equals(newValue)) {
                return false;
            } else {
                p.value = newValue;
                return true;
            }
        }

        @Override
        public String toString() {
            return "RegColumnType";
        }
    }

    public static class PartitionSubsetType<T, U>
            extends ParameterType.SubsetType {

        public PartitionSubsetType(ConcreteType t,
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

            /**
             * Pick a subset from the configuration, it will also be a list of
             * parameters Return new Parameter(SubsetType, value)
             */

            // TODO: Make all the collection contain the parameter

            List<Object> targetSet = new ArrayList<Object>(
                    (Collection<Object>) targetCollection);
            List<Object> value = new ArrayList<>();

            // FIXME: A HACK! Filter out complex type
            List<Object> filteredTargetSet = new ArrayList<>();
            for (Object o : targetSet) {
                if (!o.toString().contains("set<")) {
                    filteredTargetSet.add(o);
                }
            }

            if (filteredTargetSet.size() > 0) {
                int setSize = rand.nextInt(filteredTargetSet.size() + 1); // specified
                // by user
                List<Integer> indexArray = new ArrayList<>();
                for (int i = 0; i < filteredTargetSet.size(); i++) {
                    indexArray.add(i);
                }
                Collections.shuffle(indexArray);

                for (int i = 0; i < setSize; i++) {
                    value.add(filteredTargetSet.get(indexArray.get(i)));
                }
            }
            return new Parameter(this, value);
        }
    }
}
