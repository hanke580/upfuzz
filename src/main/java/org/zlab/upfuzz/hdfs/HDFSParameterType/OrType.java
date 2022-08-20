package org.zlab.upfuzz.hdfs.HDFSParameterType;

import org.apache.commons.lang3.RandomUtils;
import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;

public class OrType extends ParameterType.ConcreteType {

    ConcreteType[] concreteTypes;
    int choice = 0;

    public OrType(ConcreteType t1, ConcreteType t2) {
        concreteTypes = new ConcreteType[] { t1, t2 };
    }

    @Override
    public Parameter generateRandomParameter(State s, Command c) {
        choice = RandomUtils.nextInt(0, 2);
        return concreteTypes[choice].generateRandomParameter(s, c);
    }

    @Override
    public String generateStringValue(Parameter p) {
        return concreteTypes[choice].generateStringValue(p);
    }

    @Override
    public boolean isValid(State s, Command c, Parameter p) {
        return concreteTypes[choice].isValid(s, c, p);
    }

    @Override
    public void regenerate(State s, Command c, Parameter p) {
        concreteTypes[choice].regenerate(s, c, p);
    }

    @Override
    public boolean isEmpty(State s, Command c, Parameter p) {
        return concreteTypes[choice].isEmpty(s, c, p);
    }

    @Override
    public boolean mutate(State s, Command c, Parameter p) {
        int newOption = RandomUtils.nextInt(0, 2);
        if (newOption != choice) {
            choice = newOption;
            p.value = concreteTypes[choice].generateRandomParameter(s, c);
            return true;
        }
        return concreteTypes[choice].mutate(s, c, p);
    }

    @Override
    public Parameter generateRandomParameter(State s, Command c, Object init) {
        // TODO Auto-generated method stub
        return null;
    }
}
