package org.zlab.upfuzz.cassandra;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;

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
    public Object constructRandomValue(ParameterType typeInTemplate) {
      return null;
    }
  }

//  public static class PAIRType implements ParameterType.TemplatedType {
//    public static final PAIRType instance = new PAIRType();
//    public static final String signature = "org.zlab.upfuzz.PAIR";
//
//    @Override
//    public String generateStringValue(Object value) {
//      return null;
//    }
//
//    @Override
//    public Object constructRandomValue(ParameterType typeInTemplate) {
//      return null;
//    }
//  }

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
