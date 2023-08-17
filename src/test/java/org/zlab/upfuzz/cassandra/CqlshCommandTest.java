package org.zlab.upfuzz.cassandra;

import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.AbstractTest;
import org.zlab.upfuzz.CustomExceptions;
import org.zlab.upfuzz.cassandra.cqlcommands.*;

public class CqlshCommandTest extends AbstractTest {
    @Test
    public void testCreateKSCommandGeneration() {
        CassandraState s = new CassandraState();
        CREATE_KEYSPACE cmd0 = new CREATE_KEYSPACE(
                s);
        cmd0.updateState(s);
        System.out.println(cmd0.constructCommandString());

        DROP_KEYSPACE cmd1 = new DROP_KEYSPACE(
                s);
        cmd1.updateState(s);
        System.out.println(cmd1.constructCommandString());
    }

    @Test
    public void testALTER_KEYSPACECommandGeneration() {
        CassandraState s = new CassandraState();
        CREATE_KEYSPACE cmd0 = new CREATE_KEYSPACE(
                s);
        cmd0.updateState(s);
        System.out.println(cmd0.constructCommandString());

        ALTER_KEYSPACE cmd1 = new ALTER_KEYSPACE(
                s);
        cmd1.updateState(s);
        System.out.println(cmd1.constructCommandString());
    }

    @Test
    public void testCREATE_TABLECommandGeneration() {

        CassandraState s = new CassandraState();

        CREATE_KEYSPACE cmd0 = new CREATE_KEYSPACE(
                s);
        cmd0.updateState(s);
        System.out.println(cmd0.constructCommandString());

        CREATE_TABLE cmd1 = new CREATE_TABLE(
                s);
        cmd1.updateState(s);
        System.out.println(cmd1.constructCommandString());

        DROP_TABLE cmd2 = new DROP_TABLE(s);
        cmd2.updateState(s);
        System.out.println(cmd2.constructCommandString());
    }

    @Test
    public void testINSERTCommandGeneration() {

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

        CREATE_TYPE cmd3 = new CREATE_TYPE(s);
        cmd3.updateState(s);

        ALTER_TABLE cmd4 = new ALTER_TABLE(s);
        cmd4.updateState(s);

        System.out.println(cmd4.constructCommandString());
    }

    // FIXME drop primary key => infinate loop
    // @Test
    public void testALTER_TABLE_DROPCommandGeneration() {

        CassandraState s = new CassandraState();

        CREATE_TABLE cmd1 = new CREATE_TABLE(
                s);
        cmd1.updateState(s);

        System.out.println(cmd1.constructCommandString());

        try {
            ALTER_TABLE_DROP cmd2 = new ALTER_TABLE_DROP(
                    s);
            cmd2.updateState(s);
            System.out.println(cmd2.constructCommandString());
        } catch (CustomExceptions.PredicateUnSatisfyException e) {
            e.printStackTrace();
            System.out.println(
                    "Predicate is not satisfy, this command cannot be correctly constructed");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(
                    "Exception is thrown during the construction of the current command");
        }
    }

    @Test
    public void testDELETECommandGeneration() {

        CassandraState s = new CassandraState();

        CREATE_KEYSPACE cmd0 = new CREATE_KEYSPACE(
                s);
        cmd0.updateState(s);

        CREATE_TABLE cmd1 = new CREATE_TABLE(
                s);
        cmd1.updateState(s);

        System.out.println(cmd1.constructCommandString());

        try {
            DELETE cmd2 = new DELETE(s);
            cmd2.updateState(s);
            System.out.println(cmd2.constructCommandString());
        } catch (CustomExceptions.PredicateUnSatisfyException e) {
            e.printStackTrace();
            System.out.println(
                    "Predicate is not satisfy, this command cannot be correctly constructed");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(
                    "Exception is thrown during the construction of the current command");
        }
    }

    @Test
    public void testCREATE_INDEXCommandGeneration() {

        CassandraState s = new CassandraState();

        CREATE_KEYSPACE cmd0 = new CREATE_KEYSPACE(
                s);
        cmd0.updateState(s);

        CREATE_TABLE cmd1 = new CREATE_TABLE(
                s);
        cmd1.updateState(s);

        CREATE_INDEX cmd2 = new CREATE_INDEX(
                s);
        cmd2.updateState(s);

        DROP_INDEX cmd3 = new DROP_INDEX(s);
        cmd3.updateState(s);

        System.out.println(cmd0);
        System.out.println(cmd1);
        System.out.println(cmd2);
        System.out.println(cmd3);
    }

    @Test
    public void testCREATE_UDTCommandGeneration() {

        CassandraState s = new CassandraState();

        CREATE_KEYSPACE cmd0 = new CREATE_KEYSPACE(
                s);
        cmd0.updateState(s);

        CREATE_TABLE cmd1 = new CREATE_TABLE(
                s);
        cmd1.updateState(s);

        CREATE_TYPE cmd2 = new CREATE_TYPE(
                s);
        cmd2.updateState(s);

        DROP_TYPE cmd3 = new DROP_TYPE(s);
        cmd3.updateState(s);

        USE cmd4 = new USE(s);
        cmd4.updateState(s);

        System.out.println(cmd0);
        System.out.println(cmd1);
        System.out.println(cmd2);
        System.out.println(cmd3);
        System.out.println(cmd4);
    }

}
