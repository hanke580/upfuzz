package org.zlab.upfuzz.cassandra;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.utils.PAIRType;

import java.util.*;

/**
 * TODO: Need to implement mutate() function and isValid() function.
 */
public class CassandraTypes {

  public static List<ParameterType> types = new ArrayList<>();
  public static List<ParameterType.GenericType> genericTypes = new ArrayList<>();

  static {
    types.add(TEXTType.instance);
    types.add(LISTType.instance);
    types.add(PAIRType.instance);
    genericTypes.add(LISTType.instance);

    // Because of templated types - template types are dynamically generated - we do not have a fixed list.
    // When generating a TYPEType, we pick among a list of
  }

  public static class TEXTType extends ParameterType.ConcreteType {

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

  public static class LISTType extends ParameterType.GenericTypeOne {
    // templated types are not singleton!
    // This is just a hack to be used in TYPEType.
    public static final LISTType instance = new LISTType();

    // TODO: we could optimize it by remembering all templated typee.
    public static final String signature = "java.util.List";

    public LISTType() {}

    @Override
    public Parameter generateRandomParameter(State s, Command c, List<ConcreteType> typesInTemplate) {
      // (Pair<TEXT,TYPE>)
      List<Parameter> value = new ArrayList<>();

      int bound = 10; // specified by user
      int len = new Random().nextInt(bound);

      ConcreteType t = typesInTemplate.get(0);

      for (int i = 0; i < len; i++) {
        value.add(t.generateRandomParameter(s, c));
      }

      ConcreteType type = ConcreteGenericType.constructConcreteGenericType(instance, t); // LIST<WhateverType>
      Parameter ret = new Parameter(type, value);

      return ret;
    }

    @Override
    public String generateStringValue(Parameter p, List<ConcreteType> typesInTemplate) {
      return null;
    }
  }

  /**
   * TODO: This TYPEType should also be able to enumerate user defined types in Cassandra.
   *   It is feasible by using the current state: find the user defined types and use an instance of UnionType to represent them.
   */
  public static class TYPEType extends ParameterType.ConcreteType {
    public static final TYPEType instance = new TYPEType();
    public static final String signature = "org.zlab.upfuzz.TYPE";

//    @Override
//    public String generateStringValue(Object value) {
//      assert value instanceof List;
//      assert !((List) value).isEmpty();
//      assert !(((List) value).get(0) instanceof ParameterType);
//
//      StringBuilder sb = new StringBuilder(((List) value).get(0).toString());
//      for (int i = 1; i < ((List) value).size(); i++) {
//        sb.append("<");
//        sb.append(((List) value).get(i).toString());
//      }
//      for (int i = 1; i < ((List) value).size(); i++) {
//        sb.append(">");
//      }
//
//      return sb.toString();
//    }

    private ParameterType selectRandomType() {
      int typeIdx = new Random().nextInt(CassandraTypes.types.size());
      return CassandraTypes.types.get(typeIdx);
    }

    private ConcreteType generateRandomType(GenericType g) {
      if (g instanceof GenericTypeOne) {
        return new ConcreteGenericTypeOne(g, generateRandomType());
      } else if (g instanceof GenericTypeTwo) {
        return new ConcreteGenericTypeTwo(g, generateRandomType(), generateRandomType());
      }
      assert false;
      return null; // should not happen.
    }


    private ConcreteType generateRandomType() {
      ParameterType t = selectRandomType();
      if (t instanceof ConcreteType) {
        return (ConcreteType) t;
      } else if (t instanceof GenericType) {
        return generateRandomType((GenericType) t);
      }
      assert false;
      return null; // should not happen.
    }

    /**
     * TYPEType refers to a String that defines a type in Cassandra.
     * E.g., CREATE TABLE command could use a text, or Set<text>, a user defined type.
     * Its value should be a ParameterType!
     * It could either be a normal type or a templated type.
     * If it is a templated type, we need to continue generating generic types in templates.
     * @param s // current state
     * @param c // current command
     * @return
     */
    @Override
    public Parameter generateRandomParameter(State s, Command c) {

      // Should limit how complicated the type could get...
      // TODO: Limit the number of recursions/iterations
      //  Change the recursive method to a iterative loop using stack or queue and limit loop.
      //  Or count how deep the recursion is and limit it.
      return new Parameter(this, generateRandomType());

    }

    @Override
    public String generateStringValue(Parameter p) {
      return null;
    }
  }

}
