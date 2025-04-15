package org.zlab.upfuzz.ozone.sh.bucket;

import org.zlab.upfuzz.ozone.OzoneState;

public class BucketGetAcl extends BucketQuery {
    public BucketGetAcl(OzoneState state) {
        super(state);
        command = "getacl";
    }
}
