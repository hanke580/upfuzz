package org.zlab.upfuzz.hbase;

import java.util.*;
import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.utils.INTType;

public class HBaseTypes {

    public static Map<ParameterType, String> type2String = new HashMap<>();
    public static Map<ParameterType, String> genericType2String = new HashMap<>();

    static {
        type2String.put(
                org.zlab.upfuzz.cassandra.CassandraTypes.TEXTType.instance,
                "TEXT");
        type2String.put(new INTType(), "INT");

        genericType2String.put(
                org.zlab.upfuzz.cassandra.CassandraTypes.LISTType.instance,
                "LIST");
    }

    /**
     * TODO: This TYPEType should also be able to enumerate user defined types
     * in Cassandra. It is feasible by using the current state: find the user
     * defined types and use an instance of UnionType to represent them.
     */
    public static class TYPEType extends ParameterType.ConcreteType {
        public static final org.zlab.upfuzz.cassandra.CassandraTypes.TYPEType instance = new org.zlab.upfuzz.cassandra.CassandraTypes.TYPEType();
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
