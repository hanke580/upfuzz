package org.zlab.upfuzz.ozone.OzoneParameterType;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.ozone.OzoneState;

public class OzoneDirPathType extends ParameterType.ConcreteType {
    // Return an existing dir path

    @Override
    public Parameter generateRandomParameter(State s, Command c, Object init) {
        return null;
    }

    @Override
    public Parameter generateRandomParameter(State s, Command c) {
        assert s instanceof OzoneState;
        OzoneState ozoneState = (OzoneState) s;
        String dirPath = ozoneState.dfs.getRandomDirPath();
        // We need to forbid removing "/"
        if (dirPath == null)
            throw new RuntimeException("there is no dir in ozone");
        return new Parameter(this, dirPath);
    }

    @Override
    public String generateStringValue(Parameter p) {
        assert p != null && p.type instanceof OzoneDirPathType;
        return (String) p.value;
    }

    @Override
    public boolean isValid(State s, Command c, Parameter p) {
        return ((OzoneState) s).dfs.containsDir((String) p.getValue());
    }

    @Override
    public void regenerate(State s, Command c, Parameter p) {
        p.value = generateRandomParameter(s, c).value;
    }

    @Override
    public boolean isEmpty(State s, Command c, Parameter p) {
        return false;
    }

    @Override
    public boolean mutate(State s, Command c, Parameter p) {
        regenerate(s, c, p);
        return true;
    }
}
