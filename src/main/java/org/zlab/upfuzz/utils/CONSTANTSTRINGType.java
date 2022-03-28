package org.zlab.upfuzz.utils;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;

public class CONSTANTSTRINGType extends STRINGType {
    /**
     * Only two choice, empty string or the given fixed value at
     * constructor function.
     *
     * Override "GenerateRandomParameter"
     * Override "Mutate", if exists
     *  - Exist
     *  - Empty string
     */

    public static final String signature = "org.zlab.upfuzz.utils.FIXSTRINGType";

    final String fixString;

    public CONSTANTSTRINGType(String fixString) {
        this.fixString = fixString;
    }

    @Override
    public Parameter generateRandomParameter(State s, Command c) {
        return new Parameter(this, fixString);
    }

    @Override
    public Parameter generateRandomParameter(State s, Command c, Object init) {
        return generateRandomParameter(s, c);
    }

    @Override
    public String generateStringValue(Parameter p) {
        return (String) p.value;
    }

    @Override
    public boolean mutate(State s, Command c, Parameter p) {
        // Cannot mutate!
        return false;
    }
}