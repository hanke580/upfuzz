package org.zlab.upfuzz.utils;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.hbase.Utils;

import java.util.*;

public class SETType extends ParameterType.GenericTypeOne {

    public static final SETType instance = new SETType();
    private static final Random rand = new Random();

    @Override
    public Parameter generateRandomParameter(State s, Command c,
            List<ConcreteType> types) {
        // (Pair<TEXT,TYPE>)
        Set<Parameter> value = new HashSet<>();

        int len = rand.nextInt(Config.getConf().SET_TYPE_MAX_SIZE);

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
            List<ConcreteType> types, Object init) {
        // it should be a set of specific type, like if it's set<TEXT>
        ConcreteType t = types.get(0);
        Set<?> initValue = (Set<?>) init;
        Set<Parameter> value = new HashSet<>();
        for (Object item : initValue) {
            value.add(types.get(0).generateRandomParameter(s, c, item));
        }
        ConcreteType type = ConcreteGenericType
                .constructConcreteGenericType(
                        instance, t); // LIST<WhateverType>
        return new Parameter(type, value);
    }

    @Override
    public String generateStringValue(Parameter p, List<ConcreteType> types) {
        StringBuilder sb = new StringBuilder();

        Set<Parameter> value = (Set<Parameter>) p.getValue();
        sb.append("{");
        boolean isFirst = false;
        for (Parameter item : value) {
            if (!isFirst) {
                isFirst = true;
            } else {
                sb.append(",");
            }
            sb.append(item.toString());
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean isEmpty(State s, Command c, Parameter p,
            List<ConcreteType> types) {
        return ((Collection<Parameter>) p.getValue()).isEmpty();
    }

    @Override
    public boolean mutate(State s, Command c, Parameter p,
            List<ConcreteType> types) {
        // We can regenerate or just remove or add some values
        int choice = rand.nextInt(4);
        switch (choice) {
        case 0:
            p.value = generateRandomParameter(s, c, types).value;
            return true;
        case 1:
            return Utilities.setRandomDeleteAtLeaseOneItem(
                    (Set<Parameter>) p.getValue());
        case 2:
            int numToAdd = rand.nextInt(3);
            for (int i = 0; i < numToAdd; i++) {
                Parameter item = types.get(0).generateRandomParameter(s, c);
                ((Set<Parameter>) p.getValue()).add(item);
            }
            return true;
        case 3:
            // double size of this set
            int size = ((Set<Parameter>) p.getValue()).size();
            for (int i = 0; i < size; i++) {
                Parameter item = types.get(0).generateRandomParameter(s, c);
                ((Set<Parameter>) p.getValue()).add(item);
            }
        }
        return false;
    }

    @Override
    public boolean isValid(State s, Command c, Parameter p,
            List<ConcreteType> types) {
        Set<Parameter> value = (Set<Parameter>) p.getValue();
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
        return "set";
    }
}
