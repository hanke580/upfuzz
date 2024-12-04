package org.zlab.upfuzz.hdfs;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.fuzzingengine.Config;

import java.util.LinkedList;
import java.util.List;

public abstract class HdfsCommand extends Command {
    public String subdir;

    public HdfsCommand(String subdir) {
        this.subdir = subdir;
        initStorageTypeOptions();
    }

    @Override
    public void separate(State state) {
        subdir = ((HdfsState) state).subdir;
    }

    public static List<String> formatOptions = new LinkedList<>();
    static {
        formatOptions.add("\"%b\"");
        formatOptions.add("\"%n\"");
        formatOptions.add("\"%o\"");
        formatOptions.add("\"%r\"");
        formatOptions.add("\"%y\"");
        formatOptions.add("\"%f\"");
    }

    public List<String> storageTypeOptions = new LinkedList<>();

    public void initStorageTypeOptions() {
        storageTypeOptions.add("RAM_DISK");
        if (Config.getConf().support_StorageType_NVDIMM)
            storageTypeOptions.add("NVDIMM");
        storageTypeOptions.add("SSD");
        storageTypeOptions.add("DISK");
        storageTypeOptions.add("ARCHIVE");
        if (Config.getConf().support_StorageType_PROVIDED)
            storageTypeOptions.add("PROVIDED");
    }

    public String constructCommandStringWithDirSeparation(String type) {
        StringBuilder sb = new StringBuilder();
        sb.append(type).append(" ");
        int i = 0;
        while (i < params.size() - 1) {
            if (!params.get(i).toString().isEmpty())
                sb.append(params.get(i)).append(" ");
            i++;
        }
        sb.append(subdir).append(params.get(i));
        return sb.toString();
    }
}
