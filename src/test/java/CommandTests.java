import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.cassandra.CassandraCommands;
import org.zlab.upfuzz.cassandra.CassandraState;

public class CommandTests {

    @Test
    public void testCreateCommandGeneration() {

        CassandraState s = new CassandraState();
        Command cmd = new CassandraCommands.CREATETABLE(s);
        System.out.println(cmd.constructCommandString());

    }
}
