package org.zlab.upfuzz.hbase;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.*;
import org.zlab.upfuzz.hbase.DataDefinitionCommands.ALTER_ADD_FAMILY;
import org.zlab.upfuzz.hbase.DataDefinitionCommands.ALTER_DELETE_FAMILY;
import org.zlab.upfuzz.hbase.DataManipulationCommands.PUT_NEW_COLUMN;
import org.zlab.upfuzz.hbase.DataManipulationCommands.*;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.hbase.DataDefinitionCommands.CREATE;
import org.zlab.upfuzz.hbase.DataDefinitionCommands.LIST;
import org.zlab.upfuzz.hbase.hbasecommands.*;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.utils.INTType;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

import javax.swing.plaf.synth.SynthTextAreaUI;

public class CommandTests extends AbstractTest {

    @BeforeAll
    public static void setUp() {
        Config config = new Config();
        Config.instance.system = "hbase";
    }

    @Test
    public void test01() {
        HBaseState s = new HBaseState();

        CREATE cmd01 = new CREATE(s);
        String cmd01str = cmd01.constructCommandString();
        System.out.println(cmd01str);
        cmd01.updateState(s);

        ALTER_ADD_FAMILY cmd02 = new ALTER_ADD_FAMILY(s);
        String cmd02str = cmd02.constructCommandString();
        System.out.println(cmd02str);
        cmd02.updateState(s);

        ALTER_DELETE_FAMILY cmd03 = new ALTER_DELETE_FAMILY(s);
        String cmd03str = cmd03.constructCommandString();
        System.out.println(cmd03str);
        cmd03.updateState(s);

        PUT_NEW_COLUMN_and_NEW_ITEM cmd07 = new PUT_NEW_COLUMN_and_NEW_ITEM(s);
        String cmd07str = cmd07.constructCommandString();
        System.out.println(cmd07str);
        cmd07.updateState(s);

        PUT_NEW_COLUMN cmd04 = new PUT_NEW_COLUMN(s);
        String cmd04str = cmd04.constructCommandString();
        System.out.println(cmd04str);
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

}
