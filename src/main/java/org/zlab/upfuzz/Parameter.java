package org.zlab.upfuzz;

public class Parameter {

    public final ParameterType type;
    /* Inside this parameter's value, it could contain lower-level parameters. */
    public final Object value;
    public String strValue;

    public Parameter(ParameterType type, Object value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public String toString() {
        if (strValue == null) {
            strValue = type.generateStringValue(this);
        }
        return strValue;
    }

}
