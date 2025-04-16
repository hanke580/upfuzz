package org.zlab.upfuzz.ozone.sh.key;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.STRINGType;
import org.zlab.upfuzz.utils.Utilities;

public class RenameKey extends KeyQuery {

    public RenameKey(OzoneState state) {
        super(state);
        // 3: choose a keyName
        Parameter dstKeyNameParam = new STRINGType(10, 1)
                .generateRandomParameter(null, null);
        params.add(dstKeyNameParam);
        command = "rename";
    }

    @Override
    public void updateState(State state) {
        String volumeName = params.get(0).toString();
        String bucketName = params.get(1).toString();
        String oldKeyName = params.get(2).toString();
        String newKeyName = params.get(3).toString();

        ((OzoneState) state).deleteKey(volumeName, bucketName, oldKeyName);
        ((OzoneState) state).addKey(volumeName, bucketName, newKeyName);
    }

    @Override
    public String constructCommandString() {
        String volumeName = volumePrefix + params.get(0).toString();
        String bucketName = params.get(1).toString();
        String oldKeyName = params.get(2).toString();
        String newKeyName = params.get(3).toString();

        String bucketPath = volumeName + "/" + bucketName;
        return Utilities.concat("sh key", command, bucketPath,
                oldKeyName, newKeyName);
    }
}
