package org.zlab.upfuzz.ozone.sh.key;

import org.zlab.upfuzz.ozone.OzoneState;

public class CatKey extends KeyQuery {
    public CatKey(OzoneState state) {
        super(state);
        this.command = "cat";
    }
}
