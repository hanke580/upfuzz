package org.zlab.upfuzz.cassandra;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.utils.PAIRType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class CassandraTypes {

  public static List<ParameterType> types = new ArrayList<>();
  public static List<ParameterType> typesWithTemplate = new ArrayList<>();

  static {
    types.add(TEXTType.instance);
//    types.add(LISTType.instance);
//    typesWithTemplate.add(LISTType.instance);

    // Because of templated types - template types are dynamically generated - we do not have a fixed list.
    // When generating a TYPEType, we pick among a list of
  }

  public static class TEXTType extends ParameterType.NormalType {

      public static final TEXTType instance = new TEXTType();
      public static final String signature = "java.lang.String";

      private TEXTType() {}

    @Override
    public Parameter generateRandomParameter(State s, Command c) {
        // TODO: generate a random string.
      return null;
    }

    @Override
    public String generateStringValue(Parameter p) {
      assert Objects.equals(p.type, instance);
      assert p.value instanceof String;
      return (String) p.value;
    }
  }

  // Can we use generics to implement this?
  // A parameter has a type, this type can be a generic!
  // E.g., List<Pair<Text, AnyType>>:
  // LISTType<PAIRType<TEXTType, ANYType>>.
  // In LISTType:
  //  - it has an instance of itself.
  //  - it uses its template to call its instance's generateRandomParameter function. 

  public static class LISTType extends ParameterType.TemplatedType {
    // templated types are not singleton!
    // TODO: we could optimize it by remembering all templated typee.
    public static final String signature = "java.util.List";

    public LISTType(ParameterType t) { // type in template
      super(t);
    }

    @Override
    public Parameter generateRandomParameter(State s, Command c) {

      List<Parameter> value = new ArrayList<>();

      int bound = 10; // specified by user
      int len = new Random().nextInt(bound);

      ParameterType t = typesInTemplate.get(0);

      for (int i = 0; i < len; i++) {
        value.add(t.generateRandomParameter(s, c));
      }

      Parameter ret = new Parameter(this, value);

      return ret;
    }

    @Override
    public String generateStringValue(Parameter p) {
      return null;
    }
  }

  public static class TYPEType extends ParameterType.NormalType {
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

    /**
     * TYPEType refers to a String that defines a type in Cassandra.
     * E.g., CREATE TABLE command could use a text, or Set<text>, a user defined type.
     * @param s // current state
     * @param c // current command
     * @return
     */
    @Override
    public Parameter generateRandomParameter(State s, Command c) {
      return null;
    }

    @Override
    public String generateStringValue(Parameter p) {
      return null;
    }
  }

}
