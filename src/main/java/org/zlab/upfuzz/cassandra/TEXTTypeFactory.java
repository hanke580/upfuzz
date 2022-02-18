package org.zlab.upfuzz.cassandra;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterFactory;

public class TEXTTypeFactory extends ParameterFactory {

    public static class TEXTType extends Parameter {
        String str;

        @Override
        public String toString() {
            // TODO: return the string representation of this object.
            return str;
        }
    }

    @Override
    public TEXTType constructRandom() {
        // TODO: generate a random TEXTType object.
        return null;
    }
}
