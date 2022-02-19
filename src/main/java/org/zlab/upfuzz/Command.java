package org.zlab.upfuzz;

public interface Command {
    public String constructCommandString();
    public void updateState();
}
