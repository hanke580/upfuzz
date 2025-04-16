package org.zlab.upfuzz.ozone.OzoneParameterType;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;

public class OzoneFilePathType extends ParameterType.ConcreteType {
    // Return an existing file path

    @Override
    public Parameter generateRandomParameter(State s, Command c, Object init) {
        throw new UnsupportedOperationException(
                "generateRandomParameter with init is not supported");
    }

    @Override
    public Parameter generateRandomParameter(State s, Command c) {
        assert s instanceof OzoneState;
        OzoneState ozoneState = (OzoneState) s;
        String filePath = ozoneState.dfs.getRandomFilePath();
        if (filePath == null) {
            throw new RuntimeException(
                    "cannot generate ozone file path, there's no file in ozone");
        }
        return new Parameter(this, filePath);
    }

    @Override
    public String generateStringValue(Parameter p) {
        assert p != null && p.type instanceof OzoneFilePathType;
        return (String) p.value;
    }

    @Override
    public boolean isValid(State s, Command c, Parameter p) {
        return ((OzoneState) s).dfs.containsFile((String) p.getValue());
    }

    @Override
    public void regenerate(State s, Command c, Parameter p) {

    }

    @Override
    public boolean isEmpty(State s, Command c, Parameter p) {
        return false;
    }

    @Override
    public boolean mutate(State s, Command c, Parameter p) {
        return false;
    }
}
