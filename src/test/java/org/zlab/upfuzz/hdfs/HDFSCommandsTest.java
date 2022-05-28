/* (C)2022 */
package org.zlab.upfuzz.hdfs;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.hdfs.dfscommands.PutCommand;
// import org.zlab.upfuzz.hdfs.HDFSCommands.setacl;
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
}
