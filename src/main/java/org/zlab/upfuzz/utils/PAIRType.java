package org.zlab.upfuzz.utils;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraTypes;

import java.util.List;

public class PAIRType extends ParameterType.GenericTypeTwo {
  public static final PAIRType instance = new PAIRType();
  public static final String signature = "org.zlab.upfuzz.utils.Pair";

  @Override
  public Parameter generateRandomParameter(State s, Command c, List<ConcreteType> types) {

    ConcreteType t1 = types.get(0);
    ConcreteType t2 = types.get(1);

    Pair<Parameter, Parameter> value =
        new Pair<>(t1.generateRandomParameter(s, c), t2.generateRandomParameter(s, c));

    ConcreteType type = ConcreteGenericType.constructConcreteGenericType(instance, t1, t2);
    Parameter ret = new Parameter(type, value);

    return ret;
  }

  @Override
  public String generateStringValue(Parameter p, List<ConcreteType> types) {
    return null;
  }
}

