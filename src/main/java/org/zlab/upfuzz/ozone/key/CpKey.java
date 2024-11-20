package org.zlab.upfuzz.ozone.key;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.ozone.Sh;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.ozone.OzoneParameterType.OzoneKeyType;
import org.zlab.upfuzz.utils.STRINGType;

public class CpKey extends Sh {

    public CpKey(OzoneState state) {
        super(state.key);

        Parameter cpKeyCmd = new CONSTANTSTRINGType("cp")
                .generateRandomParameter(null, null);
        params.add(cpKeyCmd);

        Parameter keyNameParameter = new OzoneKeyType()
                .generateRandomParameter(state, null);
        params.add(keyNameParameter);

        Parameter destinationKeyNameParameter = new STRINGType(20)
                .generateRandomParameter(null, null);
        params.add(destinationKeyNameParameter);
    }

    @Override
    public void updateState(State state) {
        // Add a real objnode to state
        String currentKeyPath = (params.get(1).toString()).toString();
        String bucketPath = currentKeyPath.substring(0,
                currentKeyPath.lastIndexOf("/"));
        String p = bucketPath + "/"
                + (params.get(2).toString()).toString();
        ((OzoneState) state).oos.createKey(p);
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

}
