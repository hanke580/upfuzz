package org.zlab.upfuzz.hdfs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.AbstractTest;
import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.hdfs.dfscommands.*;

public class CommandTest extends AbstractTest {
    static Logger logger = LogManager
            .getLogger(CommandTest.class);

    @Test
    public void testSetACL() {
        HdfsState hdfsState = new HdfsState();
        Command setaclCommand = new SetaclCommand(hdfsState);
        System.out.println(setaclCommand.constructCommandString());
    }

    @Test
    public void testPut() {
        HdfsState hdfsState = new HdfsState();
        Command putCommand = new PutCommand(hdfsState);
        putCommand.updateState(hdfsState);
        System.out.println(putCommand.constructCommandString());
    }

    @Test
    public void testSetSpaceQuotaCommand() {
        HdfsState hdfsState = new HdfsState();
        Command setSpaceQuotaCommand = new SetSpaceQuotaCommand(hdfsState);
        System.out.println(setSpaceQuotaCommand.constructCommandString());
    }

    @Test
    public void testCountCommandCommand() {
        HdfsState hdfsState = new HdfsState();
        Command countCommand = new CountCommand(hdfsState);
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
        Command catCommand = new CatCommand(hdfsState);
        System.out.println(catCommand.constructCommandString());
    }

    @Test
    public void testMv() {
        HdfsState hdfsState = new HdfsState();
        Command touchz = new Touchz(hdfsState);
        touchz.updateState(hdfsState);
        logger.info(touchz);
        Command mvCommand = new MvCommand(hdfsState);
        mvCommand.updateState(hdfsState);
        logger.info(mvCommand);
    }

}
