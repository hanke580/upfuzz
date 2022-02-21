package org.zlab.upfuzz.utils;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraTypes;

public class PAIRType extends ParameterType.TemplatedType {
//  public static final PAIRType instance = new PAIRType();
  public static final String signature = "org.zlab.upfuzz.utils.Pair";

  public PAIRType(ParameterType t1, ParameterType t2) {
    super(t1, t2);
  }

  @Override
  public Parameter generateRandomParameter(State s, Command c) {

    ParameterType t1 = typesInTemplate.get(0);
    ParameterType t2 = typesInTemplate.get(1);

    Pair<Parameter, Parameter> value =
        new Pair<>(t1.generateRandomParameter(s, c), t2.generateRandomParameter(s, c));

    Parameter ret = new Parameter(this, value);

    return ret;
  }

  @Override
  public String generateStringValue(Parameter p) {
    return null;
  }
}

