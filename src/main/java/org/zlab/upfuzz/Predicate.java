package org.zlab.upfuzz;

import java.io.Serializable;

public interface Predicate extends Serializable {
    boolean operate(State state, Command command);
}
