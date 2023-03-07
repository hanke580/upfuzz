package org.zlab.upfuzz.hbase;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.hbase.DataDefinitionCommands.CREATE;
import org.zlab.upfuzz.hbase.DataDefinitionCommands.LIST;
import org.zlab.upfuzz.hbase.hbasecommands.*;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.utils.INTType;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

public class CommandTests {

    @BeforeAll
    public static void setUp() {
        Config config = new Config();
        Config.instance.system = "hbase";
    }

    @Test
    public void test01(){
        HBaseState s = new HBaseState();

        CREATE cmd01 = new CREATE(s);
        String cmd01str = cmd01.constructCommandString();
        System.out.println(cmd01str);
        cmd01.updateState(s);
    }

    @Test
    public void CommandTests()throws Exception{
        test01();
    }

}
