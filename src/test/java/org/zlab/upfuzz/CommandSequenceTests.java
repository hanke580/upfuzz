package org.zlab.upfuzz;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.cassandra.CassandraCommands;
import org.zlab.upfuzz.cassandra.CassandraState;

public class CommandSequenceTests {

    @Test
    public void testSequenceGeneration() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {

        boolean useIdx = false;

        CassandraState state = new CassandraState();
        CommandSequence commandSequence = CommandSequence.generateSequence(CassandraCommands.commandClassList, CassandraCommands.createCommandClassList, CassandraState.class);

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
}
