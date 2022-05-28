/* (C)2022 */
package org.zlab.upfuzz;

import java.io.Serializable;
import java.util.Collection;

public interface FetchCollectionLambda extends Serializable {
    Collection operate(State state, Command command);
}
