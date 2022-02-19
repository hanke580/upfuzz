package org.zlab.upfuzz.cassandra;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;

public class TypeParameter extends Parameter {

  public TypeParameter() {
    super(CassandraTypes.TYPEType.instance);
  }

  @Override
  public void generateValue(State state, Command currCmd) {
    value = type.constructRandomValue();
  }
}
