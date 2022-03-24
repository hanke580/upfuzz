package org.zlab.upfuzz.hdfs;

import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.hdfs.HDFSCommands.setacl;


public class TestHDFSCommands {
    protected void setUp() {

    }

    @Test
    public void testSetACL() {
        Command setaclCommand = new setacl();
        System.out.println(setaclCommand.constructCommandString());

    }
}
