package org.zlab.upfuzz.ozone.sh.key;

import org.zlab.upfuzz.ozone.OzoneState;

public class KeyGetAcl extends KeyQuery {

    public KeyGetAcl(OzoneState state) {
        super(state);
        this.command = "getacl";
    }
}
