package org.zlab.upfuzz.cassandra;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.*;
import org.zlab.upfuzz.cassandra.cqlcommands.*;
import org.zlab.upfuzz.utils.INTType;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.SETType;
import org.zlab.upfuzz.utils.Utilities;

public class CommandTest extends AbstractTest {
    // @Test
    public void testSerializable() {
        CassandraState s = new CassandraState();

        CREATE_KEYSPACE cmd0 = new CREATE_KEYSPACE(
                s);
        cmd0.updateState(s);
        System.out.println(cmd0.constructCommandString());

        CREATE_TABLE cmd1 = new CREATE_TABLE(
                s);
        cmd1.updateState(s);
        System.out.println(cmd1.constructCommandString());

        INSERT cmd2 = new INSERT(s);
        cmd1.updateState(s);
        System.out.println(cmd1.constructCommandString());

        List<Command> l = new LinkedList<>();

        l.add(cmd0);
        l.add(cmd1);
        l.add(cmd2);

        try {
            FileOutputStream fileOut = new FileOutputStream("/tmp/LIST.ser");
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

    // @Test
    public void testCommandWithInitialValue()
            throws Exception {
        CommandSequence commandSequence = cass13939CommandSequence();
        CommandSequence validationCommandSequence = commandSequence
                .generateRelatedReadSequence();

        Path filePath = Paths.get("/tmp/seed_cassandra_13939.ser");

        try {
            FileOutputStream fileOut = new FileOutputStream(filePath.toFile());
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(
                    new Pair<>(commandSequence, validationCommandSequence));
            out.close();
            fileOut.close();
            System.out.println("Serialized data is saved in " +
                    filePath.toString());
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

        Utilities.printCommandSequence(e.left);

        commandSequence.mutate();
        boolean useIdx = false;

        // print : after mutation
        System.out.println("== After mutation ==");

        List<String> commandStringList = commandSequence.getCommandStringList();
        for (int i = 0; i < commandStringList.size(); i++) {
            if (useIdx)
                System.out.println("[" + i + "]"
                        + "\t" + commandStringList.get(i));
            else
                System.out.println(commandStringList.get(i));
        }
        System.out.println("command size = " + commandStringList.size());
    }

    // @Test
    public void testOneByteDiffCommandWithInitialValue()
            throws Exception {
        // This will create a command which only have one Byte difference,
        // remove one char from the string
        CommandSequence commandSequence = cass13939CommandSequence_One_Byte_Diff();
        CommandSequence validationCommandSequence = commandSequence
                .generateRelatedReadSequence();

        Path filePath = Paths
                .get("/tmp/seed_cassandra_13939_One_Byte_Diff.ser");

        try {
            FileOutputStream fileOut = new FileOutputStream(filePath.toFile());
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(
                    new Pair<>(commandSequence, validationCommandSequence));
            out.close();
            fileOut.close();
            System.out.println("Serialized data is saved in " +
                    filePath.toString());
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
                System.out.println("[" + i + "]"
                        + "\t" + commandStringList.get(i));
            else
                System.out.println(commandStringList.get(i));
        }
        System.out.println("command size = " + commandStringList.size());
    }

    // @Test
    public void testTwoByteDiffCommandWithInitialValue1()
            throws Exception {
        // This will create a command which only have one Byte difference,
        // remove one char from the string
        CommandSequence commandSequence = cass13939CommandSequence_Two_Byte_Diff1();
        CommandSequence validationCommandSequence = commandSequence
                .generateRelatedReadSequence();

        Path filePath = Paths
                .get("/tmp/seed_cassandra_13939_Two_Byte_Diff1.ser");

        try {
            FileOutputStream fileOut = new FileOutputStream(filePath.toFile());
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(
                    new Pair<>(commandSequence, validationCommandSequence));
            out.close();
            fileOut.close();
            System.out.println("Serialized data is saved in " +
                    filePath.toString());
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
                System.out.println("[" + i + "]"
                        + "\t" + commandStringList.get(i));
            else
                System.out.println(commandStringList.get(i));
        }
        System.out.println("command size = " + commandStringList.size());
    }

    // @Test
    public void testTwoByteDiffCommandWithInitialValue2()
            throws Exception {
        // This will create a command which only have one Byte difference,
        // remove one char from the string
        CommandSequence commandSequence = cass13939CommandSequence_Two_Byte_Diff2();
        CommandSequence validationCommandSequence = commandSequence
                .generateRelatedReadSequence();

        Path filePath = Paths
                .get("/tmp/seed_cassandra_13939_Two_Byte_Diff2.ser");

        try {
            FileOutputStream fileOut = new FileOutputStream(filePath.toFile());
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(
                    new Pair<>(commandSequence, validationCommandSequence));
            out.close();
            fileOut.close();
            System.out.println("Serialized data is saved in " +
                    filePath.toString());
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
                System.out.println("[" + i + "]"
                        + "\t" + commandStringList.get(i));
            else
                System.out.println(commandStringList.get(i));
        }
        System.out.println("command size = " + commandStringList.size());
    }

    // @Test
    public void testFourByteDiffCommandWithInitialValue()
            throws Exception {
        // Delete four bytes in two different commands
        CommandSequence commandSequence = cass13939CommandSequence_Four_Byte_Diff();
        CommandSequence validationCommandSequence = commandSequence
                .generateRelatedReadSequence();

        Path filePath = Paths
                .get("/tmp/seed_cassandra_13939_Four_Byte_Diff.ser");

        try {
            FileOutputStream fileOut = new FileOutputStream(filePath.toFile());
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(
                    new Pair<>(commandSequence, validationCommandSequence));
            out.close();
            fileOut.close();
            System.out.println("Serialized data is saved in " +
                    filePath.toString());
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
                System.out.println("[" + i + "]"
                        + "\t" + commandStringList.get(i));
            else
                System.out.println(commandStringList.get(i));
        }
        System.out.println("command size = " + commandStringList.size());
    }

    // @Test
    public void testOneCmdDiffCommandWithInitialValue1()
            throws Exception {
        // This will create a command which only have one Byte difference,
        // remove one char from the string
        CommandSequence commandSequence = cass13939CommandSequence_One_Command_Diff1();
        CommandSequence validationCommandSequence = commandSequence
                .generateRelatedReadSequence();

        Path filePath = Paths
                .get("/tmp/seed_cassandra_13939_One_Cmd_Diff1.ser");

        try {
            FileOutputStream fileOut = new FileOutputStream(filePath.toFile());
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(
                    new Pair<>(commandSequence, validationCommandSequence));
            out.close();
            fileOut.close();
            System.out.println("Serialized data is saved in " +
                    filePath.toString());
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
                System.out.println("[" + i + "]"
                        + "\t" + commandStringList.get(i));
            else
                System.out.println(commandStringList.get(i));
        }
        System.out.println("command size = " + commandStringList.size());
    }

    // @Test
    public void testOneCmdDiffCommandWithInitialValue2()
            throws Exception {
        // This will create a command which only have one Byte difference,
        // remove one char from the string
        CommandSequence commandSequence = cass13939CommandSequence_One_Command_Diff2();
        CommandSequence validationCommandSequence = commandSequence
                .generateRelatedReadSequence();

        Path filePath = Paths
                .get("/tmp/seed_cassandra_13939_One_Cmd_Diff2.ser");

        try {
            FileOutputStream fileOut = new FileOutputStream(filePath.toFile());
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(
                    new Pair<>(commandSequence, validationCommandSequence));
            out.close();
            fileOut.close();
            System.out.println("Serialized data is saved in " +
                    filePath.toString());
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
                System.out.println("[" + i + "]"
                        + "\t" + commandStringList.get(i));
            else
                System.out.println(commandStringList.get(i));
        }
        System.out.println("command size = " + commandStringList.size());
    }

    // @Test
    public void testTwoCmdDiffCommandWithInitialValue()
            throws Exception {
        // This will create a command which only have one Byte difference,
        // remove one char from the string
        CommandSequence commandSequence = cass13939CommandSequence_Two_Command_Diff();
        CommandSequence validationCommandSequence = commandSequence
                .generateRelatedReadSequence();

        Path filePath = Paths.get("/tmp/seed_cassandra_13939_Two_Cmd_Diff.ser");

        try {
            FileOutputStream fileOut = new FileOutputStream(filePath.toFile());
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(
                    new Pair<>(commandSequence, validationCommandSequence));
            out.close();
            fileOut.close();
            System.out.println("Serialized data is saved in " +
                    filePath.toString());
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
                System.out.println("[" + i + "]"
                        + "\t" + commandStringList.get(i));
            else
                System.out.println(commandStringList.get(i));
        }
        System.out.println("command size = " + commandStringList.size());
    }

    // @Test
    public void testTwoCmdDiffCommandWithInitialValue1()
            throws Exception {
        CommandSequence commandSequence = cass13939CommandSequence_Two_Command_Diff1();
        CommandSequence validationCommandSequence = commandSequence
                .generateRelatedReadSequence();

        Path filePath = Paths
                .get("/tmp/seed_cassandra_13939_Two_Cmd_Diff1.ser");

        try {
            FileOutputStream fileOut = new FileOutputStream(filePath.toFile());
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(
                    new Pair<>(commandSequence, validationCommandSequence));
            out.close();
            fileOut.close();
            System.out.println("Serialized data is saved in " +
                    filePath.toString());
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
                System.out.println("[" + i + "]"
                        + "\t" + commandStringList.get(i));
            else
                System.out.println(commandStringList.get(i));
        }
        System.out.println("command size = " + commandStringList.size());
    }

    // @Test
    public void testCASSANDRA14912()
            throws Exception {
        // Delete four bytes in two different commands
        CommandSequence commandSequence = cass14912();
        CommandSequence validationCommandSequence = commandSequence
                .generateRelatedReadSequence();

        // Path filePath = Paths
        // .get("/tmp/seed_cassandra_14912.ser");
        //
        // try {
        // FileOutputStream fileOut = new FileOutputStream(filePath.toFile());
        // ObjectOutputStream out = new ObjectOutputStream(fileOut);
        // out.writeObject(
        // new Pair<>(commandSequence, validationCommandSequence));
        // out.close();
        // fileOut.close();
        // System.out.println("Serialized data is saved in " +
        // filePath.toString());
        // } catch (IOException i) {
        // i.printStackTrace();
        // return;
        // }
        //
        // Pair<CommandSequence, CommandSequence> e = null;
        // try {
        // FileInputStream fileIn = new FileInputStream(filePath.toFile());
        // ObjectInputStream in = new ObjectInputStream(fileIn);
        // e = (Pair<CommandSequence, CommandSequence>) in.readObject();
        // in.close();
        // fileIn.close();
        // } catch (IOException i) {
        // i.printStackTrace();
        // return;
        // } catch (ClassNotFoundException c) {
        // System.out.println("Command class not found");
        // c.printStackTrace();
        // return;
        // }
        //
        // boolean mutateStatus = commandSequence.mutate();
        // System.out.println("mutateStatus = " + mutateStatus);
        // boolean useIdx = false;
        //
        // List<String> commandStringList =
        // commandSequence.getCommandStringList();
        // for (int i = 0; i < commandStringList.size(); i++) {
        // if (useIdx)
        // System.out.println("[" + i + "]"
        // + "\t" + commandStringList.get(i));
        // else
        // System.out.println(commandStringList.get(i));
        // }
        // System.out.println("command size = " + commandStringList.size());
    }

    @Test
    public void testCREATECommandGeneration() {
        CassandraState s = new CassandraState();

        CREATE_KEYSPACE cmd0 = new CREATE_KEYSPACE(
                s);
        cmd0.updateState(s);
        System.out.println(cmd0.constructCommandString());

        CREATE_TABLE cmd1 = new CREATE_TABLE(
                s);
        cmd1.updateState(s);
        System.out.println(cmd1.constructCommandString());

        INSERT cmd2 = new INSERT(s);
        cmd2.updateState(s);
        System.out.println(cmd2.constructCommandString());

        SELECT cmd3 = new SELECT(s);
        cmd2.updateState(s);
        System.out.println(cmd3.constructCommandString());
    }

    @Test
    public void testSELECTCommandGeneration() {

        CassandraState s = new CassandraState();

        CREATE_KEYSPACE cmd0 = new CREATE_KEYSPACE(
                s);
        cmd0.updateState(s);
        System.out.println(cmd0.constructCommandString());

        CREATE_TABLE cmd1 = new CREATE_TABLE(
                s);
        cmd1.updateState(s);
        System.out.println(cmd1.constructCommandString());

        INSERT cmd2 = new INSERT(s);
        cmd2.updateState(s);
        System.out.println(cmd2.constructCommandString());

        SELECT cmd3 = new SELECT(s);
        cmd2.updateState(s);
        System.out.println(cmd3.constructCommandString());
    }

    @Test
    public void testSELECTWithInitialValue() {

        CassandraState s = new CassandraState();

        // Command 0
        CREATE_KEYSPACE cmd0 = new CREATE_KEYSPACE(
                s, "myKS", 2, false);
        cmd0.updateState(s);
        // Command 1
        List<Pair<String, ParameterType.ConcreteType>> columns = new ArrayList<>();
        columns.add(new Pair<>("species", CassandraTypes.TEXTType.instance));
        columns.add(new Pair<>("common_name", new INTType()));
        columns.add(new Pair<>("population", new INTType()));
        columns.add(
                new Pair<>("average_size", CassandraTypes.TEXTType.instance));

        List<String> primaryColumns = new ArrayList<>();
        primaryColumns.add("species TEXT");
        primaryColumns.add("common_name INT");

        CREATE_TABLE cmd1 = new CREATE_TABLE(
                s, "myKS", "monkey_species", columns, primaryColumns, null);
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
        INSERT cmd2 = new INSERT(s, "myKS",
                "monkey_species", columns_INSERT, Values_INSERT);
        cmd2.updateState(s);

        // ------
        List<String> columns_SELECT = new ArrayList<>();
        // columns_SELECT.add("species");

        Parameter columnName = cmd2.params.get(2);
        String[] columnNameList = columnName.toString().split(",");
        columns_SELECT.add(columnNameList[0].split(" ")[0]);
        // columns_SELECT.add(columnNameList[1]);

        Parameter insertValue = cmd2.params.get(3);
        System.out.println(columnName.toString());
        System.out.println(insertValue.toString());

        // ------

        List<String> columns_where_SELECT = new ArrayList<>();
        columns_where_SELECT.add(columnNameList[0]);
        columns_where_SELECT.add(columnNameList[1]);

        // columns_where_SELECT.add("species TEXT");
        // columns_where_SELECT.add("common_name INT");

        List<Object> columns_values_SELECT = new ArrayList<>();

        List<Parameter> objects = (List<Parameter>) insertValue.getValue();
        columns_values_SELECT.add(objects.get(0).getValue());
        columns_values_SELECT.add(objects.get(1).getValue());

        System.out.println("ddd\n");
        for (Parameter p : objects) {
            System.out.println(p.toString());
        }

        SELECT cmd3 = new SELECT(s, "myKS",
                "monkey_species", columns_SELECT, columns_where_SELECT,
                columns_values_SELECT);
        cmd3.updateState(s);

        System.out.println("SELECT command: " + cmd3);
    }

    @Test
    public void testINSERTWithReadCommandGeneration() {

        CassandraState s = new CassandraState();

        CREATE_KEYSPACE cmd0 = new CREATE_KEYSPACE(
                s);
        cmd0.updateState(s);
        System.out.println(cmd0.constructCommandString());

        CREATE_TABLE cmd1 = new CREATE_TABLE(
                s);
        cmd1.updateState(s);
        System.out.println(cmd1.constructCommandString());

        INSERT cmd2 = new INSERT(s);
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

        CREATE_KEYSPACE cmd0 = new CREATE_KEYSPACE(
                s);
        cmd0.updateState(s);
        System.out.println(cmd0.constructCommandString());

        CREATE_TABLE cmd1 = new CREATE_TABLE(
                s);
        cmd1.updateState(s);
        System.out.println(cmd1.constructCommandString());

        ALTER_TABLE_ADD cmd2 = new ALTER_TABLE_ADD(
                s);
        cmd2.updateState(s);
        System.out.println(cmd2.constructCommandString());
    }

    @Test
    public void testALTER_TABLE_RENAMECommandGeneration() {

        CassandraState s = new CassandraState();

        CREATE_KEYSPACE cmd0 = new CREATE_KEYSPACE(
                s);
        cmd0.updateState(s);
        System.out.println(cmd0.constructCommandString());

        CREATE_TABLE cmd1 = new CREATE_TABLE(
                s);
        cmd1.updateState(s);
        System.out.println(cmd1.constructCommandString());

        ALTER_TABLE_RENAME cmd2 = new ALTER_TABLE_RENAME(
                s);
        System.out.println(cmd2.constructCommandString());

        cmd2.updateState(s);
    }

    @Test
    public void testReadCommandSequence() {
        List<Command> l = new LinkedList<>();

        CassandraState s = new CassandraState();

        // Command 0
        CREATE_KEYSPACE cmd0 = new CREATE_KEYSPACE(
                s, "myKS", 2, false);
        cmd0.updateState(s);
        l.add(cmd0);

        // Command 1
        List<Pair<String, ParameterType.ConcreteType>> columns = new ArrayList<>();
        columns.add(new Pair<>("species", CassandraTypes.TEXTType.instance));
        columns.add(new Pair<>("common_name", new INTType()));
        columns.add(new Pair<>("population", new INTType()));
        columns.add(
                new Pair<>("average_size", CassandraTypes.TEXTType.instance));

        List<String> primaryColumns = new ArrayList<>();
        primaryColumns.add("species TEXT");
        primaryColumns.add("common_name INT");

        CREATE_TABLE cmd1 = new CREATE_TABLE(
                s, "myKS", "monkey_species", columns, primaryColumns, null);
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
        INSERT cmd2 = new INSERT(s, "myKS",
                "monkey_species", columns_INSERT, Values_INSERT);
        cmd2.updateState(s);
        l.add(cmd2);

        // Command 3-10
        for (int i = 1; i < 9; i++) {
            Values_INSERT.remove(1);
            Values_INSERT.add(1, i);
            INSERT tmpCmd = new INSERT(s,
                    "myKS", "monkey_species", columns_INSERT, Values_INSERT);
            tmpCmd.updateState(s);
            l.add(tmpCmd);
        }

        // Command 11
        ALTER_TABLE_DROP cmd11 = new ALTER_TABLE_DROP(
                s, "myKS", "monkey_species", "population INT");
        cmd11.updateState(s);
        l.add(cmd11);

        for (Command cmd : l) {
            System.out.println(cmd.constructCommandString());
        }

        CommandSequence commandSequence = new CommandSequence(l,
                CassandraCommand.cassandraCommandPool.commandClassList,
                CassandraCommand.cassandraCommandPool.createCommandClassList,
                CassandraState.class,
                s);
        // CommandSequence validationCommandSequence = new CommandSequence(l,
        // readCommandClassList,
        // createCommandClassList, CassandraState.class,
        // commandSequence.state);
        CommandSequence readCommandSequence = commandSequence
                .generateRelatedReadSequence();

        for (String cmdStr : readCommandSequence.getCommandStringList()) {
            System.out.println(cmdStr);
        }
    }

    public static CommandSequence cass13939CommandSequence() {
        List<Command> l = new LinkedList<>();

        CassandraState s = new CassandraState();

        // Command 0
        CREATE_KEYSPACE cmd0 = new CREATE_KEYSPACE(
                s, "myKS", 2, false);
        cmd0.updateState(s);
        l.add(cmd0);

        // Command 1
        List<Pair<String, ParameterType.ConcreteType>> columns = new ArrayList<>();
        columns.add(new Pair<>("species", CassandraTypes.TEXTType.instance));
        columns.add(new Pair<>("common_name", new INTType()));
        columns.add(new Pair<>("population", new INTType()));
        columns.add(
                new Pair<>("average_size", CassandraTypes.TEXTType.instance));

        List<String> primaryColumns = new ArrayList<>();
        primaryColumns.add("species TEXT");
        primaryColumns.add("common_name INT");

        CREATE_TABLE cmd1 = new CREATE_TABLE(
                s, "myKS", "monkey_species", columns, primaryColumns, null);
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
        INSERT cmd2 = new INSERT(s, "myKS",
                "monkey_species", columns_INSERT, Values_INSERT);
        cmd2.updateState(s);
        l.add(cmd2);

        // Command 3-10
        for (int i = 1; i < 9; i++) {
            Values_INSERT.remove(1);
            Values_INSERT.add(1, i);
            INSERT tmpCmd = new INSERT(s,
                    "myKS", "monkey_species", columns_INSERT, Values_INSERT);
            tmpCmd.updateState(s);
            l.add(tmpCmd);
        }

        // Command 11
        ALTER_TABLE_DROP cmd11 = new ALTER_TABLE_DROP(
                s, "myKS", "monkey_species", "population INT");
        cmd11.updateState(s);
        l.add(cmd11);

        // for (Command cmd : l) {
        // System.out.println(cmd.constructCommandString());
        // }

        CommandSequence commandSequence = new CommandSequence(l,
                CassandraCommand.cassandraCommandPool.commandClassList,
                CassandraCommand.cassandraCommandPool.createCommandClassList,
                CassandraState.class,
                s);
        return commandSequence;
    }

    // @Test
    public void test_cass14803()
            throws Exception {
        CommandPool commandPool = new CassandraCommandPool();
        CommandSequence cass14803_cq = cass14803CommandSequence();

        CommandSequence read_cq = null;
        try {

            // If you want to generate some read sequence which is related
            // to a write command sequence, first use the write command
            // sequence to initialize the type pool, and then pass the state

            cass14803_cq.initializeTypePool();
            read_cq = CommandSequence.generateSequence(
                    commandPool.readCommandClassList, null,
                    CassandraState.class,
                    cass14803_cq.state, true);

            // CommandSequence read_cq =
            // cass14803_cq.generateRelatedReadSequence();
        } catch (Exception e) {
            e.printStackTrace();
        }
        assert (read_cq != null);
        System.out.println("Read Sequence");
        for (String str : read_cq.getCommandStringList()) {
            System.out.println(str);
        }
        Path filePath = Paths.get("/tmp/seed_cass14803.ser");
        Utilities.saveSeed(cass14803_cq, read_cq, filePath);

        cass14803_cq.mutate();

        Utilities.printCommandSequence(cass14803_cq);
    }

    public static CommandSequence cass14803CommandSequence() {
        List<Command> l = new LinkedList<>();

        CassandraState s = new CassandraState();

        // Command 0
        CREATE_KEYSPACE cmd0 = new CREATE_KEYSPACE(
                s, "myKS", 1, false);
        cmd0.updateState(s);
        l.add(cmd0);

        // Command 1
        List<Pair<String, ParameterType.ConcreteType>> columns = new ArrayList<>();
        columns.add(new Pair<>("k", new INTType()));
        columns.add(new Pair<>("c", new INTType()));
        columns.add(new Pair<>("v1", CassandraTypes.TEXTType.instance));
        columns.add(
                new Pair<>("v2", CassandraTypes.TEXTType.instance));

        List<String> primaryColumns = new ArrayList<>();
        primaryColumns.add("k INT");
        primaryColumns.add("c INT");

        CREATE_TABLE cmd1 = new CREATE_TABLE(
                s, "myKS", "tb", columns, primaryColumns, null);
        cmd1.updateState(s);
        l.add(cmd1);

        // Command 2
        // 'Monkey', 0, 30, 'AAAAAAAAAAAAAAAAAAAAAAAAAAA'
        List<String> columns_INSERT = new ArrayList<>();
        columns_INSERT.add("k INT");
        columns_INSERT.add("c INT");
        columns_INSERT.add("v1 TEXT");
        columns_INSERT.add("v2 TEXT");

        String LONGSTRING_1025_LEN = "";
        for (int i = 0; i < 1025; i++) {
            LONGSTRING_1025_LEN += "A";
        }

        List<Object> Values_INSERT = new ArrayList<>();
        Values_INSERT.add(100);
        Values_INSERT.add(0);
        Values_INSERT.add(LONGSTRING_1025_LEN);
        Values_INSERT.add(LONGSTRING_1025_LEN);

        INSERT cmd2 = new INSERT(s, "myKS",
                "tb", columns_INSERT, Values_INSERT);
        cmd2.updateState(s);
        l.add(cmd2);

        // Command3
        Values_INSERT.remove(1);
        Values_INSERT.add(1, 1);
        INSERT tmpCmd = new INSERT(s,
                "myKS", "tb", columns_INSERT, Values_INSERT);
        tmpCmd.updateState(s);
        l.add(tmpCmd);

        // INTType.addToPool(100);
        // INTType.addToPool(0);
        // INTType.addToPool(1);
        // STRINGType.addToPool(LONGSTRING_1025_LEN);

        for (Command cmd : l) {
            System.out.println(cmd.constructCommandString());
        }

        CommandSequence commandSequence = new CommandSequence(l,
                CassandraCommand.cassandraCommandPool.commandClassList,
                CassandraCommand.cassandraCommandPool.createCommandClassList,
                CassandraState.class,
                s);
        return commandSequence;
    }

    public static CommandSequence cass13939CommandSequence_One_Byte_Diff() {

        List<Command> l = new LinkedList<>();

        CassandraState s = new CassandraState();

        // Command 0
        CREATE_KEYSPACE cmd0 = new CREATE_KEYSPACE(
                s, "myKS", 2, false);
        cmd0.updateState(s);
        l.add(cmd0);

        // Command 1
        List<Pair<String, ParameterType.ConcreteType>> columns = new ArrayList<>();
        columns.add(new Pair<>("species", CassandraTypes.TEXTType.instance));
        columns.add(new Pair<>("common_name", new INTType()));
        columns.add(new Pair<>("population", new INTType()));
        columns.add(
                new Pair<>("average_size", CassandraTypes.TEXTType.instance));

        List<String> primaryColumns = new ArrayList<>();
        primaryColumns.add("species TEXT");
        primaryColumns.add("common_name INT");

        CREATE_TABLE cmd1 = new CREATE_TABLE(
                s, "myKS", "monkey_species", columns, primaryColumns, null);
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
        Values_INSERT.add("AAAAAAAAAAAAAAAAAAAAAAAAAA"); // Less one 'A', one
                                                         // bit difference
        INSERT cmd2 = new INSERT(s, "myKS",
                "monkey_species", columns_INSERT, Values_INSERT);
        cmd2.updateState(s);
        l.add(cmd2);

        Values_INSERT.set(3, "AAAAAAAAAAAAAAAAAAAAAAAAAAA"); // Add the A back
        // Command 3-10
        for (int i = 1; i < 9; i++) {
            Values_INSERT.remove(1);
            Values_INSERT.add(1, i);
            INSERT tmpCmd = new INSERT(s,
                    "myKS", "monkey_species", columns_INSERT, Values_INSERT);
            tmpCmd.updateState(s);
            l.add(tmpCmd);
        }

        // Command 11
        ALTER_TABLE_DROP cmd11 = new ALTER_TABLE_DROP(
                s, "myKS", "monkey_species", "population INT");
        cmd11.updateState(s);
        l.add(cmd11);

        for (Command cmd : l) {
            System.out.println(cmd.constructCommandString());
        }

        CommandSequence commandSequence = new CommandSequence(l,
                CassandraCommand.cassandraCommandPool.commandClassList,
                CassandraCommand.cassandraCommandPool.createCommandClassList,
                CassandraState.class,
                s);
        return commandSequence;
    }

    public static CommandSequence cass13939CommandSequence_Two_Byte_Diff1() {

        List<Command> l = new LinkedList<>();

        CassandraState s = new CassandraState();

        // Command 0
        CREATE_KEYSPACE cmd0 = new CREATE_KEYSPACE(
                s, "myKS", 2, false);
        cmd0.updateState(s);
        l.add(cmd0);

        // Command 1
        List<Pair<String, ParameterType.ConcreteType>> columns = new ArrayList<>();
        columns.add(new Pair<>("species", CassandraTypes.TEXTType.instance));
        columns.add(new Pair<>("common_name", new INTType()));
        columns.add(new Pair<>("population", new INTType()));
        columns.add(
                new Pair<>("average_size", CassandraTypes.TEXTType.instance));

        List<String> primaryColumns = new ArrayList<>();
        primaryColumns.add("species TEXT");
        primaryColumns.add("common_name INT");

        CREATE_TABLE cmd1 = new CREATE_TABLE(
                s, "myKS", "monkey_species", columns, primaryColumns, null);
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
        Values_INSERT.add("AAAAAAAAAAAAAAAAAAAAAAAAA"); // Less two 'A', two
                                                        // Bytes difference
        INSERT cmd2 = new INSERT(s, "myKS",
                "monkey_species", columns_INSERT, Values_INSERT);
        cmd2.updateState(s);
        l.add(cmd2);

        Values_INSERT.set(3, "AAAAAAAAAAAAAAAAAAAAAAAAAAA"); // Add the A back
        // Command 3-10
        for (int i = 1; i < 9; i++) {
            Values_INSERT.remove(1);
            Values_INSERT.add(1, i);
            INSERT tmpCmd = new INSERT(s,
                    "myKS", "monkey_species", columns_INSERT, Values_INSERT);
            tmpCmd.updateState(s);
            l.add(tmpCmd);
        }

        // Command 11
        ALTER_TABLE_DROP cmd11 = new ALTER_TABLE_DROP(
                s, "myKS", "monkey_species", "population INT");
        cmd11.updateState(s);
        l.add(cmd11);

        for (Command cmd : l) {
            System.out.println(cmd.constructCommandString());
        }

        CommandSequence commandSequence = new CommandSequence(l,
                CassandraCommand.cassandraCommandPool.commandClassList,
                CassandraCommand.cassandraCommandPool.createCommandClassList,
                CassandraState.class,
                s);
        return commandSequence;
    }

    public static CommandSequence cass13939CommandSequence_Two_Byte_Diff2() {
        // Add two bytes in one INSERT

        List<Command> l = new LinkedList<>();

        CassandraState s = new CassandraState();

        // Command 0
        CREATE_KEYSPACE cmd0 = new CREATE_KEYSPACE(
                s, "myKS", 2, false);
        cmd0.updateState(s);
        l.add(cmd0);

        // Command 1
        List<Pair<String, ParameterType.ConcreteType>> columns = new ArrayList<>();
        columns.add(new Pair<>("species", CassandraTypes.TEXTType.instance));
        columns.add(new Pair<>("common_name", new INTType()));
        columns.add(new Pair<>("population", new INTType()));
        columns.add(
                new Pair<>("average_size", CassandraTypes.TEXTType.instance));

        List<String> primaryColumns = new ArrayList<>();
        primaryColumns.add("species TEXT");
        primaryColumns.add("common_name INT");

        CREATE_TABLE cmd1 = new CREATE_TABLE(
                s, "myKS", "monkey_species", columns, primaryColumns, null);
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
        Values_INSERT.add("AAAAAAAAAAAAAAAAAAAAAAAAAAABB"); // Add two 'A', two
                                                            // Bytes difference
        INSERT cmd2 = new INSERT(s, "myKS",
                "monkey_species", columns_INSERT, Values_INSERT);
        cmd2.updateState(s);
        l.add(cmd2);

        Values_INSERT.set(3, "AAAAAAAAAAAAAAAAAAAAAAAAAAA"); // Add the A back
        // Command 3-10
        for (int i = 1; i < 9; i++) {
            Values_INSERT.remove(1);
            Values_INSERT.add(1, i);
            INSERT tmpCmd = new INSERT(s,
                    "myKS", "monkey_species", columns_INSERT, Values_INSERT);
            tmpCmd.updateState(s);
            l.add(tmpCmd);
        }

        // Command 11
        ALTER_TABLE_DROP cmd11 = new ALTER_TABLE_DROP(
                s, "myKS", "monkey_species", "population INT");
        cmd11.updateState(s);
        l.add(cmd11);

        for (Command cmd : l) {
            System.out.println(cmd.constructCommandString());
        }

        CommandSequence commandSequence = new CommandSequence(l,
                CassandraCommand.cassandraCommandPool.commandClassList,
                CassandraCommand.cassandraCommandPool.createCommandClassList,
                CassandraState.class,
                s);
        return commandSequence;
    }

    public static CommandSequence cass13939CommandSequence_Four_Byte_Diff() {

        List<Command> l = new LinkedList<>();

        CassandraState s = new CassandraState();

        // Command 0
        CREATE_KEYSPACE cmd0 = new CREATE_KEYSPACE(
                s, "myKS", 2, false);
        cmd0.updateState(s);
        l.add(cmd0);

        // Command 1
        List<Pair<String, ParameterType.ConcreteType>> columns = new ArrayList<>();
        columns.add(new Pair<>("species", CassandraTypes.TEXTType.instance));
        columns.add(new Pair<>("common_name", new INTType()));
        columns.add(new Pair<>("population", new INTType()));
        columns.add(
                new Pair<>("average_size", CassandraTypes.TEXTType.instance));

        List<String> primaryColumns = new ArrayList<>();
        primaryColumns.add("species TEXT");
        primaryColumns.add("common_name INT");

        CREATE_TABLE cmd1 = new CREATE_TABLE(
                s, "myKS", "monkey_species", columns, primaryColumns, null);
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
        Values_INSERT.add("AAAAAAAAAAAAAAAAAAAAAAAAA"); // Less four 'A', two
                                                        // Bytes difference
        INSERT cmd2 = new INSERT(s, "myKS",
                "monkey_species", columns_INSERT, Values_INSERT);
        cmd2.updateState(s);
        l.add(cmd2);

        // Delete two bytes in the second INSERT
        Values_INSERT.set(1, 1); // D
        INSERT cmd3 = new INSERT(s, "myKS",
                "monkey_species", columns_INSERT, Values_INSERT);
        cmd3.updateState(s);
        l.add(cmd3);

        Values_INSERT.set(3, "AAAAAAAAAAAAAAAAAAAAAAAAAAA"); // Add the A back
        // Command 3-10
        for (int i = 2; i < 9; i++) {
            Values_INSERT.set(1, i);
            INSERT tmpCmd = new INSERT(s,
                    "myKS", "monkey_species", columns_INSERT, Values_INSERT);
            tmpCmd.updateState(s);
            l.add(tmpCmd);
        }

        // Command 11
        ALTER_TABLE_DROP cmd11 = new ALTER_TABLE_DROP(
                s, "myKS", "monkey_species", "population INT");
        cmd11.updateState(s);
        l.add(cmd11);

        for (Command cmd : l) {
            System.out.println(cmd.constructCommandString());
        }

        CommandSequence commandSequence = new CommandSequence(l,
                CassandraCommand.cassandraCommandPool.commandClassList,
                CassandraCommand.cassandraCommandPool.createCommandClassList,
                CassandraState.class,
                s);
        return commandSequence;
    }

    public static CommandSequence cass13939CommandSequence_One_Command_Diff1() {
        // Less one INSERT command

        List<Command> l = new LinkedList<>();

        CassandraState s = new CassandraState();

        // Command 0
        CREATE_KEYSPACE cmd0 = new CREATE_KEYSPACE(
                s, "myKS", 2, false);
        cmd0.updateState(s);
        l.add(cmd0);

        // Command 1
        List<Pair<String, ParameterType.ConcreteType>> columns = new ArrayList<>();
        columns.add(new Pair<>("species", CassandraTypes.TEXTType.instance));
        columns.add(new Pair<>("common_name", new INTType()));
        columns.add(new Pair<>("population", new INTType()));
        columns.add(
                new Pair<>("average_size", CassandraTypes.TEXTType.instance));

        List<String> primaryColumns = new ArrayList<>();
        primaryColumns.add("species TEXT");
        primaryColumns.add("common_name INT");

        CREATE_TABLE cmd1 = new CREATE_TABLE(
                s, "myKS", "monkey_species", columns, primaryColumns, null);
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
        INSERT cmd2 = new INSERT(s, "myKS",
                "monkey_species", columns_INSERT, Values_INSERT);
        cmd2.updateState(s);
        l.add(cmd2);

        Values_INSERT.set(3, "AAAAAAAAAAAAAAAAAAAAAAAAAAA"); // Add the A back
        // Command 3-10
        for (int i = 1; i < 8; i++) { // Difference: Cut off one command 9 -> 8
            Values_INSERT.remove(1);
            Values_INSERT.add(1, i);
            INSERT tmpCmd = new INSERT(s,
                    "myKS", "monkey_species", columns_INSERT, Values_INSERT);
            tmpCmd.updateState(s);
            l.add(tmpCmd);
        }

        // Command 11
        ALTER_TABLE_DROP cmd11 = new ALTER_TABLE_DROP(
                s, "myKS", "monkey_species", "population INT");
        cmd11.updateState(s);
        l.add(cmd11);

        for (Command cmd : l) {
            System.out.println(cmd.constructCommandString());
        }

        CommandSequence commandSequence = new CommandSequence(l,
                CassandraCommand.cassandraCommandPool.commandClassList,
                CassandraCommand.cassandraCommandPool.createCommandClassList,
                CassandraState.class,
                s);
        return commandSequence;
    }

    public static CommandSequence cass13939CommandSequence_One_Command_Diff2() {
        // Less one DROP command

        List<Command> l = new LinkedList<>();

        CassandraState s = new CassandraState();

        // Command 0
        CREATE_KEYSPACE cmd0 = new CREATE_KEYSPACE(
                s, "myKS", 2, false);
        cmd0.updateState(s);
        l.add(cmd0);

        // Command 1
        List<Pair<String, ParameterType.ConcreteType>> columns = new ArrayList<>();
        columns.add(new Pair<>("species", CassandraTypes.TEXTType.instance));
        columns.add(new Pair<>("common_name", new INTType()));
        columns.add(new Pair<>("population", new INTType()));
        columns.add(
                new Pair<>("average_size", CassandraTypes.TEXTType.instance));

        List<String> primaryColumns = new ArrayList<>();
        primaryColumns.add("species TEXT");
        primaryColumns.add("common_name INT");

        CREATE_TABLE cmd1 = new CREATE_TABLE(
                s, "myKS", "monkey_species", columns, primaryColumns, null);
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
        INSERT cmd2 = new INSERT(s, "myKS",
                "monkey_species", columns_INSERT, Values_INSERT);
        cmd2.updateState(s);
        l.add(cmd2);

        Values_INSERT.set(3, "AAAAAAAAAAAAAAAAAAAAAAAAAAA"); // Add the A back
        // Command 3-10
        for (int i = 1; i < 9; i++) {
            Values_INSERT.remove(1);
            Values_INSERT.add(1, i);
            INSERT tmpCmd = new INSERT(s,
                    "myKS", "monkey_species", columns_INSERT, Values_INSERT);
            tmpCmd.updateState(s);
            l.add(tmpCmd);
        }

        // There's no drop in the end

        // // Command 11
        // ALTER_TABLE_DROP cmd11 =
        // new ALTER_TABLE_DROP(s, "myKS",
        // "monkey_species", "population INT");
        // cmd11.updateState(s);
        // l.add(cmd11);

        for (Command cmd : l) {
            System.out.println(cmd.constructCommandString());
        }

        CommandSequence commandSequence = new CommandSequence(l,
                CassandraCommand.cassandraCommandPool.commandClassList,
                CassandraCommand.cassandraCommandPool.createCommandClassList,
                CassandraState.class,
                s);
        return commandSequence;
    }

    public static CommandSequence cass13939CommandSequence_Two_Command_Diff() {
        // Remove one INSERT command + one Drop command

        List<Command> l = new LinkedList<>();

        CassandraState s = new CassandraState();

        // Command 0
        CREATE_KEYSPACE cmd0 = new CREATE_KEYSPACE(
                s, "myKS", 2, false);
        cmd0.updateState(s);
        l.add(cmd0);

        // Command 1
        List<Pair<String, ParameterType.ConcreteType>> columns = new ArrayList<>();
        columns.add(new Pair<>("species", CassandraTypes.TEXTType.instance));
        columns.add(new Pair<>("common_name", new INTType()));
        columns.add(new Pair<>("population", new INTType()));
        columns.add(
                new Pair<>("average_size", CassandraTypes.TEXTType.instance));

        List<String> primaryColumns = new ArrayList<>();
        primaryColumns.add("species TEXT");
        primaryColumns.add("common_name INT");

        CREATE_TABLE cmd1 = new CREATE_TABLE(
                s, "myKS", "monkey_species", columns, primaryColumns, null);
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
        INSERT cmd2 = new INSERT(s, "myKS",
                "monkey_species", columns_INSERT, Values_INSERT);
        cmd2.updateState(s);
        l.add(cmd2);

        Values_INSERT.set(3, "AAAAAAAAAAAAAAAAAAAAAAAAAAA"); // Add the A back
        // Command 3-10
        for (int i = 1; i < 8; i++) { // Difference: Cut off one command 9 -> 8
            Values_INSERT.remove(1);
            Values_INSERT.add(1, i);
            INSERT tmpCmd = new INSERT(s,
                    "myKS", "monkey_species", columns_INSERT, Values_INSERT);
            tmpCmd.updateState(s);
            l.add(tmpCmd);
        }

        for (Command cmd : l) {
            System.out.println(cmd.constructCommandString());
        }

        CommandSequence commandSequence = new CommandSequence(l,
                CassandraCommand.cassandraCommandPool.commandClassList,
                CassandraCommand.cassandraCommandPool.createCommandClassList,
                CassandraState.class,
                s);
        return commandSequence;
    }

    public static CommandSequence cass13939CommandSequence_Two_Command_Diff1() {
        // Less one INSERT command
        // TODO: Why does create table command generate two table with the same
        // name, this is not correct! Need to FIX
        // CREATE KEYSPACE IF NOT EXISTS uuid01009d585f434c91930a7b21c13eba5d
        // WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor'
        // : 2 };
        // CREATE KEYSPACE IF NOT EXISTS uuid39ee1eac4005486199415acd4e7860ab
        // WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor'
        // : 1 };
        // CREATE TABLE IF NOT EXISTS
        // uuid01009d585f434c91930a7b21c13eba5d.monkey_species (species
        // TEXT,common_name INT,population INT,average_size TEXT,
        // PRIMARY KEY (average_size, common_name, species ));
        // INSERT INTO uuid01009d585f434c91930a7b21c13eba5d.monkey_species
        // (species, common_name, population, average_size) VALUES
        // ('Monkey',0,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');
        // INSERT INTO uuid01009d585f434c91930a7b21c13eba5d.monkey_species
        // (species, common_name, population, average_size) VALUES
        // ('Monkey',1,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');
        // INSERT INTO uuid01009d585f434c91930a7b21c13eba5d.monkey_species
        // (species, common_name, population, average_size) VALUES
        // ('Monkey',2,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');
        // INSERT INTO uuid01009d585f434c91930a7b21c13eba5d.monkey_species
        // (population, average_size, common_name, species) VALUES
        // (2,'species',4,'species');
        // INSERT INTO uuid01009d585f434c91930a7b21c13eba5d.monkey_species
        // (species, common_name, population, average_size) VALUES
        // ('Monkey',4,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');
        // INSERT INTO uuid01009d585f434c91930a7b21c13eba5d.monkey_species
        // (species, common_name, population, average_size) VALUES
        // ('Monkey',5,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');
        // INSERT INTO uuid01009d585f434c91930a7b21c13eba5d.monkey_species
        // (species, common_name, population, average_size) VALUES
        // ('Monkey',6,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');
        // INSERT INTO uuid01009d585f434c91930a7b21c13eba5d.monkey_species
        // (species, common_name, population, average_size) VALUES
        // ('Monkey',7,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');
        // ALTER TABLE uuid01009d585f434c91930a7b21c13eba5d.monkey_species DROP
        // population ;

        List<Command> l = new LinkedList<>();

        CassandraState s = new CassandraState();

        // Command 0
        CREATE_KEYSPACE cmd0 = new CREATE_KEYSPACE(
                s, "myKS", 2, false);
        cmd0.updateState(s);
        l.add(cmd0);

        // Command 1
        List<Pair<String, ParameterType.ConcreteType>> columns = new ArrayList<>();
        columns.add(new Pair<>("species", CassandraTypes.TEXTType.instance));
        columns.add(new Pair<>("common_name", new INTType()));
        columns.add(new Pair<>("population", new INTType()));
        columns.add(
                new Pair<>("average_size", CassandraTypes.TEXTType.instance));

        List<String> primaryColumns = new ArrayList<>();
        primaryColumns.add("average_size TEXT");
        primaryColumns.add("common_name INT");
        primaryColumns.add("species TEXT");

        CREATE_TABLE cmd1 = new CREATE_TABLE(
                s, "myKS", "monkey_species", columns, primaryColumns, null);
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
        INSERT cmd2 = new INSERT(s, "myKS",
                "monkey_species", columns_INSERT, Values_INSERT);
        cmd2.updateState(s);
        l.add(cmd2);

        for (int i = 1; i < 3; i++) { // Difference: Cut off one command 9 -> 8
            Values_INSERT.remove(1);
            Values_INSERT.add(1, i);
            INSERT tmpCmd = new INSERT(s,
                    "myKS", "monkey_species", columns_INSERT, Values_INSERT);
            tmpCmd.updateState(s);
            l.add(tmpCmd);
        }

        List<String> columns_INSERT_ = new ArrayList<>();
        columns_INSERT_.add("population INT");
        columns_INSERT_.add("average_size TEXT");
        columns_INSERT_.add("common_name INT");
        columns_INSERT_.add("species TEXT");

        List<Object> Values_INSERT_ = new ArrayList<>();
        Values_INSERT_.add(2);
        Values_INSERT_.add("species");
        Values_INSERT_.add(2);
        Values_INSERT_.add("species");
        INSERT cmd3 = new INSERT(s, "myKS",
                "monkey_species", columns_INSERT_, Values_INSERT_);
        cmd3.updateState(s);
        l.add(cmd3);

        // Command 4-10
        for (int i = 4; i < 8; i++) { // Difference: Cut off one command 9 -> 8
            Values_INSERT.remove(1);
            Values_INSERT.add(1, i);
            INSERT tmpCmd = new INSERT(s,
                    "myKS", "monkey_species", columns_INSERT, Values_INSERT);
            tmpCmd.updateState(s);
            l.add(tmpCmd);
        }

        // Command 11
        ALTER_TABLE_DROP cmd11 = new ALTER_TABLE_DROP(
                s, "myKS", "monkey_species", "population INT");
        cmd11.updateState(s);
        l.add(cmd11);

        for (Command cmd : l) {
            System.out.println(cmd.constructCommandString());
        }

        CommandSequence commandSequence = new CommandSequence(l,
                CassandraCommand.cassandraCommandPool.commandClassList,
                CassandraCommand.cassandraCommandPool.createCommandClassList,
                CassandraState.class,
                s);
        return commandSequence;
    }

    public static CommandSequence cass14912() {
        String ksName = "myKS";
        String tableName = "legacy_ka_14912";

        List<Command> l = new LinkedList<>();

        CassandraState s = new CassandraState();

        // Command 0
        CREATE_KEYSPACE cmd0 = new CREATE_KEYSPACE(
                s, ksName, 2, false);
        cmd0.updateState(s);
        l.add(cmd0);

        // Command 1
        List<Pair<String, ParameterType.ConcreteType>> columns = new ArrayList<>();
        columns.add(new Pair<>("k", new INTType(0, 10)));
        columns.add(new Pair<>("v1",
                new ParameterType.ConcreteGenericTypeOne(SETType.instance,
                        CassandraTypes.TEXTType.instance)));
        columns.add(new Pair<>("v2", new CassandraTypes.TEXTType()));

        List<String> primaryColumns = new ArrayList<>();
        primaryColumns.add("k INT");

        CREATE_TABLE cmd1 = new CREATE_TABLE(
                s, ksName, tableName, columns, primaryColumns, null);
        cmd1.updateState(s);
        l.add(cmd1);

        // Command 2
        // 'Monkey', 0, 30, 'AAAAAAAAAAAAAAAAAAAAAAAAAAA'
        List<String> columns_INSERT = new ArrayList<>();
        columns_INSERT.add("k INT");
        columns_INSERT.add("v1 set<TEXT>");
        columns_INSERT.add("v2 TEXT");

        // Pick two commands, delete two bytes in each of them

        // Delete two bytes in the first INSERT
        List<Object> Values_INSERT = new ArrayList<>();
        Set<String> setValue = new HashSet<>();
        setValue.add("a");
        setValue.add("b");
        Values_INSERT.add(1);
        Values_INSERT.add(setValue);
        Values_INSERT.add("hh");
        INSERT cmd2 = new INSERT(s, ksName,
                tableName, columns_INSERT, Values_INSERT);
        cmd2.updateState(s);
        l.add(cmd2);

        ALTER_TABLE_DROP cmd11 = new ALTER_TABLE_DROP(
                s, ksName, tableName, "v1 set<TEXT>");
        cmd11.updateState(s);
        l.add(cmd11);

        // Command 11
        ALTER_TABLE_ADD cmd_add = new ALTER_TABLE_ADD(
                s, ksName, tableName, "v1",
                new CassandraTypes.TEXTType());
        cmd_add.updateState(s);
        l.add(cmd_add);

        for (Command cmd : l) {
            System.out.println(cmd.constructCommandString());
        }

        CommandSequence commandSequence = new CommandSequence(l,
                CassandraCommand.cassandraCommandPool.commandClassList,
                CassandraCommand.cassandraCommandPool.createCommandClassList,
                CassandraState.class,
                s);
        return commandSequence;
    }

    @Test
    public void t() throws IOException {
        ProcessBuilder pb = new ProcessBuilder(
                "/bin/sh", "-c", "rm -rf /Users/hanke/Desktop/dd");
        pb.start();
    }

    @Test
    public void testCreateTable() {
        CassandraState s = new CassandraState();

        // cmd1
        CREATE_KEYSPACE cmd0 = new CREATE_KEYSPACE(
                s, "myKS", 2, false);
        cmd0.updateState(s);

        // cmd2
        List<Pair<String, ParameterType.ConcreteType>> columns = new ArrayList<>();
        columns.add(new Pair<>("species", CassandraTypes.TEXTType.instance));
        columns.add(new Pair<>("common_name", new INTType()));
        columns.add(new Pair<>("50ms", new INTType()));
        columns.add(
                new Pair<>("average_size", CassandraTypes.TEXTType.instance));

        List<String> primaryColumns = new ArrayList<>();
        primaryColumns.add("species TEXT");
        primaryColumns.add("common_name INT");

        CREATE_TABLE cmd1 = new CREATE_TABLE(
                s, "myKS", "monkey_species", columns, primaryColumns, null);

        assert !cmd1.isValid(s);
    }
}
