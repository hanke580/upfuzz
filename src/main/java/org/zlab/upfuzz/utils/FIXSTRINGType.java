package org.zlab.upfuzz.utils;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import java.util.Random;

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
    public static final String signature = "org.zlab.upfuzz.utils.FIXSTRINGType";

    final String fixString;
    boolean isEmpty;

    public FIXSTRINGType(String fixString) {
        this.fixString = fixString;
        Random rand = new Random();
        if (rand.nextBoolean()) {
            isEmpty = true;
        } else {
            isEmpty = false;
        }
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
        return (String) p.value;
    }

    @Override
    public void mutate(Command c, State s, Parameter p) {
        isEmpty = !isEmpty;
    }
}