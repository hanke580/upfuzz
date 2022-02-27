package org.zlab.upfuzz.utils;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;

public class FIXSTRINGType extends STRINGType {
    /**
     * Only two choice, empty string or the given fixed value at
     * constructor function.
     *
     * Override "GenerateRandomParameter"
     * Override "Mutate", if exists
     *  - Exist
     *  - Empty string
     */

    // TODO:Should we keep using instance here?
//        public static final FIXEDSTRINGType instance = new FIXEDSTRINGType();
    public static final String signature = "org.zlab.upfuzz.utils.FIXEDSTRINGType";

    final String fixString;

    boolean isEmpty;

    public FIXSTRINGType(String fixString) {
        this.fixString = fixString;
    }

    @Override
    public Parameter generateRandomParameter(State s, Command c) {
        // TODO: generate a random string.
        if (isEmpty) {
            return new Parameter(this, "");
        } else {
            return new Parameter(this, fixString);
        }
    }

    @Override
    public String generateStringValue(Parameter p) {
        return fixString;
    }

}