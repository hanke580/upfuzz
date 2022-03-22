import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.cassandra.CassandraCommands;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.utils.STRINGType;

import java.lang.reflect.InvocationTargetException;

public class CommandTests {

    @Test
    public void testCreateCommandGeneration() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {

        CassandraState s = new CassandraState();
        CassandraCommands.CREATETABLE cmd = new CassandraCommands.CREATETABLE(s);
        System.out.println(cmd.constructCommandString());
//        cmd.mutate(s);

//        STRINGType.flipBit(null);
    }


    @Test
    public void testINSERTCommandGeneration() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {

        CassandraState s = new CassandraState();

        CassandraCommands.CREATETABLE cmd1 = new CassandraCommands.CREATETABLE(s);
        cmd1.updateState(s);

        System.out.println(cmd1.constructCommandString());


        CassandraCommands.INSERT cmd2 = new CassandraCommands.INSERT(s);
        System.out.println(cmd2.constructCommandString());
    }


    @Test
    public void testALTER_TABLE_DROPCommandGeneration() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {

        CassandraState s = new CassandraState();

        CassandraCommands.CREATETABLE cmd1 = new CassandraCommands.CREATETABLE(s);
        cmd1.updateState(s);

        System.out.println(cmd1.constructCommandString());


        CassandraCommands.ALTER_TABLE_DROP cmd2 = new CassandraCommands.ALTER_TABLE_DROP(s);
        cmd2.updateState(s);
        System.out.println(cmd2.constructCommandString());
    }
}
