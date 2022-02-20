package org.zlab.upfuzz.cassandra;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.utils.PAIRType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CassandraTypes {

  public static List<ParameterType> types = new ArrayList<>();
  public static List<ParameterType> typesWithTemplate = new ArrayList<>();

  static {
    types.add(TEXTType.instance);
    types.add(LISTType.instance);

    typesWithTemplate.add(LISTType.instance);
  }

  public static class TEXTType implements ParameterType.NormalType {

      public static final TEXTType instance = new TEXTType();
      public static final String signature = "java.lang.String";

      private TEXTType() {}

      @Override
      public Object constructRandomValue() {
          Object ret = "RandomString"; // TODO: Generate a random string.
          // assert ret.type == TEXTType.instance;
          return ret;
      }

      @Override
      public String generateStringValue(Object value) {
          return (String) value;
      }
  }

  public static class LISTType implements ParameterType.TemplatedType {

    public static final LISTType instance = new LISTType();
    public static final String signature = "java.util.List";

    private LISTType() {}

    @Override
    public String generateStringValue(Object value) {
      return (String) value;
    }

    @Override
    public Object constructRandomValue(Parameter.TemplatedParameter parameter) {
      // This parameter should be the list itself.

      // value of a LISTType parameter is a list of parameters.
      List<Parameter> value = new ArrayList<>();

      int bound = 10; // specified by user
      int len = new Random().nextInt(bound);

      for (int i = 0; i < len; i++) {

        // For this specific list = columns - each parameter is a Pair<String, Type>.
        Parameter p1 =
            new Parameter.TemplatedParameter(PAIRType.instance,
                CassandraTypes.TEXTType.instance,
                CassandraTypes.TYPEType.instance) {
              @Override
              public void generateValue(State state, Command currCmd) {
                // p1's value doesn't exist in currCmd.colName2Type.keySet();
                // This might cause concurrent modification to columns! Need to check.
                // There is a way to use iterator to do concurrent modification.

//                                Pair<Parameter, Parameter>
              }
            };

        value.add(p1);
      }

      return null;
    }

    @Override
    public Object constructRandomValue(ParameterType typeInTemplate) {
      return null;
    }
  }

  public static class TYPEType implements ParameterType.NormalType {
    public static final TYPEType instance = new TYPEType();
    public static final String signature = "org.zlab.upfuzz.TYPE";
    @Override
    public Object constructRandomValue() {
      // TODO:
      //  Theoretically, template is an expression that could contain multiple types.
      //  For now, we only consider the case where there is one type.
      //  Because we did not see any Cassandra commands with complicated template expressions.
      List<ParameterType> type = new ArrayList<>();

      int typeIdx = new Random().nextInt(CassandraTypes.types.size());
      ParameterType t = CassandraTypes.types.get(typeIdx); // Need to enumerate this set.
      type.add(t);

      while (CassandraTypes.typesWithTemplate.contains(t)) {
        new Random().nextInt(CassandraTypes.types.size());
        t = CassandraTypes.types.get(typeIdx);
        type.add(t);
      }
      return type;
    }

    @Override
    public String generateStringValue(Object value) {
      assert value instanceof List;
      assert !((List) value).isEmpty();
      assert !(((List) value).get(0) instanceof ParameterType);

      StringBuilder sb = new StringBuilder(((List) value).get(0).toString());
      for (int i = 1; i < ((List) value).size(); i++) {
        sb.append("<");
        sb.append(((List) value).get(i).toString());
      }
      for (int i = 1; i < ((List) value).size(); i++) {
        sb.append(">");
      }

      return sb.toString();
    }
  }

}
