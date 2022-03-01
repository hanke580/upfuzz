package org.zlab.upfuzz;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

/**
 * User need to implement two methods constructCommandString() and updateState().
 * If our custom mutation is not enough, they can implement their mutation by
 * overriding mutate() method.
 */
public abstract class Command {

    public abstract String constructCommandString();
    public abstract void updateState(State state);

    public void mutate(State state) throws
            IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        // Use reflection to pick one parameter for mutation.
        Field[] fields = this.getClass().getDeclaredFields();
        Random rand = new Random();
        int mutateParamIdx = rand.nextInt(fields.length);
        Field fieldDef = fields[mutateParamIdx];
        fieldDef.setAccessible(true);
        Object fieldVal = fieldDef.get(this);
        Method m = fieldVal.getClass().getDeclaredMethod("mutate", new Class[]{});
        m.invoke(fieldVal);
    }
}
