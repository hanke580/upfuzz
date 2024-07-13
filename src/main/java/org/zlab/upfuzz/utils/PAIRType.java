package org.zlab.upfuzz.utils;

import java.util.List;
import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;

public class PAIRType extends ParameterType.GenericTypeTwo {
    public static final PAIRType instance = new PAIRType();
    public static final String signature = "org.zlab.upfuzz.utils.Pair";

    @Override
    public Parameter generateRandomParameter(State s, Command c,
            List<ConcreteType> types) {

        ConcreteType t1 = types.get(0); // TEXTType
        ConcreteType t2 = types.get(1); // TYPEType

        Pair<Parameter, Parameter> value = new Pair<>(
                t1.generateRandomParameter(s, c),
                t2.generateRandomParameter(s, c));

        ConcreteType type = ConcreteGenericType
                .constructConcreteGenericType(instance, t1, t2);

        return new Parameter(type, value);
    }

    @Override
    public Parameter generateRandomParameter(State s, Command c,
            List<ConcreteType> types,
            Object init) {
        assert init instanceof Pair;
        Pair<Object, Object> initValues = (Pair<Object, Object>) init;

        ConcreteType t1 = types.get(0); // TEXTType
        ConcreteType t2 = types.get(1); // TYPEType

        Pair<Parameter, Parameter> value = new Pair<>(
                t1.generateRandomParameter(s, c, initValues.left),
                t2.generateRandomParameter(s, c, ((Pair<?, ?>) init).right));

        ConcreteType type = ConcreteGenericType
                .constructConcreteGenericType(instance, t1, t2);

        return new Parameter(type, value);
    }

    @Override
    public String generateStringValue(Parameter p, List<ConcreteType> types) {
        StringBuilder sb = new StringBuilder();

        Pair<Parameter, Parameter> value = (Pair<Parameter, Parameter>) p.value;
        sb.append(value.left.toString());
        sb.append(" ");
        sb.append(value.right.toString());
        return sb.toString();
    }

    @Override
    public boolean isEmpty(State s, Command c, Parameter p,
            List<ConcreteType> types) {
        return false;
    }

    @Override
    public boolean mutate(State s, Command c, Parameter p,
            List<ConcreteType> types) {
        p.value = generateRandomParameter(s, c, types).value;

        ConcreteType t1 = types.get(0);
        ConcreteType t2 = types.get(1);

        assert p.value instanceof Pair;
        Pair<Parameter, Parameter> value = (Pair<Parameter, Parameter>) p.value;

        // 30% mutate 0, 70% mutate 1
        int mutateIdx = ParameterType.rand.nextInt(10);
        if (mutateIdx < 3) {
            return t1.mutate(s, c, value.left);
        } else {
            return t2.mutate(s, c, value.right);
        }
    }

    @Override
    public boolean isValid(State s, Command c, Parameter p,
            List<ConcreteType> types) {
        assert p.value instanceof Pair;

        Pair<Parameter, Parameter> initValues = (Pair<Parameter, Parameter>) p.value;

        ConcreteType t1 = types.get(0); // TEXTType
        ConcreteType t2 = types.get(1); // TYPEType

        return t1.isValid(s, c, initValues.left) &&
                t2.isValid(s, c, initValues.right);
    }

    @Override
    public String toString() {
        return "PAIR";
    }
}
