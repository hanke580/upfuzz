package org.zlab.upfuzz.utils;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.cassandra.CassandraTypes;

public class PAIRType implements ParameterType.TemplatedType {
  public static final PAIRType instance = new PAIRType();
  public static final String signature = "org.zlab.upfuzz.utils.Pair";

  @Override
  public String generateStringValue(Object value) {
    return null;
  }

  @Override
  public Object constructRandomValue(Parameter.TemplatedParameter parameter) {
    return null;
  }
}

