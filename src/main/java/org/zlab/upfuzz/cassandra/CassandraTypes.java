package org.zlab.upfuzz.cassandra;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;

import java.util.*;

public class CassandraTypes {

  public static List<ParameterType> types = new ArrayList<>();
  public static List<ParameterType.GenericType> genericTypes = new ArrayList<>();

  static {
    types.add(TEXTType.instance);
    types.add(LISTType.instance);
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

  public static class LISTType extends ParameterType.GenericType {
    // templated types are not singleton!
    // This is just a hack to be used in TYPEType.
    public static final LISTType instance = new LISTType();

    // TODO: we could optimize it by remembering all templated typee.
    public static final String signature = "java.util.List";

    private LISTType() {}

    @Override
    public Parameter generateRandomParameter(State s, Command c, List<ConcreteType> typesInTemplate) {

      List<Parameter> value = new ArrayList<>();

      int bound = 10; // specified by user
      int len = new Random().nextInt(bound);

      ConcreteType t = typesInTemplate.get(0);

      for (int i = 0; i < len; i++) {
        value.add(t.generateRandomParameter(s, c));
      }

      Parameter ret = new Parameter(this, value);

      return ret;
    }

    @Override
    public String generateStringValue(Parameter p, List<ConcreteType> typesInTemplate) {
      return null;
    }
  }

  public static class TYPEType extends ParameterType.ConcreteType {
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

      while (CassandraTypes.genericTypes.contains(t)) {
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

    private ParameterType selectRandomType() {
      int typeIdx = new Random().nextInt(CassandraTypes.types.size());
      return CassandraTypes.types.get(typeIdx);
    }
    private ConcreteType generateRandomType(GenericType g) {
      if ()
    }

    private ConcreteType generateRandomType() {
      ParameterType t = selectRandomType();
      if (t instanceof ConcreteType) {
        return (ConcreteType) t;
      } else if (t instanceof GenericType) {
        return new TemplatedType((ConcreteType) t, selectRandomType());
      }
      return (ConcreteType) t;
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

      ParameterType t = selectRandomType();

      Queue<ParameterType> q = new LinkedList<>();
      q.add(t);
      while (!q.isEmpty()) {
        if (t instanceof TemplatedTypeOne) {
          // generate one more

        } else if (t instanceof TemplatedTypeTwo) {

        }
      }

      if (t instanceof ConcreteType) {
        return new Parameter(this, t);
      } else if (t instanceof GenericType) {
        return new Parameter(this, new TemplatedType(t, selectRandomType()));
      } else if (t instanceof TemplatedTypeTwo) {
        return new Parameter(this, new )
      }

      while (CassandraTypes.genericTypes.contains(t)) {
        new Random().nextInt(CassandraTypes.types.size());
        t = CassandraTypes.types.get(typeIdx);
        type.add(t);
      }
      return null;
    }

    @Override
    public String generateStringValue(Parameter p) {
      return null;
    }
  }

}
