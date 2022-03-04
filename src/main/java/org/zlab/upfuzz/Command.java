package org.zlab.upfuzz;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * User need to implement two methods constructCommandString() and updateState().
 * If our custom mutation is not enough, they can implement their mutation by
 * overriding mutate() method.
 */
public abstract class Command {

    public List<Parameter> params;

    public abstract String constructCommandString();
    public abstract void updateState(State state);

    public void mutate(State s) throws
            IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Random rand = new Random();
        int mutateParamIdx = rand.nextInt(params.size());
        params.get(mutateParamIdx).mutate();
    }

    public void check(State s, Command c) {
        /**
         * Run check on each existing parameters in order.
         * It will includes
         * - Parameter.isValid()
         * - Parameter.fixIfNotValid()
         * Two functions
         */
        for (Parameter param : params) {
            if (!param.isValid(s, c)) {
                param.fixIfNotValid(s, c);
            }
        }
    }
}
