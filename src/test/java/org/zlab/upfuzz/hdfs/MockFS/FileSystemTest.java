package org.zlab.upfuzz.hdfs.MockFS;

import java.nio.file.Path;
import java.nio.file.Paths;
import junit.framework.TestCase;
import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.hdfs.MockFS.INode.IType;

public class FileSystemTest extends TestCase {
    protected void setUp() {
    }

    // @Test
    public void testPath() {
        Path p = Paths.get(
                "/PATH/TO/MockFS/FileSystemTest.java");
        while (true) {
            p = p.getParent();
            System.out.println(p);
            if (p == null) {
                break;
            }
        }
    }

    @Test
    public void testCreateFile() {
        INode fileNode = new INode("test", IType.File, 1000, 1000, 0660);
        System.out.printf("hash: %16x\noffset: %16x\n", fileNode.hashCode(),
                fileNode.hashCode() >>> 16);
    }
}
