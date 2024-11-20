package org.zlab.upfuzz;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.ozone.OzoneState;

/**
 * User need to implement two methods constructCommandString() and
 * updateState(). If our custom mutation is not enough, they can implement their
 * mutation by overriding mutate() method.
 */
public abstract class Command implements Serializable {
    static Logger logger = LogManager.getLogger(Command.class);

    public static final int RETRY_TIMES = 5;

    public List<Parameter> params;

    public Command() {
        params = new LinkedList<>();
    }

    @Override
    public String toString() {
        return String.format("[COMMAND] Execute {%s}",
                constructCommandString());
    }

    public abstract String constructCommandString();

    public abstract void updateState(State state);

    public boolean mutate(State s) throws Exception {
        Random rand = new Random();
        if (params.size() == 0)
            return false;
        for (int i = 0; i < RETRY_TIMES; i++) {
            int mutateParamIdx = rand.nextInt(params.size());
            // logger.debug("Mutate Parameter Pos = " + mutateParamIdx);
            if (params.get(mutateParamIdx).mutate(s, this))
                return true;
        }
        return false;
    }

    public boolean regenerateIfNotValid(State s) {
        // Run check on each existing parameters in order.
        // It includes
        // - Parameter.isValid()
        // - Parameter.fixIfNotValid()
        // Two functions
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

    public boolean isValid(State s) {
        for (Parameter param : params) {
            if (!param.isValid(s, this)) {
                return false;
            }
        }
        return true;
    }

    public void updateTypePool() {
        for (Parameter param : params) {
            try {
                param.updateTypePool();
            } catch (Exception e) {
                // log stack trace
                logger.error("Exception: " + e + " command = " + this
                        + ", param idx = " + params.indexOf(param));
                for (StackTraceElement ste : e.getStackTrace()) {
                    logger.error(ste);
                }
            }
        }
    }

    public Set<Command> generateRelatedReadCommand(State state) {
        return null;
    }

    public abstract void separate(State state);
}
