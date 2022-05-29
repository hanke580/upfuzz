package org.zlab.upfuzz.hdfs.MockFS;

public class INode {

    int i_mode; /* File mode */

    int i_uid; /* Low 16 bits of Owner Uid */

    int i_gid; /* Low 16 bits of Group Id */

    int i_size_lo; /* Size in bytes */

    int i_atime; /* Access time */

    int i_ctime; /* Inode Change time */

    int i_mtime; /* Modification time */

    int i_dtime; /* Deletion Time */

    int i_links_count; /* Links count */

    int i_blocks_lo; /* Blocks count */

    int i_flags; /* File flags */

    int i_file_acl_lo; /* File ACL */

    // int i_block[EXT4_N_BLOCKS]; /* Pointers to blocks */

    // int i_generation; /* File version (for NFS) */

    int i_size_high;
}
