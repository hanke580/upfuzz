package org.zlab.upfuzz;

import java.util.ArrayList;
import java.util.List;

public abstract class Parameter {

    public final ParameterType type;
    /* Inside this parameter's value, it could contain lower-level parameters. */
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

//    // TODO: think about this interface
//    public void mutate(State state, Command currCmd) {
//        value = type.constructRandomValue();
//        while (!isValid(state, currCmd)) {
//            value = type.constructRandomValue();
//        }
//    }

    @Override
    public String toString() {
        if (strValue == null) {
            strValue = type.generateStringValue(value);
        }
        return strValue;
    }

    public static abstract class NormalParameter extends Parameter {
        public NormalParameter(ParameterType.NormalType type) {
            super(type);
        }
    }

    public static abstract class TemplatedParameter extends Parameter {
        // Support a variable number of templates. E.g., Pair<K, V>.
        public List<ParameterType> typesInTemplate = new ArrayList<>();

        public TemplatedParameter(ParameterType.TemplatedType type,
                                  ParameterType typeInTemplate) {
            super(type);
            this.typesInTemplate.add(typeInTemplate);
        }

        public TemplatedParameter(ParameterType.TemplatedType type,
                                  ParameterType typeInTemplate1,
                                  ParameterType typeInTemplate2) {
            super(type);
            this.typesInTemplate.add(typeInTemplate1);
            this.typesInTemplate.add(typeInTemplate2);
        }
    }
}
