package org.zlab.upfuzz.hdfs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.AbstractTest;
import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.hdfs.dfsadmin.SetSpaceQuota;
import org.zlab.upfuzz.hdfs.dfs.*;

public class CommandTest extends AbstractTest {
    static Logger logger = LogManager
            .getLogger(CommandTest.class);

    @Test
    public void testSetACL() {
        HdfsState hdfsState = new HdfsState();
        Command setaclCommand = new Setacl(hdfsState);
        System.out.println(setaclCommand.constructCommandString());
    }

    @Test
    public void testGetACL() {
        HdfsState hdfsState = new HdfsState();
        Command cmd = new Getfacl(hdfsState);
        System.out.println(cmd.constructCommandString());
    }

    @Test
    public void testGetfattr() {
        HdfsState hdfsState = new HdfsState();
        Command cmd = new Getfattr(hdfsState);
        System.out.println(cmd.constructCommandString());
    }

    @Test
    public void testStat() {
        HdfsState hdfsState = new HdfsState();
        Command cmd1 = new Touchz(hdfsState);
        cmd1.updateState(hdfsState);
        Command cmd2 = new Stat(hdfsState);
        cmd2.updateState(hdfsState);
        System.out.println(cmd2.constructCommandString());
    }

    @Test
    public void testChown() {
        HdfsState hdfsState = new HdfsState();
        Command cmd = new Chown(hdfsState);
        System.out.println(cmd.constructCommandString());
    }

    @Test
    public void testPut() {
        HdfsState hdfsState = new HdfsState();
        Command putCommand = new Put(hdfsState);
        putCommand.updateState(hdfsState);
        System.out.println(putCommand.constructCommandString());
    }

    @Test
    public void testSetSpaceQuotaCommand() {
        HdfsState hdfsState = new HdfsState();
        Command setSpaceQuotaCommand = new SetSpaceQuota(hdfsState);
        System.out.println(setSpaceQuotaCommand.constructCommandString());
    }

    @Test
    public void testCountCommandCommand() {
        HdfsState hdfsState = new HdfsState();
        Command countCommand = new Count(hdfsState);
        System.out.println(countCommand.constructCommandString());
    }

    @Test
    public void testMkdirCommandCommand() {
        HdfsState hdfsState = new HdfsState();
        Command countCommand = new Mkdir(hdfsState);
        countCommand.updateState(hdfsState);
        System.out.println(countCommand.constructCommandString());
    }

    @Test
    public void testCat() {
        HdfsState hdfsState = new HdfsState();
        Command touchCommand = new Touchz(hdfsState);
        touchCommand.updateState(hdfsState);
        Command catCommand = new Cat(hdfsState);
        System.out.println(catCommand.constructCommandString());
    }

    @Test
    public void testMv() {
        HdfsState hdfsState = new HdfsState();
        Command touchz = new Touchz(hdfsState);
        touchz.updateState(hdfsState);
        logger.info(touchz);
        Command mvCommand = new Mv(hdfsState);
        mvCommand.updateState(hdfsState);
        logger.info(mvCommand);
    }

}
