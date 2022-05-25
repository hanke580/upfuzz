package org.zlab.upfuzz;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.cassandra.CassandraCommands;
import org.zlab.upfuzz.cassandra.CassandraState;

public class CommandSequenceTests {

    @Test
    public void testSequenceGeneration() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {

        boolean useIdx = false;

        CassandraState state = new CassandraState();
        CommandSequence commandSequence = CommandSequence.generateSequence(CassandraCommands.commandClassList, CassandraCommands.createCommandClassList, CassandraState.class, null);

        List<String> l = commandSequence.getCommandStringList();
        for (int i = 0; i < l.size(); i++) {
            if (useIdx)
                System.out.println("[" + i + "]" + "\t" + l.get(i));
            else
                System.out.println(l.get(i));
        }
        System.out.println("command size = " + l.size());

        System.out.println("\n-----------Sequence Mutation Start-----------");
        commandSequence.mutate();
        System.out.println("-----------Sequence Mutation End-----------\n");

        l = commandSequence.getCommandStringList();
        for (int i = 0; i < l.size(); i++) {
            if (useIdx)
                System.out.println("[" + i + "]" + "\t" + l.get(i));
            else
                System.out.println(l.get(i));
        }
        System.out.println("command size = " + l.size());

    }

    @Test
    public void testMutation() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        CommandSequence commandSequence = CommandTests.cass13939CommandSequence();
        boolean mutateStatus = commandSequence.mutate();
        if (!mutateStatus) {
            System.out.println("Mutate failed");
        } else {
            System.out.println("After Mutation");
            for (String cmdStr : commandSequence.getCommandStringList()) {
                System.out.println(cmdStr);
            }
        }
    }

    @Test
    public void genUUID() {
        final String uuid = UUID.randomUUID().toString().replace("-", "");
        System.out.println("uuid = " + uuid);
    }

    @Test
    public void testTypeIsValidCheck() {
        CommandSequence commandSequence = CommandTests.cass13939CommandSequence();

        try {
            commandSequence.mutate();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        for (String cmdStr : commandSequence.getCommandStringList()) {
            System.out.println(cmdStr);
        }
    }

    @Test
    public void test() {
        Map<Set<Integer>, Set<Set<Integer>>> orders = new HashMap<>();

        Set<Integer> cmd1Pos1 = new HashSet<>();
        Set<Integer> cmd2Pos1 = new HashSet<>();
        cmd1Pos1.add(1);
        cmd1Pos1.add(2);
        cmd2Pos1.add(4);


        Set<Integer> cmd1Pos2 = new HashSet<>();
        Set<Integer> cmd2Pos2 = new HashSet<>();

        cmd1Pos2.add(1);
        cmd1Pos2.add(2);
        cmd2Pos2.add(4);

        orders.put(cmd1Pos1, new HashSet<>());
        orders.put(cmd1Pos2, new HashSet<>());

        orders.get(cmd1Pos1).add(cmd2Pos1);
        orders.get(cmd1Pos1).add(cmd2Pos2);

//
//
//        orders.get(cmd1Pos1).add(cmd2Pos1);
//
//        if (orders.containsKey(cmd1Pos2)) {
//            System.out.println("TRUE");
//        }
//        orders.put(cmd1Pos2, new HashSet<>());
//        orders.get(cmd1Pos2).add(cmd2Pos2);

        System.out.println(orders);

    }

}
