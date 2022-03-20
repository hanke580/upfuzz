package org.zlab.upfuzz.hdfs;

import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.hdfs.HDFSCommands.setacl;

import junit.framework.*;
import junit.textui.*;

public class TestHDFSCommands extends TestCase {
    protected void setUp() {

    }

    @Test
    public void testSetACL() {
        Command setaclCommand = new setacl();
        System.out.println(setaclCommand.constructCommandString());

    }
}
