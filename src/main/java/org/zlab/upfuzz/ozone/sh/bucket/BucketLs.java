package org.zlab.upfuzz.ozone.sh.bucket;

import org.zlab.upfuzz.ozone.OzoneState;

public class BucketLs extends BucketQuery {
    public BucketLs(OzoneState state) {
        super(state);
        command = "ls";
    }
}
