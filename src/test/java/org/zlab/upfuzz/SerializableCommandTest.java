/* (C)2022 */
package org.zlab.upfuzz;

import junit.framework.TestCase;

public class SerializableCommandTest extends TestCase {

    public void testNothing() {}

    // static Gson gson;

    // @BeforeAll
    // static public void initAll() {
    //     GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping();
    //     // gsonBuilder.registerTypeAdapter(ParameterType.class, new ParameterTypeAdapter());
    //     gsonBuilder.registerTypeAdapter(ConcreteType.class, new
    // CassandraTypeAdapter<ConcreteType>());
    //     gsonBuilder.registerTypeAdapter(GenericType.class, new
    // CassandraTypeAdapter<GenericType>());
    //     gsonBuilder.registerTypeAdapter(ConcreteGenericType.class, new
    // CassandraTypeAdapter<ConcreteGenericTypeOne>());
    //     gsonBuilder.registerTypeAdapter(Parameter.class, new
    // CassandraParameterAdapter<Parameter>());
    //     gsonBuilder.registerTypeAdapter(FetchCollectionLambda.class, new
    // LambdaInterfaceAdapter());
    //     gson = gsonBuilder.create();
    // }

    // // @Test
    // public void testSerializeCassandraTypes() {
    //     ParameterType.ConcreteType columnsType = // LIST<PAIR<String,TYPEType>>
    //             new ParameterType.NotEmpty(ParameterType.ConcreteGenericType
    //                     .constructConcreteGenericType(CassandraTypes.MapLikeListType.instance,
    //
    // ParameterType.ConcreteGenericType.constructConcreteGenericType(PAIRType.instance,
    //                                     new ParameterType.NotEmpty(STRINGType.instance),
    //                                     CassandraTypes.TYPEType.instance)));

    //     String json = gson.toJson(columnsType, ConcreteType.class);
    //     System.out.println("gson:\n" + json + "\n");
    // }

    // // @Test
    // public void testDeserializeCassandraTypes() {
    //     ParameterType.ConcreteType columnsType = // LIST<PAIR<String,TYPEType>>
    //             new ParameterType.NotEmpty(ParameterType.ConcreteGenericType
    //                     .constructConcreteGenericType(CassandraTypes.MapLikeListType.instance,
    //
    // ParameterType.ConcreteGenericType.constructConcreteGenericType(PAIRType.instance,
    //                                     new ParameterType.NotEmpty(STRINGType.instance),
    //                                     CassandraTypes.TYPEType.instance)));

    //     String json = gson.toJson(columnsType, ConcreteType.class);
    //     ParameterType.ConcreteType type = gson.fromJson(json, ConcreteType.class);
    //     System.out.println("columns0: " + columnsType.generateRandomParameter(null,
    // null).toString());
    //     System.out.println("columns1: " + type.generateRandomParameter(null, null).toString());
    // }

    // // @Test
    // public void testCassandraParameter() {
    //     CassandraState s = new CassandraState();

    //     ParameterType.ConcreteType columnsType = // LIST<PAIR<String,TYPEType>>
    //             new ParameterType.NotEmpty(ParameterType.ConcreteGenericType
    //                     .constructConcreteGenericType(CassandraTypes.MapLikeListType.instance,
    //
    // ParameterType.ConcreteGenericType.constructConcreteGenericType(PAIRType.instance,
    //                                     new ParameterType.NotEmpty(STRINGType.instance),
    //                                     CassandraTypes.TYPEType.instance)));

    //     Parameter columns = columnsType.generateRandomParameter(s, null);
    //     System.out.println("columns:\n" + columns.toString() + "\n");
    //     String json = gson.toJson(columns, Parameter.class);
    //     System.out.println("gson:\n" + json + "\n");
    // }

    // // @Test
    // public void testDeserializeCassandraParameter() {
    //     CassandraState s = new CassandraState();

    //     ParameterType.ConcreteType columnsType = // LIST<PAIR<String,TYPEType>>
    //             new ParameterType.NotEmpty(ParameterType.ConcreteGenericType
    //                     .constructConcreteGenericType(CassandraTypes.MapLikeListType.instance,
    //
    // ParameterType.ConcreteGenericType.constructConcreteGenericType(PAIRType.instance,
    //                                     new ParameterType.NotEmpty(STRINGType.instance),
    //                                     CassandraTypes.TYPEType.instance)));

    //     Parameter columns = columnsType.generateRandomParameter(s, null, null);
    //     // System.out.println("columns:\n" + columns.toString() + "\n");
    //     String json = gson.toJson(columns, Parameter.class);
    //     System.out.println("gson:\n" + json + "\n");

    // }

    // // @Test
    // public void testFetchCollection() {

    //     // CassandraState s = new CassandraState();

    //     // ParameterType.ConcreteType primaryColumnsType = new ParameterType.NotEmpty(new
    // ParameterType.SubsetType(
    //     //         columnsType, (s, c) -> (Collection<Parameter>) c.params.get(2).getValue(),
    // null));

    // }

    // // @Test
    // public void testJsonSerializeCommand() {
    //     CassandraState s = new CassandraState();

    //     CassandraCommands.CREAT_KEYSPACE cmd0 = new CassandraCommands.CREAT_KEYSPACE(s);
    //     cmd0.updateState(s);
    //     System.out.println(cmd0.constructCommandString());

    //     String json = gson.toJson(cmd0, CassandraCommands.CREAT_KEYSPACE.class);
    //     System.out.println("gson result:\n " + json);
    // }

    // // @Test
    // public void testJsonDeserializeCommand() {
    //     CassandraState s = new CassandraState();

    //     CassandraCommands.CREAT_KEYSPACE cmd0 = new CassandraCommands.CREAT_KEYSPACE(s);
    //     cmd0.updateState(s);
    //     System.out.println(cmd0.constructCommandString());

    //     String json = gson.toJson(cmd0, CassandraCommands.CREAT_KEYSPACE.class);
    //     System.out.println("gson:\n " + json);

    //     CassandraCommands.CREAT_KEYSPACE cmd1 = gson.fromJson(json,
    // CassandraCommands.CREAT_KEYSPACE.class);
    //     System.out.println("deseriailize:\n" + cmd1.constructCommandString());

    // }

    // // @Test
    // public void testJsonSerializeAbstractCommand() {
    //     CassandraState s = new CassandraState();

    //     CassandraCommands.CREATETABLE cmd0 = new CassandraCommands.CREATETABLE(s);
    //     cmd0.updateState(s);
    //     System.out.println(cmd0.constructCommandString());

    //     Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    //     gson.toJson(cmd0, CassandraCommands.CREATETABLE.class);
    //     System.out.println("gson result:\n " + gson);
    // }

    // // @Test
    // public void testSerializeViaJson() {
    //     CassandraState s = new CassandraState();

    //     CassandraCommands.CREAT_KEYSPACE cmd0 = new CassandraCommands.CREAT_KEYSPACE(s);
    //     cmd0.updateState(s);
    //     System.out.println(cmd0.constructCommandString());

    //     CassandraCommands.CREATETABLE cmd1 = new CassandraCommands.CREATETABLE(s);
    //     cmd1.updateState(s);
    //     System.out.println(cmd1.constructCommandString());

    //     List<Command> l = new LinkedList<>();

    //     l.add(cmd0);
    //     l.add(cmd1);

    //     // Type listType = new TypeToken<LinkedList<Command>>(){}.getType();
    //     // Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    //     // gson.toJson(l, listType);
    // }
}
