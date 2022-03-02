import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.cassandra.CassandraCommands;
import org.zlab.upfuzz.cassandra.CassandraState;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class CommandSequenceTests {
    @Test
    public void testSequenceGeneration() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {

        CommandSequence commandSequence = CommandSequence.generateSequence(CassandraCommands.commandClassList, CassandraState.class);
        List<String> l = commandSequence.getCommandStringList();
        for (String commandString : l) {
            System.out.println(commandString);
        }
        System.out.println(l.size());

        System.out.println("\n\n");

        commandSequence.mutate();
        l = commandSequence.getCommandStringList();
        for (String commandString : l) {
            System.out.println(commandString);
        }
        System.out.println(l.size());

    }
}
