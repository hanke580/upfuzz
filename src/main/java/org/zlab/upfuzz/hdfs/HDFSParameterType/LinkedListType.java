package org.zlab.upfuzz.hdfs.HDFSParameterType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.utils.Utilities;

public class LinkedListType extends ParameterType.ConcreteType {

    List<Parameter> parameters;

    LinkedListType(Parameter... params) {
        parameters = Arrays.asList(params);
    }

    @Override
    public Parameter generateRandomParameter(State s, Command c) {
        List<Parameter> parameterList = new ArrayList<>();
        for (Parameter p : parameters) {
            Parameter parameterInstance = p.type.generateRandomParameter(s, c);
            parameterList.add(parameterInstance);
        }
        return new Parameter(this, parameterList);
    }

    @Override
    public String generateStringValue(Parameter p) {
        StringBuilder str = new StringBuilder();
        for (Parameter pi : (List<Parameter>) p.value) {
            String ps = pi.type.generateStringValue(pi);
            str.append(ps);
        }
        return str.toString();
    }

    @Override
    public boolean isValid(State s, Command c, Parameter p) {
        for (Parameter pi : (List<Parameter>) p.value) {
            if (!pi.type.isValid(s, c, pi)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void regenerate(State s, Command c, Parameter p) {
        for (Parameter pi : (List<Parameter>) p.value) {
            pi.type.regenerate(s, c, pi);
        }
    }

    @Override
    public boolean isEmpty(State s, Command c, Parameter p) {
        return ((List<Parameter>) p.value).size() == 0;
    }

    @Override
    public boolean mutate(State s, Command c, Parameter p) {
        List<Parameter> parameterList = (List<Parameter>) p.value;
        List<Integer> mutateOrder = Utilities.permutation(parameterList.size());
        for (int index : mutateOrder) {
            Parameter pi = parameterList.get(index);
            if (pi.type.mutate(s, c, p)) {
                return true;
            }
        }
        return false;
    }
}
