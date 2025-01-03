package org.zlab.upfuzz.ozone.key;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.ozone.Sh;
import org.zlab.upfuzz.ozone.OzoneParameterType.OzoneKeyType;
import org.zlab.upfuzz.utils.STRINGType;

public class RenameKey extends Sh {

    public RenameKey(OzoneState state) {
        super(state.key);

        Parameter renameKeyCmd = new CONSTANTSTRINGType("rename")
                .generateRandomParameter(null, null);
        params.add(renameKeyCmd);

        Parameter keyNameParameter = new OzoneKeyType()
                .generateRandomParameter(state, null);
        params.add(keyNameParameter);

        Parameter destinationKeyNameParameter = new STRINGType(20)
                .generateRandomParameter(null, null);
        params.add(destinationKeyNameParameter);
    }

    @Override
    public void updateState(State state) {
        // Update a real objnode in state
        String currentKeyPath = (params.get(1).toString()).toString();
        String bucketPath = currentKeyPath.substring(0,
                currentKeyPath.lastIndexOf("/"));
        String p = bucketPath + "/"
                + (params.get(2).toString()).toString();
        // ((OzoneState) state).oos.renameKey(currentKeyPath, bucketPath);
    }

    @Override
    public String constructCommandString() {
        String currentKeyPath = (params.get(1).toString()).toString();
        String bucketPath = currentKeyPath.substring(0,
                currentKeyPath.lastIndexOf("/"));
        String currentKeyName = currentKeyPath
                .substring(currentKeyPath.lastIndexOf("/") + 1);
        String p = (params.get(2).toString()).toString();
        return "sh key" + " " + params.get(0) +
                " " + bucketPath + " " + currentKeyName + " " + p;
    }

    @Override
    public void separate(State state) {
    }
}
