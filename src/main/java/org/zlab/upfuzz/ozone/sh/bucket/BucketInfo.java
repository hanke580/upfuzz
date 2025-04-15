package org.zlab.upfuzz.ozone.sh.bucket;

import org.zlab.upfuzz.ozone.OzoneState;

public class BucketInfo extends BucketQuery {
    public BucketInfo(OzoneState state) {
        super(state);
        command = "info";
    }
}
