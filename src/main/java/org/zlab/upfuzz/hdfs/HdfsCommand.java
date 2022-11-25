package org.zlab.upfuzz.hdfs;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.State;

public abstract class HdfsCommand extends Command {

    public String subdir;

    public HdfsCommand(String subdir) {
        this.subdir = subdir;
    }

    @Override
    public void separate(State state) {
        subdir = ((HdfsState) state).subdir;
    }

}
