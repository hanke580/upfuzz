package org.zlab.upfuzz.hdfs;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.hdfs.dfscommands.CountCommand;
import org.zlab.upfuzz.hdfs.dfscommands.PutCommand;
// import org.zlab.upfuzz.hdfs.HDFSCommands.setacl;
import org.zlab.upfuzz.hdfs.dfscommands.SetSpaceQuotaCommand;
import org.zlab.upfuzz.hdfs.dfscommands.SetaclCommand;

public class HDFSCommandsTest {

    static HdfsState hdfsState;

    @BeforeAll
    public static void initAll() {
        hdfsState = new HdfsState();
        // hdfsState.randomize(0.6);
        System.out.println("root " + hdfsState.lfs.localRoot);
    }

    @Test
    public void testSetACL() {
        Command setaclCommand = new SetaclCommand();
        System.out.println(setaclCommand.constructCommandString());
    }

    @Test
    public void testPut() {
        Command putCommand = new PutCommand(hdfsState);
        System.out.println(putCommand.constructCommandString());
    }

    @Test
    public void testSetSpaceQuotaCommand() {
        Command setSpaceQuotaCommand = new SetSpaceQuotaCommand(hdfsState);
        System.out.println(setSpaceQuotaCommand.constructCommandString());
    }

    @Test
    public void testCountCommandCommand() {
        Command countCommand = new CountCommand(hdfsState);
        System.out.println(countCommand.constructCommandString());
    }

}
