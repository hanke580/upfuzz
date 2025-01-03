package org.zlab.upfuzz.ozone.sh.key;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.ozone.OzoneParameterType.RandomLocalPathType;

public class GetKey extends KeyQuery {

    public GetKey(OzoneState state) {
        super(state);
        Parameter dstParameter = new RandomLocalPathType()
                .generateRandomParameter(state, null);
        params.add(dstParameter);

        this.command = "get";
    }

    @Override
    public String constructCommandString() {
        String volumeName = params.get(0).toString();
        String bucketName = params.get(1).toString();
        String keyName = params.get(2).toString();
        String remotePath = volumeName + "/" + bucketName + "/" + keyName;
        String localPath = params.get(3).toString();
        return "sh key get" + " " + remotePath + " " + localPath;
    }

}
