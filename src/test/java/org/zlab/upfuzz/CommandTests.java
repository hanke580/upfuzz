package org.zlab.upfuzz;

import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.CustomExceptions;
import org.zlab.upfuzz.cassandra.CassandraCommands;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.utils.STRINGType;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
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

        List<Command> l = new LinkedList<>();

        l.add(cmd0);
        l.add(cmd1);

        try {
            FileOutputStream fileOut =
                    new FileOutputStream("/Users/hanke/Desktop/LIST.ser");
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(l);
            out.close();
            fileOut.close();
            System.out.println("Serialized data is saved in /Users/hanke/Desktop/LIST.ser");
        } catch (IOException i) {
            i.printStackTrace();
            return;
        }

//        cmd.mutate(s);

//        STRINGType.flipBit(null);
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
            List<Command> e = null;
            try {
                FileInputStream fileIn = new FileInputStream("/Users/hanke/Desktop/LIST.ser");
                ObjectInputStream in = new ObjectInputStream(fileIn);
                e = (List<Command>) in.readObject();
                in.close();
                fileIn.close();
            } catch (IOException i) {
                i.printStackTrace();
                return;
            } catch (ClassNotFoundException c) {
                System.out.println("Employee class not found");
                c.printStackTrace();
                return;
            }

            System.out.println();
    }

}
