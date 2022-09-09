package org.zlab.upfuzz.cassandra;

import java.util.*;
import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.utils.INTType;
import org.zlab.upfuzz.utils.PAIRType;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.STRINGType;

public class CassandraTypes {

    public static Map<ParameterType, String> type2String = new HashMap<>();
    public static Map<ParameterType, String> genericType2String = new HashMap<>();

    // public static List<ParameterType> types = new ArrayList<>();
    // public static List<ParameterType.GenericType> genericTypes = new
    // ArrayList<>();

    static {
        type2String.put(TEXTType.instance, "TEXT");
        type2String.put(new INTType(), "INT");

        // types.add(LISTType.instance);
        // types.add(PAIRType.instance);
        genericType2String.put(LISTType.instance, "LIST");

        // Because of templated types - template types are dynamically generated
        // -
        // we do not have a fixed list. When generating a TYPEType, we pick
        // among a
        // list of
    }

    public static class TEXTType extends STRINGType {
        /**
         * The difference between TEXTType and STRINGType is generateStringValue
         * func, For TEXT, it needs to be enclosed by ''.
         *
         * Also TEXT cannot be empty.
         */
        public static final TEXTType instance = new TEXTType();
        public static final String signature = ""; // TODO: Choose the right
                                                   // signature

        public TEXTType() {
            super();
        }

        public TEXTType(int MAX_LEN) {
            super(MAX_LEN);
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c) {
            String str = generateRandomString();

            while (str.isEmpty()) {
                str = generateRandomString();
            }

            return new Parameter(this, str);
        }

        @Override
        public Parameter generateRandomParameter(State s, Command c,
                Object init) {
            if (init == null) {
                return generateRandomParameter(s, c);
            }
            assert init instanceof String;
            String initValue = (String) init;
            return new Parameter(this, initValue);
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
            while (tmpParam.isEmpty(s, c) == true) {
                tmpParam.value = p.value;
                super.mutate(s, c, tmpParam);
            }
            p.value = tmpParam.value;
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

            int bound = 10; // specified by user
            int len = new Random().nextInt(bound);

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
            p.value = generateRandomParameter(s, c, types).value;
            return true;
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
            int len = new Random().nextInt(bound);

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

        private ParameterType selectRandomType() {
            // Can avoid this transform by storing a separate List
            List<ParameterType> types = new ArrayList<ParameterType>(
                    type2String.keySet());
            int typeIdx = new Random().nextInt(types.size());
            return types.get(typeIdx);
        }

        private ConcreteType generateRandomType(GenericType g) {
            if (g instanceof GenericTypeOne) {
                return new ConcreteGenericTypeOne(g, generateRandomType());
            } else if (g instanceof GenericTypeTwo) {
                return new ConcreteGenericTypeTwo(g, generateRandomType(),
                        generateRandomType());
            }
            assert false;
            return null; // should not happen.
        }

        private ConcreteType generateRandomType() {
            ParameterType t = selectRandomType();
            if (t instanceof ConcreteType) {
                return (ConcreteType) t;
            } else if (t instanceof GenericType) { // Shouldn't happen for now.
                return generateRandomType((GenericType) t);
            }
            assert false;
            return null; // should not happen.
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
            return new Parameter(this, generateRandomType());
        }
    }
}
