package org.zlab.upfuzz;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * User need to implement two methods constructCommandString() and updateState().
 * If our custom mutation is not enough, they can implement their mutation by
 * overriding mutate() method.
 */
public abstract class Command implements Serializable {

    public List<Parameter> params;
    public String executableCommandString;

    public Command() {
        params = new LinkedList<>();
    }

    public void updateExecutableCommandString() {
        executableCommandString = constructCommandString();
    }

    @Override
    public String toString() {
        return executableCommandString;
    }

    public abstract String constructCommandString();

    public abstract void updateState(State state);

    public boolean mutate(State s) throws IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {
        Random rand = new Random();
        int mutateParamIdx = rand.nextInt(params.size());
        //        mutateParamIdx = 0;
        System.out.println("\n Mutate Param Pos = " + mutateParamIdx);

        return params.get(mutateParamIdx).mutate(s, this);
    }

    public boolean regenerateIfNotValid(State s) {
        /**
         * Run check on each existing parameters in order.
         * It will includes
         * - Parameter.isValid()
         * - Parameter.fixIfNotValid()
         * Two functions
         */
        for (Parameter param : params) {
            try {
                if (!param.isValid(s, this)) {
                    param.regenerate(s, this);
                }
            } catch (Exception e) {
                // This parameter cannot be fixed
                return false;
            }
        }
        return true;
    }

    public Set<Command> generateRelatedReadCommand(State state) {
        return null;
    }
}
