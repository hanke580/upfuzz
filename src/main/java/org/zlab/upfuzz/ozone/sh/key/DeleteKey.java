package org.zlab.upfuzz.ozone.sh.key;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;

public class DeleteKey extends KeyQuery {

    public DeleteKey(OzoneState state) {
        super(state);
        this.command = "delete";
    }

    @Override
    public void updateState(State state) {
        String volumeName = params.get(0).toString();
        String bucketName = params.get(1).toString();
        String keyName = params.get(2).toString();
        ((OzoneState) state).deleteKey(volumeName, bucketName, keyName);
    }
}
