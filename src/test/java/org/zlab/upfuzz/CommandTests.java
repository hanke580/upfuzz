package org.zlab.upfuzz;

import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.cassandra.CassandraCommands;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.cassandra.CassandraTypes;
import org.zlab.upfuzz.utils.INTType;
import org.zlab.upfuzz.utils.Pair;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class CommandTests {

    @Test
    public void testCreateKSCommandGeneration(){
        CassandraState s = new CassandraState();
        CassandraCommands.CREAT_KEYSPACE cmd0 = new CassandraCommands.CREAT_KEYSPACE(s);
        cmd0.updateState(s);
        System.out.println(cmd0.constructCommandString());

        CassandraCommands.DROP_KEYSPACE cmd1 = new CassandraCommands.DROP_KEYSPACE(s);
        cmd1.updateState(s);
        System.out.println(cmd1.constructCommandString());
    }

    @Test
    public void testALTER_KEYSPACECommandGeneration(){
        CassandraState s = new CassandraState();
        CassandraCommands.CREAT_KEYSPACE cmd0 = new CassandraCommands.CREAT_KEYSPACE(s);
        cmd0.updateState(s);
        System.out.println(cmd0.constructCommandString());

        CassandraCommands.ALTER_KEYSPACE cmd1 = new CassandraCommands.ALTER_KEYSPACE(s);
        cmd1.updateState(s);
        System.out.println(cmd1.constructCommandString());
    }

    @Test
    public void testCREATE_TABLECommandGeneration() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {

        CassandraState s = new CassandraState();

        CassandraCommands.CREAT_KEYSPACE cmd0 = new CassandraCommands.CREAT_KEYSPACE(s);
        cmd0.updateState(s);
        System.out.println(cmd0.constructCommandString());


        CassandraCommands.CREATE_TABLE cmd1 = new CassandraCommands.CREATE_TABLE(s);
        cmd1.updateState(s);
        System.out.println(cmd1.constructCommandString());

        CassandraCommands.DROP_TABLE cmd2 = new CassandraCommands.DROP_TABLE(s);
        cmd2.updateState(s);
        System.out.println(cmd2.constructCommandString());

    }

    @Test
    public void testINSERTCommandGeneration() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {

        CassandraState s = new CassandraState();

        CassandraCommands.CREAT_KEYSPACE cmd0 = new CassandraCommands.CREAT_KEYSPACE(s);
        cmd0.updateState(s);
        System.out.println(cmd0.constructCommandString());

        CassandraCommands.CREATE_TABLE cmd1 = new CassandraCommands.CREATE_TABLE(s);
        cmd1.updateState(s);
        System.out.println(cmd1.constructCommandString());

        CassandraCommands.INSERT cmd2 = new CassandraCommands.INSERT(s);
        cmd2.updateState(s);
        System.out.println(cmd2.constructCommandString());
    }

    // FIXME drop primary key => infinate loop
    // @Test
    public void testALTER_TABLE_DROPCommandGeneration() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {

        CassandraState s = new CassandraState();

        CassandraCommands.CREATE_TABLE cmd1 = new CassandraCommands.CREATE_TABLE(s);
        cmd1.updateState(s);

        System.out.println(cmd1.constructCommandString());


        try {
            CassandraCommands.ALTER_TABLE_DROP cmd2 = new CassandraCommands.ALTER_TABLE_DROP(s);
            cmd2.updateState(s);
            System.out.println(cmd2.constructCommandString());
        } catch (CustomExceptions.PredicateUnSatisfyException e) {
            e.printStackTrace();
            System.out.println("Predicate is not satisfy, this command cannot be correctly constructed");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception is thrown during the construction of the current command");
        }
    }

    @Test
    public void testDELETECommandGeneration() {

        CassandraState s = new CassandraState();

        CassandraCommands.CREAT_KEYSPACE cmd0 = new CassandraCommands.CREAT_KEYSPACE(s);
        cmd0.updateState(s);

        CassandraCommands.CREATE_TABLE cmd1 = new CassandraCommands.CREATE_TABLE(s);
        cmd1.updateState(s);

        System.out.println(cmd1.constructCommandString());


        try {
            CassandraCommands.DELETE cmd2 = new CassandraCommands.DELETE(s);
            cmd2.updateState(s);
            System.out.println(cmd2.constructCommandString());
        } catch (CustomExceptions.PredicateUnSatisfyException e) {
            e.printStackTrace();
            System.out.println("Predicate is not satisfy, this command cannot be correctly constructed");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception is thrown during the construction of the current command");
        }
    }

    @Test
    public void testCREATE_INDEXCommandGeneration() {

        CassandraState s = new CassandraState();

        CassandraCommands.CREAT_KEYSPACE cmd0 = new CassandraCommands.CREAT_KEYSPACE(s);
        cmd0.updateState(s);

        CassandraCommands.CREATE_TABLE cmd1 = new CassandraCommands.CREATE_TABLE(s);
        cmd1.updateState(s);

        CassandraCommands.CREAT_INDEX cmd2 = new CassandraCommands.CREAT_INDEX(s);
        cmd2.updateState(s);

        CassandraCommands.DROP_INDEX cmd3 = new CassandraCommands.DROP_INDEX(s);
        cmd3.updateState(s);

        System.out.println(cmd0);
        System.out.println(cmd1);
        System.out.println(cmd2);
        System.out.println(cmd3);

    }

    @Test
    public void testCREATE_UDTCommandGeneration() {

        CassandraState s = new CassandraState();

        CassandraCommands.CREAT_KEYSPACE cmd0 = new CassandraCommands.CREAT_KEYSPACE(s);
        cmd0.updateState(s);

        CassandraCommands.CREATE_TABLE cmd1 = new CassandraCommands.CREATE_TABLE(s);
        cmd1.updateState(s);

        CassandraCommands.CREATE_TYPE cmd2 = new CassandraCommands.CREATE_TYPE(s);
        cmd2.updateState(s);

        CassandraCommands.DROP_TYPE cmd3 = new CassandraCommands.DROP_TYPE(s);
        cmd3.updateState(s);

        CassandraCommands.USE cmd4 = new CassandraCommands.USE(s);
        cmd4.updateState(s);
        
        System.out.println(cmd0);
        System.out.println(cmd1);
        System.out.println(cmd2);
        System.out.println(cmd3);
        System.out.println(cmd4);

    }

    @Test
    public void testSerializable() {
        CassandraState s = new CassandraState();

        CassandraCommands.CREAT_KEYSPACE cmd0 = new CassandraCommands.CREAT_KEYSPACE(s);
        cmd0.updateState(s);
        System.out.println(cmd0.constructCommandString());

        CassandraCommands.CREATE_TABLE cmd1 = new CassandraCommands.CREATE_TABLE(s);
        cmd1.updateState(s);
        System.out.println(cmd1.constructCommandString());

        CassandraCommands.INSERT cmd2 = new CassandraCommands.INSERT(s);
        cmd1.updateState(s);
        System.out.println(cmd1.constructCommandString());

        List<Command> l = new LinkedList<>();

        l.add(cmd0);
        l.add(cmd1);
        l.add(cmd2);



        try {
            FileOutputStream fileOut =
                    new FileOutputStream("/tmp/LIST.ser");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(l);
            out.close();
            fileOut.close();
            System.out.println("Serialized data is saved in /tmp/LIST.ser");
        } catch (IOException i) {
            i.printStackTrace();
            return;
        }

        List<Command> e = null;
        try {
            FileInputStream fileIn = new FileInputStream("/tmp/LIST.ser");
            ObjectInputStream in = new ObjectInputStream(fileIn);
            e = (List<Command>) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException i) {
            i.printStackTrace();
            return;
        } catch (ClassNotFoundException c) {
            System.out.println("Command class not found");
            c.printStackTrace();
            return;
        }
        assert e.size() == 3;

        System.out.println();
    }

    @Test
    public void testCommandWithInitialValue() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        CommandSequence commandSequence = cass13939CommandSequence();
        CommandSequence validationCommandSequence = commandSequence.generateRelatedReadSequence();

        Path filePath = Paths.get("/tmp/seed_cassandra_13939.ser");

        try {
            FileOutputStream fileOut =
                    new FileOutputStream(filePath.toFile());
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(new Pair<>(commandSequence, validationCommandSequence));
            out.close();
            fileOut.close();
            System.out.println("Serialized data is saved in " + filePath.toString());
        } catch (IOException i) {
            i.printStackTrace();
            return;
        }

        Pair<CommandSequence, CommandSequence> e = null;
        try {
            FileInputStream fileIn = new FileInputStream(filePath.toFile());
            ObjectInputStream in = new ObjectInputStream(fileIn);
            e = (Pair<CommandSequence, CommandSequence>) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException i) {
            i.printStackTrace();
            return;
        } catch (ClassNotFoundException c) {
            System.out.println("Command class not found");
            c.printStackTrace();
            return;
        }

        commandSequence.mutate();
        boolean useIdx = false;

        List<String> commandStringList = commandSequence.getCommandStringList();
        for (int i = 0; i < commandStringList.size(); i++) {
            if (useIdx)
                System.out.println("[" + i + "]" + "\t" + commandStringList.get(i));
            else
                System.out.println(commandStringList.get(i));
        }
        System.out.println("command size = " + commandStringList.size());

    }

    @Test
    public void testOneByteDiffCommandWithInitialValue() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        // This will create a command which only have one Byte difference, remove one char from the string
        CommandSequence commandSequence = cass13939CommandSequence_One_Byte_Diff();
        CommandSequence validationCommandSequence = commandSequence.generateRelatedReadSequence();

        Path filePath = Paths.get("/tmp/seed_cassandra_13939_One_Byte_Diff.ser");

        try {
            FileOutputStream fileOut =
                    new FileOutputStream(filePath.toFile());
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(new Pair<>(commandSequence, validationCommandSequence));
            out.close();
            fileOut.close();
            System.out.println("Serialized data is saved in " + filePath.toString());
        } catch (IOException i) {
            i.printStackTrace();
            return;
        }

        Pair<CommandSequence, CommandSequence> e = null;
        try {
            FileInputStream fileIn = new FileInputStream(filePath.toFile());
            ObjectInputStream in = new ObjectInputStream(fileIn);
            e = (Pair<CommandSequence, CommandSequence>) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException i) {
            i.printStackTrace();
            return;
        } catch (ClassNotFoundException c) {
            System.out.println("Command class not found");
            c.printStackTrace();
            return;
        }

        boolean mutateStatus = commandSequence.mutate();
        System.out.println("mutateStatus = " + mutateStatus);
        boolean useIdx = false;

        List<String> commandStringList = commandSequence.getCommandStringList();
        for (int i = 0; i < commandStringList.size(); i++) {
            if (useIdx)
                System.out.println("[" + i + "]" + "\t" + commandStringList.get(i));
            else
                System.out.println(commandStringList.get(i));
        }
        System.out.println("command size = " + commandStringList.size());

    }

    @Test
    public void testTwoByteDiffCommandWithInitialValue1() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        // This will create a command which only have one Byte difference, remove one char from the string
        CommandSequence commandSequence = cass13939CommandSequence_Two_Byte_Diff1();
        CommandSequence validationCommandSequence = commandSequence.generateRelatedReadSequence();

        Path filePath = Paths.get("/tmp/seed_cassandra_13939_Two_Byte_Diff1.ser");

        try {
            FileOutputStream fileOut =
                    new FileOutputStream(filePath.toFile());
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(new Pair<>(commandSequence, validationCommandSequence));
            out.close();
            fileOut.close();
            System.out.println("Serialized data is saved in " + filePath.toString());
        } catch (IOException i) {
            i.printStackTrace();
            return;
        }

        Pair<CommandSequence, CommandSequence> e = null;
        try {
            FileInputStream fileIn = new FileInputStream(filePath.toFile());
            ObjectInputStream in = new ObjectInputStream(fileIn);
            e = (Pair<CommandSequence, CommandSequence>) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException i) {
            i.printStackTrace();
            return;
        } catch (ClassNotFoundException c) {
            System.out.println("Command class not found");
            c.printStackTrace();
            return;
        }

        boolean mutateStatus = commandSequence.mutate();
        System.out.println("mutateStatus = " + mutateStatus);
        boolean useIdx = false;

        List<String> commandStringList = commandSequence.getCommandStringList();
        for (int i = 0; i < commandStringList.size(); i++) {
            if (useIdx)
                System.out.println("[" + i + "]" + "\t" + commandStringList.get(i));
            else
                System.out.println(commandStringList.get(i));
        }
        System.out.println("command size = " + commandStringList.size());

    }

    @Test
    public void testTwoByteDiffCommandWithInitialValue2() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        // This will create a command which only have one Byte difference, remove one char from the string
        CommandSequence commandSequence = cass13939CommandSequence_Two_Byte_Diff2();
        CommandSequence validationCommandSequence = commandSequence.generateRelatedReadSequence();

        Path filePath = Paths.get("/tmp/seed_cassandra_13939_Two_Byte_Diff2.ser");

        try {
            FileOutputStream fileOut =
                    new FileOutputStream(filePath.toFile());
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(new Pair<>(commandSequence, validationCommandSequence));
            out.close();
            fileOut.close();
            System.out.println("Serialized data is saved in " + filePath.toString());
        } catch (IOException i) {
            i.printStackTrace();
            return;
        }

        Pair<CommandSequence, CommandSequence> e = null;
        try {
            FileInputStream fileIn = new FileInputStream(filePath.toFile());
            ObjectInputStream in = new ObjectInputStream(fileIn);
            e = (Pair<CommandSequence, CommandSequence>) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException i) {
            i.printStackTrace();
            return;
        } catch (ClassNotFoundException c) {
            System.out.println("Command class not found");
            c.printStackTrace();
            return;
        }

        boolean mutateStatus = commandSequence.mutate();
        System.out.println("mutateStatus = " + mutateStatus);
        boolean useIdx = false;

        List<String> commandStringList = commandSequence.getCommandStringList();
        for (int i = 0; i < commandStringList.size(); i++) {
            if (useIdx)
                System.out.println("[" + i + "]" + "\t" + commandStringList.get(i));
            else
                System.out.println(commandStringList.get(i));
        }
        System.out.println("command size = " + commandStringList.size());

    }

    @Test
    public void testFourByteDiffCommandWithInitialValue() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        // Delete four bytes in two different commands
        CommandSequence commandSequence = cass13939CommandSequence_Four_Byte_Diff();
        CommandSequence validationCommandSequence = commandSequence.generateRelatedReadSequence();

        Path filePath = Paths.get("/tmp/seed_cassandra_13939_Four_Byte_Diff.ser");

        try {
            FileOutputStream fileOut =
                    new FileOutputStream(filePath.toFile());
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(new Pair<>(commandSequence, validationCommandSequence));
            out.close();
            fileOut.close();
            System.out.println("Serialized data is saved in " + filePath.toString());
        } catch (IOException i) {
            i.printStackTrace();
            return;
        }

        Pair<CommandSequence, CommandSequence> e = null;
        try {
            FileInputStream fileIn = new FileInputStream(filePath.toFile());
            ObjectInputStream in = new ObjectInputStream(fileIn);
            e = (Pair<CommandSequence, CommandSequence>) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException i) {
            i.printStackTrace();
            return;
        } catch (ClassNotFoundException c) {
            System.out.println("Command class not found");
            c.printStackTrace();
            return;
        }

        boolean mutateStatus = commandSequence.mutate();
        System.out.println("mutateStatus = " + mutateStatus);
        boolean useIdx = false;

        List<String> commandStringList = commandSequence.getCommandStringList();
        for (int i = 0; i < commandStringList.size(); i++) {
            if (useIdx)
                System.out.println("[" + i + "]" + "\t" + commandStringList.get(i));
            else
                System.out.println(commandStringList.get(i));
        }
        System.out.println("command size = " + commandStringList.size());

    }

    @Test
    public void testOneCmdDiffCommandWithInitialValue1() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        // This will create a command which only have one Byte difference, remove one char from the string
        CommandSequence commandSequence = cass13939CommandSequence_One_Command_Diff1();
        CommandSequence validationCommandSequence = commandSequence.generateRelatedReadSequence();

        Path filePath = Paths.get("/tmp/seed_cassandra_13939_One_Cmd_Diff1.ser");

        try {
            FileOutputStream fileOut =
                    new FileOutputStream(filePath.toFile());
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(new Pair<>(commandSequence, validationCommandSequence));
            out.close();
            fileOut.close();
            System.out.println("Serialized data is saved in " + filePath.toString());
        } catch (IOException i) {
            i.printStackTrace();
            return;
        }

        Pair<CommandSequence, CommandSequence> e = null;
        try {
            FileInputStream fileIn = new FileInputStream(filePath.toFile());
            ObjectInputStream in = new ObjectInputStream(fileIn);
            e = (Pair<CommandSequence, CommandSequence>) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException i) {
            i.printStackTrace();
            return;
        } catch (ClassNotFoundException c) {
            System.out.println("Command class not found");
            c.printStackTrace();
            return;
        }

        boolean mutateStatus = commandSequence.mutate();
        System.out.println("mutateStatus = " + mutateStatus);
        boolean useIdx = false;

        List<String> commandStringList = commandSequence.getCommandStringList();
        for (int i = 0; i < commandStringList.size(); i++) {
            if (useIdx)
                System.out.println("[" + i + "]" + "\t" + commandStringList.get(i));
            else
                System.out.println(commandStringList.get(i));
        }
        System.out.println("command size = " + commandStringList.size());

    }

    @Test
    public void testOneCmdDiffCommandWithInitialValue2() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        // This will create a command which only have one Byte difference, remove one char from the string
        CommandSequence commandSequence = cass13939CommandSequence_One_Command_Diff2();
        CommandSequence validationCommandSequence = commandSequence.generateRelatedReadSequence();

        Path filePath = Paths.get("/tmp/seed_cassandra_13939_One_Cmd_Diff2.ser");

        try {
            FileOutputStream fileOut =
                    new FileOutputStream(filePath.toFile());
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(new Pair<>(commandSequence, validationCommandSequence));
            out.close();
            fileOut.close();
            System.out.println("Serialized data is saved in " + filePath.toString());
        } catch (IOException i) {
            i.printStackTrace();
            return;
        }

        Pair<CommandSequence, CommandSequence> e = null;
        try {
            FileInputStream fileIn = new FileInputStream(filePath.toFile());
            ObjectInputStream in = new ObjectInputStream(fileIn);
            e = (Pair<CommandSequence, CommandSequence>) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException i) {
            i.printStackTrace();
            return;
        } catch (ClassNotFoundException c) {
            System.out.println("Command class not found");
            c.printStackTrace();
            return;
        }

        boolean mutateStatus = commandSequence.mutate();
        System.out.println("mutateStatus = " + mutateStatus);
        boolean useIdx = false;

        List<String> commandStringList = commandSequence.getCommandStringList();
        for (int i = 0; i < commandStringList.size(); i++) {
            if (useIdx)
                System.out.println("[" + i + "]" + "\t" + commandStringList.get(i));
            else
                System.out.println(commandStringList.get(i));
        }
        System.out.println("command size = " + commandStringList.size());

    }

    @Test
    public void testSELECTCommandGeneration() {

        CassandraState s = new CassandraState();

        CassandraCommands.CREAT_KEYSPACE cmd0 = new CassandraCommands.CREAT_KEYSPACE(s);
        cmd0.updateState(s);
        System.out.println(cmd0.constructCommandString());

        CassandraCommands.CREATE_TABLE cmd1 = new CassandraCommands.CREATE_TABLE(s);
        cmd1.updateState(s);
        System.out.println(cmd1.constructCommandString());

        CassandraCommands.INSERT cmd2 = new CassandraCommands.INSERT(s);
        cmd2.updateState(s);
        System.out.println(cmd2.constructCommandString());

        CassandraCommands.SELECT cmd3 = new CassandraCommands.SELECT(s);
        cmd2.updateState(s);
        System.out.println(cmd3.constructCommandString());
    }

    @Test
    public void testSELECTWithInitialValue() {

        CassandraState s = new CassandraState();

        // Command 0
        CassandraCommands.CREAT_KEYSPACE cmd0 = new CassandraCommands.CREAT_KEYSPACE(s, "myKS", 2, false);
        cmd0.updateState(s);
        // Command 1
        List<Pair<String, ParameterType.ConcreteType>> columns = new ArrayList<>();
        columns.add(new Pair<>("species", CassandraTypes.TEXTType.instance));
        columns.add(new Pair<>("common_name", new INTType()));
        columns.add(new Pair<>("population", new INTType()));
        columns.add(new Pair<>("average_size", CassandraTypes.TEXTType.instance));

        List<String> primaryColumns = new ArrayList<>();
        primaryColumns.add("species TEXT");
        primaryColumns.add("common_name INT");

        CassandraCommands.CREATE_TABLE cmd1 = new CassandraCommands.CREATE_TABLE(s, "myKS", "monkey_species", columns, primaryColumns, null);
        cmd1.updateState(s);
        // Command 2
        // 'Monkey', 0, 30, 'AAAAAAAAAAAAAAAAAAAAAAAAAAA'
        List<String> columns_INSERT = new ArrayList<>();
        columns_INSERT.add("species TEXT");
        columns_INSERT.add("common_name INT");
        columns_INSERT.add("population INT");
        columns_INSERT.add("average_size TEXT");

        List<Object> Values_INSERT = new ArrayList<>();
        Values_INSERT.add("Monkey");
        Values_INSERT.add(0);
        Values_INSERT.add(30);
        Values_INSERT.add("AAAAAAAAAAAAAAAAAAAAAAAAAAA");
        CassandraCommands.INSERT cmd2 = new CassandraCommands.INSERT(s, "myKS",
                "monkey_species", columns_INSERT, Values_INSERT);
        cmd2.updateState(s);




        //------
        List<String> columns_SELECT = new ArrayList<>();
//        columns_SELECT.add("species");

        Parameter columnName = cmd2.params.get(2);
        String[] columnNameList = columnName.toString().split(",");
        columns_SELECT.add(columnNameList[0].split(" ")[0]);
//        columns_SELECT.add(columnNameList[1]);

        Parameter insertValue = cmd2.params.get(3);
        System.out.println(columnName.toString());
        System.out.println(insertValue.toString());

        //------


        List<String> columns_where_SELECT = new ArrayList<>();
        columns_where_SELECT.add(columnNameList[0]);
        columns_where_SELECT.add(columnNameList[1]);


//        columns_where_SELECT.add("species TEXT");
//        columns_where_SELECT.add("common_name INT");

        List<Object> columns_values_SELECT = new ArrayList<>();

        List<Parameter> objects = (List<Parameter>) insertValue.getValue();
        columns_values_SELECT.add(objects.get(0).getValue());
        columns_values_SELECT.add(objects.get(1).getValue());


        System.out.println("ddd\n");
        for (Parameter p : objects) {
            System.out.println(p.toString());
        }

        CassandraCommands.SELECT cmd3 = new CassandraCommands.SELECT(s, "myKS",
                "monkey_species", columns_SELECT, columns_where_SELECT, columns_values_SELECT);
        cmd3.updateState(s);

        System.out.println("SELECT command: " + cmd3);

    }

    @Test
    public void testINSERTWithReadCommandGeneration() {

        CassandraState s = new CassandraState();

        CassandraCommands.CREAT_KEYSPACE cmd0 = new CassandraCommands.CREAT_KEYSPACE(s);
        cmd0.updateState(s);
        System.out.println(cmd0.constructCommandString());

        CassandraCommands.CREATE_TABLE cmd1 = new CassandraCommands.CREATE_TABLE(s);
        cmd1.updateState(s);
        System.out.println(cmd1.constructCommandString());

        CassandraCommands.INSERT cmd2 = new CassandraCommands.INSERT(s);
        cmd2.updateState(s);
        System.out.println(cmd2.constructCommandString());

        Set<Command> readCmds = cmd2.generateRelatedReadCommand(s);

        if (readCmds != null) {
            for (Command readCmd : readCmds) {
                System.out.println(readCmd.toString());
            }
        }

    }

    @Test
    public void testALTER_TABLE_ADDCommandGeneration() {

        CassandraState s = new CassandraState();

        CassandraCommands.CREAT_KEYSPACE cmd0 = new CassandraCommands.CREAT_KEYSPACE(s);
        cmd0.updateState(s);
        System.out.println(cmd0.constructCommandString());

        CassandraCommands.CREATE_TABLE cmd1 = new CassandraCommands.CREATE_TABLE(s);
        cmd1.updateState(s);
        System.out.println(cmd1.constructCommandString());

        CassandraCommands.ALTER_TABLE_ADD cmd2 = new CassandraCommands.ALTER_TABLE_ADD(s);
        cmd2.updateState(s);
        System.out.println(cmd2.constructCommandString());
    }

    @Test
    public void testReadCommandSequence() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        List<Command> l = new LinkedList<>();

        CassandraState s = new CassandraState();

        // Command 0
        CassandraCommands.CREAT_KEYSPACE cmd0 = new CassandraCommands.CREAT_KEYSPACE(s, "myKS", 2, false);
        cmd0.updateState(s);
        l.add(cmd0);

        // Command 1
        List<Pair<String, ParameterType.ConcreteType>> columns = new ArrayList<>();
        columns.add(new Pair<>("species", CassandraTypes.TEXTType.instance));
        columns.add(new Pair<>("common_name", new INTType()));
        columns.add(new Pair<>("population", new INTType()));
        columns.add(new Pair<>("average_size", CassandraTypes.TEXTType.instance));

        List<String> primaryColumns = new ArrayList<>();
        primaryColumns.add("species TEXT");
        primaryColumns.add("common_name INT");

        CassandraCommands.CREATE_TABLE cmd1 = new CassandraCommands.CREATE_TABLE(s, "myKS", "monkey_species", columns, primaryColumns, null);
        cmd1.updateState(s);
        l.add(cmd1);

        // Command 2
        // 'Monkey', 0, 30, 'AAAAAAAAAAAAAAAAAAAAAAAAAAA'
        List<String> columns_INSERT = new ArrayList<>();
        columns_INSERT.add("species TEXT");
        columns_INSERT.add("common_name INT");
        columns_INSERT.add("population INT");
        columns_INSERT.add("average_size TEXT");

        List<Object> Values_INSERT = new ArrayList<>();
        Values_INSERT.add("Monkey");
        Values_INSERT.add(0);
        Values_INSERT.add(30);
        Values_INSERT.add("AAAAAAAAAAAAAAAAAAAAAAAAAAA");
        CassandraCommands.INSERT cmd2 = new CassandraCommands.INSERT(s, "myKS", "monkey_species", columns_INSERT, Values_INSERT);
        cmd2.updateState(s);
        l.add(cmd2);

        // Command 3-10
        for (int i = 1; i < 9; i++) {
            Values_INSERT.remove(1);
            Values_INSERT.add(1, i);
            CassandraCommands.INSERT tmpCmd = new CassandraCommands.INSERT(s, "myKS", "monkey_species", columns_INSERT, Values_INSERT);
            tmpCmd.updateState(s);
            l.add(tmpCmd);
        }

        // Command 11
        CassandraCommands.ALTER_TABLE_DROP cmd11 = new CassandraCommands.ALTER_TABLE_DROP(s, "myKS", "monkey_species", "population INT");
        cmd11.updateState(s);
        l.add(cmd11);


        for (Command cmd : l) {
            System.out.println(cmd);
        }


        CommandSequence commandSequence = new CommandSequence(l, CassandraCommands.commandClassList, CassandraCommands.createCommandClassList, CassandraState.class, s);
