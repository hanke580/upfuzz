/* (C)2022 */
package org.zlab.upfuzz;

import java.io.Serializable;

public abstract class State implements Serializable {
    public abstract void clearState();
}
