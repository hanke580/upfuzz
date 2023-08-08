package org.zlab.upfuzz.cassandra;

import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.AbstractTest;
import org.zlab.upfuzz.cassandra.cqlcommands.*;
import org.zlab.upfuzz.cassandra.nodetool.*;

import java.math.BigInteger;
import java.util.Random;

public class NodetoolCommandTest extends AbstractTest {
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

        CREATE_INDEX cmd3 = new CREATE_INDEX(s);
        cmd3.updateState(s);
        System.out.println(cmd3.constructCommandString());

        REMOVENODE cmd4 = new REMOVENODE(s);
        cmd4.updateState(s);

        System.out.println(cmd4.constructCommandString());
    }

    @Test
    public void test() {
        BigInteger b = new BigInteger(63, new Random());
        System.out.println(b);
    }

}
