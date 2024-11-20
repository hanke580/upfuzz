package org.zlab.upfuzz.ozone.key;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.ozone.Sh;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.ozone.OzoneParameterType.OzoneKeyType;
import org.zlab.upfuzz.ozone.OzoneParameterType.OzoneBucketType;
import org.zlab.upfuzz.ozone.OzoneParameterType.OzoneDirPathType;
import org.zlab.upfuzz.ozone.OzoneParameterType.RandomLocalPathType;
import org.zlab.upfuzz.utils.STRINGType;

public class PutKey extends Sh {

    public PutKey(OzoneState state) {
        super(state.key);

        Parameter putKeyCmd = new CONSTANTSTRINGType("put")
                .generateRandomParameter(null, null);
        params.add(putKeyCmd);

        Parameter bucketNameParameter = new OzoneBucketType()
                .generateRandomParameter(state, null);
        params.add(bucketNameParameter);

        Parameter keyNameParameter = new STRINGType(20)
                .generateRandomParameter(null, null);
        params.add(keyNameParameter);

        Parameter srcParameter = new RandomLocalPathType()
                .generateRandomFileParameter(state, null);
        params.add(srcParameter);
    }

    @Override
    public void updateState(State state) {
        // Add a real objnode to state
        if (!(params.get(3).toString()).toString().equals("")) {
            String p1 = (params.get(1).toString()).toString() + "/"
                    + (params.get(2).toString()).toString();
            ((OzoneState) state).oos.createKey(p1);
        }
        return;
    }

    @Override
    public String constructCommandString() {
        String p1 = (params.get(1).toString()).toString() + "/"
                + (params.get(2).toString()).toString();
        String p2 = (params.get(3).toString()).toString();
        return "sh key" + " " + params.get(0) +
                " " + p1 + " " + p2;
    }

    @Override
    public void separate(State state) {
    }
}
