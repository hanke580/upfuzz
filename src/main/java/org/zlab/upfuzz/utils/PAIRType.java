package org.zlab.upfuzz.utils;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;

import java.util.List;

public class PAIRType extends ParameterType.GenericTypeTwo {
  public static final PAIRType instance = new PAIRType();
  public static final String signature = "org.zlab.upfuzz.utils.Pair";

  @Override
  public Parameter generateRandomParameter(State s, Command c, List<ConcreteType> types) {

    ConcreteType t1 = types.get(0); // TEXTType
    ConcreteType t2 = types.get(1); // TYPEType

    Pair<Parameter, Parameter> value =
        new Pair<>(t1.generateRandomParameter(s, c), t2.generateRandomParameter(s, c));

    ConcreteType type = ConcreteGenericType.constructConcreteGenericType(instance, t1, t2);

    return new Parameter(type, value);
  }

  @Override
  public Parameter generateRandomParameter(State s, Command c, List<ConcreteType> types, Object init) {
    assert init instanceof Pair;
    Pair<Object, Object> initValues = (Pair<Object, Object>) init;

    ConcreteType t1 = types.get(0); // TEXTType
    ConcreteType t2 = types.get(1); // TYPEType

    Pair<Parameter, Parameter> value =
            new Pair<>(t1.generateRandomParameter(s, c, initValues.left), t2.generateRandomParameter(s, c, ((Pair<?, ?>) init).right));

    ConcreteType type = ConcreteGenericType.constructConcreteGenericType(instance, t1, t2);

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
  public boolean isEmpty(State s, Command c, Parameter p, List<ConcreteType> types) {
    return false;
  }

  @Override
  public boolean mutate(State s, Command c, Parameter p, List<ConcreteType> types) {
    p.value = generateRandomParameter(s, c, types).value;
    return true;
  }
}

