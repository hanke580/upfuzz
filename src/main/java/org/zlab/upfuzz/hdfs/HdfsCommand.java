package org.zlab.upfuzz.hdfs;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.fuzzingengine.Config;

import java.util.List;

public abstract class HdfsCommand extends Command {
    public String subdir;

    public HdfsCommand(String subdir) {
        this.subdir = subdir;
    }

    @Override
    public void separate(State state) {
        subdir = ((HdfsState) state).subdir;
    }

    public void initStorageTypeOptions(List<String> storageTypeOptions) {
        storageTypeOptions.add("RAM_DISK");
        if (Config.getConf().support_NVDIMM)
            storageTypeOptions.add("NVDIMM");
        storageTypeOptions.add("SSD");
        storageTypeOptions.add("DISK");
        storageTypeOptions.add("ARCHIVE");
        if (Config.getConf().support_PROVIDED)
            storageTypeOptions.add("PROVIDED");
    }
}
