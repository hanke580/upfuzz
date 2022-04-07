package org.zlab.upfuzz;

import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.CustomExceptions;
import org.zlab.upfuzz.cassandra.CassandraCommands;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.cassandra.CassandraTypes;
import org.zlab.upfuzz.utils.INTType;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.STRINGType;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CommandTests {

    @Test
    public void testCreateKSCommandGeneration(){
        CassandraState s = new CassandraState();
        CassandraCommands.CREAT_KEYSPACE cmd = new CassandraCommands.CREAT_KEYSPACE(s);
        cmd.updateState(s);
        System.out.println(cmd.constructCommandString());
    }

    @Test
    public void testCreateTableCommandGeneration() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {

        CassandraState s = new CassandraState();

        CassandraCommands.CREAT_KEYSPACE cmd0 = new CassandraCommands.CREAT_KEYSPACE(s);
        cmd0.updateState(s);
        System.out.println(cmd0.constructCommandString());


        CassandraCommands.CREATETABLE cmd1 = new CassandraCommands.CREATETABLE(s);
        cmd1.updateState(s);
        System.out.println(cmd1.constructCommandString());

    }

    @Test
    public void testINSERTCommandGeneration() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {

        CassandraState s = new CassandraState();

        CassandraCommands.CREAT_KEYSPACE cmd0 = new CassandraCommands.CREAT_KEYSPACE(s);
        cmd0.updateState(s);
        System.out.println(cmd0.constructCommandString());

        CassandraCommands.CREATETABLE cmd1 = new CassandraCommands.CREATETABLE(s);
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

        CassandraCommands.CREATETABLE cmd1 = new CassandraCommands.CREATETABLE(s);
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

        CassandraCommands.CREATETABLE cmd1 = new CassandraCommands.CREATETABLE(s);
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
    public void testSerializable() {
        CassandraState s = new CassandraState();

        CassandraCommands.CREAT_KEYSPACE cmd0 = new CassandraCommands.CREAT_KEYSPACE(s);
        cmd0.updateState(s);
        System.out.println(cmd0.constructCommandString());

        CassandraCommands.CREATETABLE cmd1 = new CassandraCommands.CREATETABLE(s);
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
    public void testCreateKSCommandWithInitialValue() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
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

        CassandraCommands.CREATETABLE cmd1 = new CassandraCommands.CREATETABLE(s, "myKS", "monkey_species", columns, primaryColumns, null);
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

        CommandSequence commandSequence = new CommandSequence(l, CassandraCommands.commandClassList, CassandraCommands.createCommandClassList, CassandraState.class, null);
        CommandSequence validationCommandSequence = new CommandSequence(l, CassandraCommands.readCommandClassList, CassandraCommands.createCommandClassList, CassandraState.class, commandSequence.state);

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
    public void testSELECTCommandGeneration() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {

        CassandraState s = new CassandraState();

        CassandraCommands.CREAT_KEYSPACE cmd0 = new CassandraCommands.CREAT_KEYSPACE(s);
        cmd0.updateState(s);
        System.out.println(cmd0.constructCommandString());

        CassandraCommands.CREATETABLE cmd1 = new CassandraCommands.CREATETABLE(s);
        cmd1.updateState(s);
        System.out.println(cmd1.constructCommandString());

        CassandraCommands.INSERT cmd2 = new CassandraCommands.INSERT(s);
        cmd2.updateState(s);
        System.out.println(cmd2.constructCommandString());

        CassandraCommands.SELECT cmd3 = new CassandraCommands.SELECT(s);
        cmd2.updateState(s);
        System.out.println(cmd3.constructCommandString());
    }

}