//        CommandSequence validationCommandSequence = new CommandSequence(l, CassandraCommands.readCommandClassList, CassandraCommands.createCommandClassList, CassandraState.class, commandSequence.state);
        CommandSequence readCommandSequence = commandSequence.generateRelatedReadSequence();

        for (String cmdStr : readCommandSequence.getCommandStringList()) {
            System.out.println(cmdStr);
        }

    }

    public static CommandSequence cass13939CommandSequence() {
        List<Command> l = new LinkedList<>();

        CassandraState s = new CassandraState();

        // Command 0
        CassandraCommands.CREAT_KEYSPACE cmd0 = new CassandraCommands.CREAT_KEYSPACE(s, "myKS", 2, false);
        cmd0.updateState(s);
        l.add(cmd0);

        // Command 1
        List<Pair<String, ParameterType.ConcreteType>> columns = new ArrayList<>();
        columns.add(new Pair<>("species", CassandraTypes.TEXTType.instance));
        columns.add(new Pair<>("common_name", new INTType()));
        columns.add(new Pair<>("population", new INTType()));
        columns.add(new Pair<>("average_size", CassandraTypes.TEXTType.instance));

        List<String> primaryColumns = new ArrayList<>();
        primaryColumns.add("species TEXT");
        primaryColumns.add("common_name INT");

        CassandraCommands.CREATE_TABLE cmd1 = new CassandraCommands.CREATE_TABLE(s, "myKS", "monkey_species", columns, primaryColumns, null);
        cmd1.updateState(s);
        l.add(cmd1);

        // Command 2
        // 'Monkey', 0, 30, 'AAAAAAAAAAAAAAAAAAAAAAAAAAA'
        List<String> columns_INSERT = new ArrayList<>();
        columns_INSERT.add("species TEXT");
        columns_INSERT.add("common_name INT");
        columns_INSERT.add("population INT");
        columns_INSERT.add("average_size TEXT");

        List<Object> Values_INSERT = new ArrayList<>();
        Values_INSERT.add("Monkey");
        Values_INSERT.add(0);
        Values_INSERT.add(30);
        Values_INSERT.add("AAAAAAAAAAAAAAAAAAAAAAAAAAA");
        CassandraCommands.INSERT cmd2 = new CassandraCommands.INSERT(s, "myKS", "monkey_species", columns_INSERT, Values_INSERT);
        cmd2.updateState(s);
        l.add(cmd2);

        // Command 3-10
        for (int i = 1; i < 9; i++) {
            Values_INSERT.remove(1);
            Values_INSERT.add(1, i);
            CassandraCommands.INSERT tmpCmd = new CassandraCommands.INSERT(s, "myKS", "monkey_species", columns_INSERT, Values_INSERT);
            tmpCmd.updateState(s);
            l.add(tmpCmd);
        }

        // Command 11
        CassandraCommands.ALTER_TABLE_DROP cmd11 = new CassandraCommands.ALTER_TABLE_DROP(s, "myKS", "monkey_species", "population INT");
        cmd11.updateState(s);
        l.add(cmd11);


        for (Command cmd : l) {
            System.out.println(cmd);
        }

        CommandSequence commandSequence =
                new CommandSequence(l,
                        CassandraCommands.commandClassList,
                        CassandraCommands.createCommandClassList, CassandraState.class, s);
        return commandSequence;
    }

    public static CommandSequence cass13939CommandSequence_One_Byte_Diff() {

        List<Command> l = new LinkedList<>();

        CassandraState s = new CassandraState();

        // Command 0
        CassandraCommands.CREAT_KEYSPACE cmd0 =
                new CassandraCommands.CREAT_KEYSPACE(s, "myKS", 2, false);
        cmd0.updateState(s);
        l.add(cmd0);

        // Command 1
        List<Pair<String, ParameterType.ConcreteType>> columns = new ArrayList<>();
        columns.add(new Pair<>("species", CassandraTypes.TEXTType.instance));
        columns.add(new Pair<>("common_name", new INTType()));
        columns.add(new Pair<>("population", new INTType()));
        columns.add(new Pair<>("average_size", CassandraTypes.TEXTType.instance));

        List<String> primaryColumns = new ArrayList<>();
        primaryColumns.add("species TEXT");
        primaryColumns.add("common_name INT");

        CassandraCommands.CREATE_TABLE cmd1 =
                new CassandraCommands.CREATE_TABLE(s, "myKS", "monkey_species",
                        columns, primaryColumns, null);
        cmd1.updateState(s);
        l.add(cmd1);

        // Command 2
        // 'Monkey', 0, 30, 'AAAAAAAAAAAAAAAAAAAAAAAAAAA'
        List<String> columns_INSERT = new ArrayList<>();
        columns_INSERT.add("species TEXT");
        columns_INSERT.add("common_name INT");
        columns_INSERT.add("population INT");
        columns_INSERT.add("average_size TEXT");

        List<Object> Values_INSERT = new ArrayList<>();
        Values_INSERT.add("Monkey");
        Values_INSERT.add(0);
        Values_INSERT.add(30);
        Values_INSERT.add("AAAAAAAAAAAAAAAAAAAAAAAAAA"); // Less one 'A', one bit difference
        CassandraCommands.INSERT cmd2 =
                new CassandraCommands.INSERT(s, "myKS", "monkey_species",
                        columns_INSERT, Values_INSERT);
        cmd2.updateState(s);
        l.add(cmd2);

        Values_INSERT.set(3, "AAAAAAAAAAAAAAAAAAAAAAAAAAA"); // Add the A back
        // Command 3-10
        for (int i = 1; i < 9; i++) {
            Values_INSERT.remove(1);
            Values_INSERT.add(1, i);
            CassandraCommands.INSERT tmpCmd =
                    new CassandraCommands.INSERT(s, "myKS", "monkey_species",
                            columns_INSERT, Values_INSERT);
            tmpCmd.updateState(s);
            l.add(tmpCmd);
        }

        // Command 11
        CassandraCommands.ALTER_TABLE_DROP cmd11 =
                new CassandraCommands.ALTER_TABLE_DROP(s, "myKS",
                        "monkey_species", "population INT");
        cmd11.updateState(s);
        l.add(cmd11);


        for (Command cmd : l) {
            System.out.println(cmd);
        }

        CommandSequence commandSequence =
                new CommandSequence(l, CassandraCommands.commandClassList,
                        CassandraCommands.createCommandClassList, CassandraState.class,
                        s);
        return commandSequence;
    }

    public static CommandSequence cass13939CommandSequence_Two_Byte_Diff1() {

        List<Command> l = new LinkedList<>();

        CassandraState s = new CassandraState();

        // Command 0
        CassandraCommands.CREAT_KEYSPACE cmd0 =
                new CassandraCommands.CREAT_KEYSPACE(s, "myKS", 2, false);
        cmd0.updateState(s);
        l.add(cmd0);

        // Command 1
        List<Pair<String, ParameterType.ConcreteType>> columns = new ArrayList<>();
        columns.add(new Pair<>("species", CassandraTypes.TEXTType.instance));
        columns.add(new Pair<>("common_name", new INTType()));
        columns.add(new Pair<>("population", new INTType()));
        columns.add(new Pair<>("average_size", CassandraTypes.TEXTType.instance));

        List<String> primaryColumns = new ArrayList<>();
        primaryColumns.add("species TEXT");
        primaryColumns.add("common_name INT");

        CassandraCommands.CREATE_TABLE cmd1 =
                new CassandraCommands.CREATE_TABLE(s, "myKS", "monkey_species",
                        columns, primaryColumns, null);
        cmd1.updateState(s);
        l.add(cmd1);

        // Command 2
        // 'Monkey', 0, 30, 'AAAAAAAAAAAAAAAAAAAAAAAAAAA'
        List<String> columns_INSERT = new ArrayList<>();
        columns_INSERT.add("species TEXT");
        columns_INSERT.add("common_name INT");
        columns_INSERT.add("population INT");
        columns_INSERT.add("average_size TEXT");

        List<Object> Values_INSERT = new ArrayList<>();
        Values_INSERT.add("Monkey");
        Values_INSERT.add(0);
        Values_INSERT.add(30);
        Values_INSERT.add("AAAAAAAAAAAAAAAAAAAAAAAAA"); // Less two 'A', two Bytes difference
        CassandraCommands.INSERT cmd2 =
                new CassandraCommands.INSERT(s, "myKS", "monkey_species",
                        columns_INSERT, Values_INSERT);
        cmd2.updateState(s);
        l.add(cmd2);

        Values_INSERT.set(3, "AAAAAAAAAAAAAAAAAAAAAAAAAAA"); // Add the A back
        // Command 3-10
        for (int i = 1; i < 9; i++) {
            Values_INSERT.remove(1);
            Values_INSERT.add(1, i);
            CassandraCommands.INSERT tmpCmd =
                    new CassandraCommands.INSERT(s, "myKS", "monkey_species",
                            columns_INSERT, Values_INSERT);
            tmpCmd.updateState(s);
            l.add(tmpCmd);
        }

        // Command 11
        CassandraCommands.ALTER_TABLE_DROP cmd11 =
                new CassandraCommands.ALTER_TABLE_DROP(s, "myKS",
                        "monkey_species", "population INT");
        cmd11.updateState(s);
        l.add(cmd11);


        for (Command cmd : l) {
            System.out.println(cmd);
        }

        CommandSequence commandSequence =
                new CommandSequence(l, CassandraCommands.commandClassList,
                        CassandraCommands.createCommandClassList, CassandraState.class,
                        s);
        return commandSequence;
    }

    public static CommandSequence cass13939CommandSequence_Two_Byte_Diff2() {
        // Add two bytes in one INSERT

        List<Command> l = new LinkedList<>();

        CassandraState s = new CassandraState();

        // Command 0
        CassandraCommands.CREAT_KEYSPACE cmd0 =
                new CassandraCommands.CREAT_KEYSPACE(s, "myKS", 2, false);
        cmd0.updateState(s);
        l.add(cmd0);

        // Command 1
        List<Pair<String, ParameterType.ConcreteType>> columns = new ArrayList<>();
        columns.add(new Pair<>("species", CassandraTypes.TEXTType.instance));
        columns.add(new Pair<>("common_name", new INTType()));
        columns.add(new Pair<>("population", new INTType()));
        columns.add(new Pair<>("average_size", CassandraTypes.TEXTType.instance));

        List<String> primaryColumns = new ArrayList<>();
        primaryColumns.add("species TEXT");
        primaryColumns.add("common_name INT");

        CassandraCommands.CREATE_TABLE cmd1 =
                new CassandraCommands.CREATE_TABLE(s, "myKS", "monkey_species",
                        columns, primaryColumns, null);
        cmd1.updateState(s);
        l.add(cmd1);

        // Command 2
        // 'Monkey', 0, 30, 'AAAAAAAAAAAAAAAAAAAAAAAAAAA'
        List<String> columns_INSERT = new ArrayList<>();
        columns_INSERT.add("species TEXT");
        columns_INSERT.add("common_name INT");
        columns_INSERT.add("population INT");
        columns_INSERT.add("average_size TEXT");

        List<Object> Values_INSERT = new ArrayList<>();
        Values_INSERT.add("Monkey");
        Values_INSERT.add(0);
        Values_INSERT.add(30);
        Values_INSERT.add("AAAAAAAAAAAAAAAAAAAAAAAAAAABB"); // Add two 'A', two Bytes difference
        CassandraCommands.INSERT cmd2 =
                new CassandraCommands.INSERT(s, "myKS", "monkey_species",
                        columns_INSERT, Values_INSERT);
        cmd2.updateState(s);
        l.add(cmd2);

        Values_INSERT.set(3, "AAAAAAAAAAAAAAAAAAAAAAAAAAA"); // Add the A back
        // Command 3-10
        for (int i = 1; i < 9; i++) {
            Values_INSERT.remove(1);
            Values_INSERT.add(1, i);
            CassandraCommands.INSERT tmpCmd =
                    new CassandraCommands.INSERT(s, "myKS", "monkey_species",
                            columns_INSERT, Values_INSERT);
            tmpCmd.updateState(s);
            l.add(tmpCmd);
        }

        // Command 11
        CassandraCommands.ALTER_TABLE_DROP cmd11 =
                new CassandraCommands.ALTER_TABLE_DROP(s, "myKS",
                        "monkey_species", "population INT");
        cmd11.updateState(s);
        l.add(cmd11);


        for (Command cmd : l) {
            System.out.println(cmd);
        }

        CommandSequence commandSequence =
                new CommandSequence(l, CassandraCommands.commandClassList,
                        CassandraCommands.createCommandClassList, CassandraState.class,
                        s);
        return commandSequence;
    }

    public static CommandSequence cass13939CommandSequence_Four_Byte_Diff() {

        List<Command> l = new LinkedList<>();

        CassandraState s = new CassandraState();

        // Command 0
        CassandraCommands.CREAT_KEYSPACE cmd0 =
                new CassandraCommands.CREAT_KEYSPACE(s, "myKS", 2, false);
        cmd0.updateState(s);
        l.add(cmd0);

        // Command 1
        List<Pair<String, ParameterType.ConcreteType>> columns = new ArrayList<>();
        columns.add(new Pair<>("species", CassandraTypes.TEXTType.instance));
        columns.add(new Pair<>("common_name", new INTType()));
        columns.add(new Pair<>("population", new INTType()));
        columns.add(new Pair<>("average_size", CassandraTypes.TEXTType.instance));

        List<String> primaryColumns = new ArrayList<>();
        primaryColumns.add("species TEXT");
        primaryColumns.add("common_name INT");

        CassandraCommands.CREATE_TABLE cmd1 =
                new CassandraCommands.CREATE_TABLE(s, "myKS", "monkey_species",
                        columns, primaryColumns, null);
        cmd1.updateState(s);
        l.add(cmd1);

        // Command 2
        // 'Monkey', 0, 30, 'AAAAAAAAAAAAAAAAAAAAAAAAAAA'
        List<String> columns_INSERT = new ArrayList<>();
        columns_INSERT.add("species TEXT");
        columns_INSERT.add("common_name INT");
        columns_INSERT.add("population INT");
        columns_INSERT.add("average_size TEXT");

        // Pick two commands, delete two bytes in each of them

        // Delete two bytes in the first INSERT
        List<Object> Values_INSERT = new ArrayList<>();
        Values_INSERT.add("Monkey");
        Values_INSERT.add(0);
        Values_INSERT.add(30);
        Values_INSERT.add("AAAAAAAAAAAAAAAAAAAAAAAAA"); // Less four 'A', two Bytes difference
        CassandraCommands.INSERT cmd2 =
                new CassandraCommands.INSERT(s, "myKS", "monkey_species",
                        columns_INSERT, Values_INSERT);
        cmd2.updateState(s);
        l.add(cmd2);

        // Delete two bytes in the second INSERT
        Values_INSERT.set(1, 1); // D
        CassandraCommands.INSERT cmd3 =
                new CassandraCommands.INSERT(s, "myKS", "monkey_species",
                        columns_INSERT, Values_INSERT);
        cmd3.updateState(s);
        l.add(cmd3);


        Values_INSERT.set(3, "AAAAAAAAAAAAAAAAAAAAAAAAAAA"); // Add the A back
        // Command 3-10
        for (int i = 2; i < 9; i++) {
            Values_INSERT.set(1, i);
            CassandraCommands.INSERT tmpCmd =
                    new CassandraCommands.INSERT(s, "myKS", "monkey_species",
                            columns_INSERT, Values_INSERT);
            tmpCmd.updateState(s);
            l.add(tmpCmd);
        }

        // Command 11
        CassandraCommands.ALTER_TABLE_DROP cmd11 =
                new CassandraCommands.ALTER_TABLE_DROP(s, "myKS",
                        "monkey_species", "population INT");
        cmd11.updateState(s);
        l.add(cmd11);


        for (Command cmd : l) {
            System.out.println(cmd);
        }

        CommandSequence commandSequence =
                new CommandSequence(l, CassandraCommands.commandClassList,
                        CassandraCommands.createCommandClassList, CassandraState.class,
                        s);
        return commandSequence;
    }

    public static CommandSequence cass13939CommandSequence_One_Command_Diff1() {
        // Less one INSERT command

        List<Command> l = new LinkedList<>();

        CassandraState s = new CassandraState();

        // Command 0
        CassandraCommands.CREAT_KEYSPACE cmd0 =
                new CassandraCommands.CREAT_KEYSPACE(s, "myKS", 2, false);
        cmd0.updateState(s);
        l.add(cmd0);

        // Command 1
        List<Pair<String, ParameterType.ConcreteType>> columns = new ArrayList<>();
        columns.add(new Pair<>("species", CassandraTypes.TEXTType.instance));
        columns.add(new Pair<>("common_name", new INTType()));
        columns.add(new Pair<>("population", new INTType()));
        columns.add(new Pair<>("average_size", CassandraTypes.TEXTType.instance));

        List<String> primaryColumns = new ArrayList<>();
        primaryColumns.add("species TEXT");
        primaryColumns.add("common_name INT");

        CassandraCommands.CREATE_TABLE cmd1 =
                new CassandraCommands.CREATE_TABLE(s, "myKS", "monkey_species",
                        columns, primaryColumns, null);
        cmd1.updateState(s);
        l.add(cmd1);

        // Command 2
        // 'Monkey', 0, 30, 'AAAAAAAAAAAAAAAAAAAAAAAAAAA'
        List<String> columns_INSERT = new ArrayList<>();
        columns_INSERT.add("species TEXT");
        columns_INSERT.add("common_name INT");
        columns_INSERT.add("population INT");
        columns_INSERT.add("average_size TEXT");

        List<Object> Values_INSERT = new ArrayList<>();
        Values_INSERT.add("Monkey");
        Values_INSERT.add(0);
        Values_INSERT.add(30);
        Values_INSERT.add("AAAAAAAAAAAAAAAAAAAAAAAAAAA");
        CassandraCommands.INSERT cmd2 =
                new CassandraCommands.INSERT(s, "myKS", "monkey_species",
                        columns_INSERT, Values_INSERT);
        cmd2.updateState(s);
        l.add(cmd2);

        Values_INSERT.set(3, "AAAAAAAAAAAAAAAAAAAAAAAAAAA"); // Add the A back
        // Command 3-10
        for (int i = 1; i < 8; i++) { // Difference: Cut off one command 9 -> 8
            Values_INSERT.remove(1);
            Values_INSERT.add(1, i);
            CassandraCommands.INSERT tmpCmd =
                    new CassandraCommands.INSERT(s, "myKS", "monkey_species",
                            columns_INSERT, Values_INSERT);
            tmpCmd.updateState(s);
            l.add(tmpCmd);
        }

        // Command 11
        CassandraCommands.ALTER_TABLE_DROP cmd11 =
                new CassandraCommands.ALTER_TABLE_DROP(s, "myKS",
                        "monkey_species", "population INT");
        cmd11.updateState(s);
        l.add(cmd11);


        for (Command cmd : l) {
            System.out.println(cmd);
        }

        CommandSequence commandSequence =
                new CommandSequence(l, CassandraCommands.commandClassList,
                        CassandraCommands.createCommandClassList, CassandraState.class,
                        s);
        return commandSequence;
    }

    public static CommandSequence cass13939CommandSequence_One_Command_Diff2() {
        // Less one DROP command

        List<Command> l = new LinkedList<>();

        CassandraState s = new CassandraState();

        // Command 0
        CassandraCommands.CREAT_KEYSPACE cmd0 =
                new CassandraCommands.CREAT_KEYSPACE(s, "myKS", 2, false);
        cmd0.updateState(s);
        l.add(cmd0);

        // Command 1
        List<Pair<String, ParameterType.ConcreteType>> columns = new ArrayList<>();
        columns.add(new Pair<>("species", CassandraTypes.TEXTType.instance));
        columns.add(new Pair<>("common_name", new INTType()));
        columns.add(new Pair<>("population", new INTType()));
        columns.add(new Pair<>("average_size", CassandraTypes.TEXTType.instance));

        List<String> primaryColumns = new ArrayList<>();
        primaryColumns.add("species TEXT");
        primaryColumns.add("common_name INT");

        CassandraCommands.CREATE_TABLE cmd1 =
                new CassandraCommands.CREATE_TABLE(s, "myKS", "monkey_species",
                        columns, primaryColumns, null);
        cmd1.updateState(s);
        l.add(cmd1);

        // Command 2
        // 'Monkey', 0, 30, 'AAAAAAAAAAAAAAAAAAAAAAAAAAA'
        List<String> columns_INSERT = new ArrayList<>();
        columns_INSERT.add("species TEXT");
        columns_INSERT.add("common_name INT");
        columns_INSERT.add("population INT");
        columns_INSERT.add("average_size TEXT");

        List<Object> Values_INSERT = new ArrayList<>();
        Values_INSERT.add("Monkey");
        Values_INSERT.add(0);
        Values_INSERT.add(30);
        Values_INSERT.add("AAAAAAAAAAAAAAAAAAAAAAAAAAA");
        CassandraCommands.INSERT cmd2 =
                new CassandraCommands.INSERT(s, "myKS", "monkey_species",
                        columns_INSERT, Values_INSERT);
        cmd2.updateState(s);
        l.add(cmd2);

        Values_INSERT.set(3, "AAAAAAAAAAAAAAAAAAAAAAAAAAA"); // Add the A back
        // Command 3-10
        for (int i = 1; i < 9; i++) {
            Values_INSERT.remove(1);
            Values_INSERT.add(1, i);
            CassandraCommands.INSERT tmpCmd =
                    new CassandraCommands.INSERT(s, "myKS", "monkey_species",
                            columns_INSERT, Values_INSERT);
            tmpCmd.updateState(s);
            l.add(tmpCmd);
        }

        // There's no drop in the end

//        // Command 11
//        CassandraCommands.ALTER_TABLE_DROP cmd11 =
//                new CassandraCommands.ALTER_TABLE_DROP(s, "myKS",
//                        "monkey_species", "population INT");
//        cmd11.updateState(s);
//        l.add(cmd11);

        for (Command cmd : l) {
            System.out.println(cmd);
        }

        CommandSequence commandSequence =
                new CommandSequence(l, CassandraCommands.commandClassList,
                        CassandraCommands.createCommandClassList, CassandraState.class,
                        s);
        return commandSequence;
    }

}
