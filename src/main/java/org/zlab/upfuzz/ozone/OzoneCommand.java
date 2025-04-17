package org.zlab.upfuzz.ozone;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.Utilities;

import java.util.LinkedList;
import java.util.List;

public abstract class OzoneCommand extends Command {

    public OzoneCommand() {
        initStorageTypeOptions();
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

    /**
     * This helper function will randomly pick an existing volume and return its
     * tablename as parameter.
     */
    public static Parameter chooseVolume(OzoneState state, Command command) {
        ParameterType.ConcreteType keyspaceNameType = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> ((OzoneState) s).getVolumes(),
                null);
        return keyspaceNameType.generateRandomParameter(state, command);
    }

    /**
     * This helper function will randomly pick an existing volume and return its
     * tablename as parameter.
     */
    public static Parameter chooseBucket(OzoneState state, Command command) {
        ParameterType.ConcreteType keyspaceNameType = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> state.getBuckets(c.params.get(0).toString()),
                null);
        return keyspaceNameType.generateRandomParameter(state, command);
    }

    public static Parameter chooseKey(OzoneState state, Command command,
            String volumeName, String bucketName) {
        ParameterType.ConcreteType keyspaceNameType = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> state.getKeys(volumeName, bucketName),
                null);
        return keyspaceNameType.generateRandomParameter(state, command);
    }
}
