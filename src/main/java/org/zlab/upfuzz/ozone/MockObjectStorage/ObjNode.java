package org.zlab.upfuzz.ozone.MockObjectStorage;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ObjNode implements Serializable {

    public ObjNode(ObjNode onode) {
        this.i_gid = onode.i_gid;
        this.file_path = onode.file_path;
        this.file_name = onode.file_name;
        this.o_type = onode.o_type;
        this.i_uid = onode.i_uid;
        this.i_atime = onode.i_atime;
        this.i_ctime = onode.i_ctime;
        this.i_mtime = onode.i_mtime;
        this.i_dtime = onode.i_dtime;
        this.i_file_acl_lo = onode.i_file_acl_lo;
        this.i_size_high = onode.i_size_high;
    }

    public ObjNode(ObjNode dir, String name, ObjType type) {
        this(PathUtils.join(dir, name), type, 1000, 1000, 0660);
    }

    public ObjNode(String filepath, ObjType type, int uid, int gid, int acl) {
        setPath(filepath);
        o_type = type;
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

    public Boolean isVolume() {
        return o_type == ObjType.Volume;
    }

    public Boolean isBucket() {
        return o_type == ObjType.Bucket;
    }

    public Boolean isKey() {
        return o_type == ObjType.Key;
    }

    ObjType o_type; /* File or Directory */

    int i_uid; /* Low 16 bits of Owner Uid */

    int i_gid; /* Low 16 bits of Group Id */

    int i_atime; /* Access time */

    int i_ctime; /* Inode Change time */

    int i_mtime; /* Modification time */

    int i_dtime; /* Deletion Time */

    int i_file_acl_lo; /* File ACL */

    int i_size_high;

    enum ObjType {
        Key, Bucket, Volume;
    }
}
