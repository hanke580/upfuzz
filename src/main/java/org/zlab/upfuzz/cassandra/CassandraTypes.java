package org.zlab.upfuzz.cassandra;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.utils.INTType;
import org.zlab.upfuzz.utils.PAIRType;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.STRINGType;

import java.util.*;

/**
 * TODO: Need to implement mutate() function and isValid() function.
 */
public class CassandraTypes {

  public static Map<ParameterType, String> type2String = new HashMap<>();
  public static Map<ParameterType, String> genericType2String = new HashMap<>();

  // public static List<ParameterType> types = new ArrayList<>();
  // public static List<ParameterType.GenericType> genericTypes = new ArrayList<>();

  static {
    type2String.put(TEXTType.instance, "TEXT");
    type2String.put(INTType.instance, "INT");

//    types.add(LISTType.instance);
//    types.add(PAIRType.instance);
    genericType2String.put(LISTType.instance, "LIST");

    // Because of templated types - template types are dynamically generated - we do not have a fixed list.
    // When generating a TYPEType, we pick among a list of
  }

  public static class TEXTType extends STRINGType {
    /**
     * The difference between TEXTType and STRINGType is generateStringValue func,
     * For TEXT, it needs to be enclosed by ''.
     */
    public static final TEXTType instance = new TEXTType();
    public static final String signature = ""; // TODO: Choose the right signature

    private TEXTType() {}

    @Override
    public Parameter generateRandomParameter(State s, Command c) {
      // TODO: generate a random string.
      return new Parameter(TEXTType.instance, generateRandomString());
    }

    @Override
    public String generateStringValue(Parameter p) {
      assert Objects.equals(p.type, instance);
      assert p.value instanceof String;
      return  "'" + (String) p.value + "'";

    }
  }

  public static class LISTType extends ParameterType.GenericTypeOne {
    // templated types are not singleton!
    // This is just a hack to be used in TYPEType.
    public static final LISTType instance = new LISTType();

    // TODO: we could optimize it by remembering all templated type.
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
      return new Parameter(type, value);
    }
//
//    @Override
//    void mutate() {
//      /**
//       * List mutation
//       * 1. Regenerate list
//       * 2. Pick one item, mutate it! (t.mutate())
//       */
//    }

    @Override
    public String generateStringValue(Parameter p, List<ConcreteType> typesInTemplate) {
      StringBuilder sb = new StringBuilder();

      ConcreteType t = typesInTemplate.get(0);
      if (t instanceof ConcreteGenericTypeTwo) {
        ConcreteGenericTypeTwo concreteGenericTypeTwo = (ConcreteGenericTypeTwo) t;
        if (concreteGenericTypeTwo.t instanceof PAIRType) {
          List<Pair<Parameter, Parameter>> value = (List<Pair<Parameter, Parameter>>) p.value;
          for (int i = 0; i < value.size(); i++) {
            sb.append(value.get(i).left.toString());
            sb.append(" ");
            sb.append(value.get(i).right.toString());
            if (i < value.size() - 1)
              sb.append(",");
          }
        }
      } else {
        List<Parameter> value = (List<Parameter>) p.value;
        for (int i = 0; i < value.size(); i++) {
          sb.append(value.get(i).toString());
          if (i < value.size() - 1)
            sb.append(",");
        }
      }
      return sb.toString();
    }
  }

  /**
   * Type will be List<Pair<Parameter, Parameter>>, but it requires the Pair.first must be unique
   */
  public static class MapLikeListType extends LISTType {
    // Example of mapFunc:
    // Parameter p -> p.value.left // p's ParameterType is Pair<TEXT, TEXTType>

    public static final MapLikeListType instance = new MapLikeListType();

    public MapLikeListType() {}

    @Override
    public Parameter generateRandomParameter(State s, Command c, List<ConcreteType> typesInTemplate) {

      List<Pair<Parameter, Parameter>> value = new ArrayList<>();
      Set<Parameter> leftSet = new HashSet<>();

      int bound = 10; // specified by user
      int len = new Random().nextInt(bound);

      ConcreteType t = typesInTemplate.get(0);

      for (int i = 0; i < len; i++) {
        Pair<Parameter, Parameter> val = (Pair<Parameter, Parameter>) t.generateRandomParameter(s, c).value;
        while (leftSet.contains(val.left)) {
          val = (Pair<Parameter, Parameter>) t.generateRandomParameter(s, c).value;
        }
        value.add(val);
        leftSet.add(val.left);
      }

      ConcreteType type = ConcreteGenericType.constructConcreteGenericType(instance, t); // LIST<WhateverType>

      return new Parameter(type, value);
    }
  }

  /**
   * TODO: This TYPEType should also be able to enumerate user defined types in Cassandra.
   *   It is feasible by using the current state: find the user defined types and use an instance of UnionType to represent them.
   */
  public static class TYPEType extends ParameterType.ConcreteType {
    public static final TYPEType instance = new TYPEType();
    public static final String signature = "org.zlab.upfuzz.TYPE";

    @Override
    public String generateStringValue(Parameter p) {
      /**
       * For now, we first only construct single concrete type here.
       * TODO: Implement the nested type.
       */
//      List<ParameterType> types = (List) type2String.values();

      return type2String.get(p.value);

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
    }

    @Override
    public boolean isValid(State s, Command c, Parameter p) {
      return false;
    }

    @Override
    public void fixIfNotValid(State s, Command c, Parameter p) {

    }

    private ParameterType selectRandomType() {
      // Can avoid this transform by storing a separate List
      List<ParameterType> types =  new ArrayList<ParameterType>(type2String.keySet());
      int typeIdx = new Random().nextInt(types.size());
      return types.get(typeIdx);
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
      } else if (t instanceof GenericType) { // Shouldn't happen for now.
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
     */
    @Override
    public Parameter generateRandomParameter(State s, Command c) {

      // Should limit how complicated the type could get...
      // TODO: Limit the number of recursions/iterations
      //  Change the recursive method to a iterative loop using stack or queue and limit loop.
      //  Or count how deep the recursion is and limit it.
      return new Parameter(this, generateRandomType());

    }
  }

}
