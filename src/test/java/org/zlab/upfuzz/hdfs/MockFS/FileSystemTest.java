package org.zlab.upfuzz.hdfs.MockFS;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.hdfs.MockFS.INode.IType;

import junit.framework.TestCase;

public class FileSystemTest extends TestCase {
    protected void setUp() {

    }

    @Test
    public void testPath() {
        Path p = Paths.get("/home/yayu/Project/Upgrade-Fuzzing/upfuzz/src/test/java/org/zlab/upfuzz/hdfs/MockFS/FileSystemTest.java");
        while( true ){
            p = p.getParent();
            System.out.println(p);
            if( p == null ){
                break;
            }
        }
    }

    @Test
    public void testCreateFile() {
        INode fileNode = new INode("test", IType.File, 1000, 1000, 0660);
        System.out.printf("hash: %16x\noffset: %16x\n", fileNode.hashCode(), fileNode.hashCode() >>> 16);
    }
}
