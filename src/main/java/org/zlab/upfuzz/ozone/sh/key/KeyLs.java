package org.zlab.upfuzz.ozone.sh.key;

import org.zlab.upfuzz.ozone.OzoneState;

public class KeyLs extends KeyQuery {

    public KeyLs(OzoneState state) {
        super(state);
        this.command = "ls";
    }
}
