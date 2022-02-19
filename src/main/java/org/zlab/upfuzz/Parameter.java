package org.zlab.upfuzz;

public abstract class Parameter {

    public final ParameterType type;
    public Object value;
    public String strValue;

    public Parameter(ParameterType type) {
        this.type = type;
    }

    /**
     * generateValue() follows rules to generate a value for this parameter.
     * @param state
     * @param currCmd
     *  these rules might use the state and other parameters in the current command.
     */
    public abstract void generateValue(State state, Command currCmd);

    public boolean isValid(State state, Command currCmd) {
        return true;
    }

    @Override
    public String toString() {
        if (strValue == null) {
            strValue = type.generateStringValue(value);
        }
        return strValue;
    }
}
