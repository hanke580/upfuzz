package org.zlab.upfuzz.hbase;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.*;
import org.zlab.upfuzz.hbase.ddl.*;
import org.zlab.upfuzz.hbase.dml.*;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.hbase.rsgroup.GET_TABLE_RSGROUP;
import org.zlab.upfuzz.hbase.snapshot.*;
import org.zlab.upfuzz.hbase.tools.*;
import org.zlab.upfuzz.utils.Utilities;

public class CommandTest extends AbstractTest {
    static Logger logger = LogManager.getLogger(CommandTest.class);

    @BeforeAll
    public static void setUp() {
        Config.instance.system = "hbase";
    }

    @Test
    public void test01() {
        HBaseState s = new HBaseState();

        CREATE cmd01 = new CREATE(s);
        String cmd01str = cmd01.constructCommandString();
        // System.out.println(cmd01str);
        cmd01.updateState(s);

        ALTER_ADD_FAMILY cmd02 = new ALTER_ADD_FAMILY(s);
        String cmd02str = cmd02.constructCommandString();
        // System.out.println(cmd02str);
        cmd02.updateState(s);

        ALTER_DELETE_FAMILY cmd03 = new ALTER_DELETE_FAMILY(s);
        String cmd03str = cmd03.constructCommandString();
        // System.out.println(cmd03str);
        cmd03.updateState(s);

        PUT_NEW cmd07 = new PUT_NEW(s);
        String cmd07str = cmd07.constructCommandString();
        // System.out.println(cmd07str);
        cmd07.updateState(s);

        PUT_NEW cmd04 = new PUT_NEW(s);
        String cmd04str = cmd04.constructCommandString();
        // System.out.println(cmd04str);
        cmd04.updateState(s);

        // PUT_NEW_ITEM cmd05 = new PUT_NEW_ITEM(s);
        // String cmd05str = cmd05.constructCommandString();
        // System.out.println(cmd05str);
        // cmd05.updateState(s);
    }

    @Test
    public void CommandTests() throws Exception {
        test01();
    }

    @Test
    public void testScanTimeMask() {
        String a = "ROW  COLUMN+CELL\n" +
                "0 row(s)\n" +
                "Took 0.0116 seconds";
        // If a string matches Took 0.0116 seconds, remove it
        String b = Utilities.maskScanTime(a);
        // System.out.println(b);
        assert !b.contains("Took 0.0116 seconds");
    }

    @Test
    public void testCREATE() {
        HBaseState s = new HBaseState();

        CREATE cmd01 = new CREATE(s);
        String cmd01str = cmd01.constructCommandString();
        System.out.println(cmd01str);
        cmd01.updateState(s);
    }

    @Test
    public void testAlter() {
        try {
            HBaseState s = execInitCommands();
            Command cmd = new ALTER_CF_OPTION(s);
            cmd.updateState(s);
            System.out.println(cmd.constructCommandString());
        } catch (CustomExceptions.EmptyCollectionException e) {
            // Exception is normal, but could be avoided
        }
    }

    @Test
    public void testAlterStatus() {
        HBaseState s = execInitCommands();
        Command cmd = new ALTER_STATUS(s);
        cmd.updateState(s);
        System.out.println(cmd.constructCommandString());
    }

    @Test
    public void testCloneTableSchema() {
        HBaseState s = execInitCommands();
        Command cmd = new CLONE_TABLE_SCHEMA(s);
        cmd.updateState(s);
        System.out.println(cmd.constructCommandString());
    }

    @Test
    public void testPutModify() {
        try {
            HBaseState s = execInitCommands();
            Command cmd = new PUT_MODIFY(s);
            cmd.updateState(s);
            System.out.println(cmd.constructCommandString());
        } catch (CustomExceptions.EmptyCollectionException e) {
            // Exception is normal, but could be avoided
        }
    }

    @Test
    public void test() {
        try {
            HBaseState s = execInitCommands();

            Command cmd = new FLUSH(s);
            cmd.updateState(s);
            System.out.println(cmd.constructCommandString());

            // cmd = new ALTER_NAMESPACE(s);
            // cmd.updateState(s);
            // System.out.println(cmd.constructCommandString());
        } catch (CustomExceptions.EmptyCollectionException e) {
            // Exception is normal, but could be avoided
            e.printStackTrace();
        }
    }

    @Test
    public void testPut() {
        HBaseState s = new HBaseState();
        Command c = new CREATE(s);
        c.updateState(s);
        System.out.println(c.constructCommandString());

        Command c2 = new COMPACT_RS(s);
        c2.updateState(s);
        System.out.println(c2.constructCommandString());
    }

    public static HBaseState execInitCommands() {
        HBaseState s = new HBaseState();

        Command c = new CREATE(s);
        c.updateState(s);
        System.out.println(c.constructCommandString());

        c = new CREATE(s);
        c.updateState(s);
        System.out.println(c.constructCommandString());

        Command c2 = new PUT_NEW(s);
        c2.updateState(s);
        System.out.println(c2.constructCommandString());
        Command c3 = new PUT_NEW(s);
        c3.updateState(s);
        System.out.println(c3.constructCommandString());
        Command c4 = new PUT_NEW(s);
        c4.updateState(s);
        System.out.println(c4.constructCommandString());
        return s;
    }

    @Test
    public void testCOMMON() {
        HBaseState s = execInitCommands();
        try {

            Command cmd = new SNAPSHOT(s);
            cmd.updateState(s);
            System.out.println(cmd.constructCommandString());

            cmd = new GET_TABLE_RSGROUP(s);
            cmd.updateState(s);
            System.out.println(cmd.constructCommandString());

        } catch (CustomExceptions.EmptyCollectionException e) {
            // Exception is normal, but could be avoided
            e.printStackTrace();
        }
    }

    @Test
    public void test1() {
        String strs = "disable_exceed_throttle_quota, disable_rpc_throttle, enable_exceed_throttle_quota, enable_rpc_throttle, list_quota_snapshots, list_quota_table_sizes, list_quotas, list_snapshot_sizes, set_quota";
        for (String str : strs.strip().split(",")) {
            System.out.println(str + ", " + str.toUpperCase());
        }
    }

}
