package org.zlab.upfuzz.ozone.sh.key;

import org.zlab.upfuzz.ozone.OzoneState;

public class KeyInfo extends KeyQuery {

    public KeyInfo(OzoneState state) {
        super(state);
        this.command = "info";
    }

}
