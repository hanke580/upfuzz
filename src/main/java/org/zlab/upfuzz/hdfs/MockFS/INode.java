package org.zlab.upfuzz.hdfs.MockFS;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

public class INode implements Serializable {

    public INode(INode inode) {
        this.i_gid = inode.i_gid;
        this.file_path = inode.file_path;
        this.file_name = inode.file_name;
        this.i_type = inode.i_type;
        this.i_uid = inode.i_uid;
        this.i_atime = inode.i_atime;
        this.i_ctime = inode.i_ctime;
        this.i_mtime = inode.i_mtime;
        this.i_dtime = inode.i_dtime;
        this.i_file_acl_lo = inode.i_file_acl_lo;
        this.i_size_high = inode.i_size_high;
    }

    public INode(INode dir, String name, IType type) {
        this(PathUtils.join(dir, name), type, 1000, 1000, 0660);
    }

    public INode(String filepath, IType type, int uid, int gid, int acl) {
        setPath(filepath);
        i_type = type;
        i_uid = uid;
        i_gid = gid;
        i_file_acl_lo = acl;
    }

    public void setPath(String filepath) {
        file_path = filepath;
        Path filename = Paths.get(filepath).getFileName();
        file_name = filename == null ? "" : filename.toString();
    }

    public String file_path;
    public String file_name;

    public Boolean isFile() {
        return i_type == IType.File;
    }

    public Boolean isDir() {
        return i_type == IType.Dir;
    }

    IType i_type; /* File or Directory */

    // int i_mode; /* File mode */

    int i_uid; /* Low 16 bits of Owner Uid */

    int i_gid; /* Low 16 bits of Group Id */

    // int i_size_lo; /* Size in bytes */

    int i_atime; /* Access time */

    int i_ctime; /* Inode Change time */

    int i_mtime; /* Modification time */

    int i_dtime; /* Deletion Time */

    // int i_links_count; /* Links count */

    // int i_blocks_lo; /* Blocks count */

    // int i_flags; /* File flags */

    int i_file_acl_lo; /* File ACL */

    // int i_block[EXT4_N_BLOCKS]; /* Pointers to blocks */

    // int i_generation; /* File version (for NFS) */

    int i_size_high;

    enum IType {
        File, Dir;
    }
}
