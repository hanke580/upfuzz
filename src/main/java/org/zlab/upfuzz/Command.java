package org.zlab.upfuzz;

public interface Command {
    public String constructCommand();
    public void updateState();
}
