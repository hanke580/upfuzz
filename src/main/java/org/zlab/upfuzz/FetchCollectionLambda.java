package org.zlab.upfuzz;

import java.io.Serializable;
import java.util.Collection;

public interface FetchCollectionLambda extends Serializable {
    Collection<Parameter> operate(State state, Command command);
}