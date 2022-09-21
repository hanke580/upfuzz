package org.zlab.upfuzz;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.cassandra.CassandraCommands;
import org.zlab.upfuzz.fuzzingengine.testplan.event.Event;

/**
 * User need to implement two methods constructCommandString() and
 * updateState(). If our custom mutation is not enough, they can implement their
 * mutation by overriding mutate() method.
 */
public abstract class Command implements Serializable, Event {
    static Logger logger = LogManager.getLogger(Command.class);

    public static final int RETRY_TIMES = 5;

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
            NoSuchMethodException,
            InvocationTargetException {
        Random rand = new Random();

        for (int i = 0; i < RETRY_TIMES; i++) {
            int mutateParamIdx = rand.nextInt(params.size());
            if (CassandraCommands.DEBUG) {
                mutateParamIdx = 2;
            }
            // logger.debug("Mutate Parameter Pos = " + mutateParamIdx);
            if (params.get(mutateParamIdx).mutate(s, this) == true)
                return true;
        }
        return false;
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

    public void updateTypePool() {
        for (Parameter param : params) {
            param.updateTypePool();
        }
    }

    public Set<Command> generateRelatedReadCommand(State state) {
        return null;
    };

    public void changeKeyspaceName() {
        // Only Create keyspace command should override this method
        return;
    }
}
