package org.zlab.upfuzz;

public class Parameter {

    public ParameterType.ConcreteType type;
    /* Inside this parameter's value, it could contain lower-level parameters. */
    public Object value;
    public String strValue;

    public Parameter(ParameterType.ConcreteType type, Object value) {
        this.type = type;
        this.value = value;
        this.strValue = null;
    }

    @Override
    public String toString() {
        if (strValue == null) {
            strValue = type.generateStringValue(this);
        }
        return strValue;
    }

    public void mutate() {
        System.out.println("hello mutation");
    }

}
