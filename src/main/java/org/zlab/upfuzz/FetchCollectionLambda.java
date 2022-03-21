package org.zlab.upfuzz;

import java.util.Collection;

public interface FetchCollectionLambda {
    Collection operate(State state, Command command);
}