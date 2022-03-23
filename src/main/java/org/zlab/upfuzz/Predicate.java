package org.zlab.upfuzz;

public interface Predicate {
    boolean operate(State state, Command command);
}
