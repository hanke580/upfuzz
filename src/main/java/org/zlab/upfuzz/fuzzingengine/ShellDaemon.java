package org.zlab.upfuzz.fuzzingengine;

public abstract class ShellDaemon {
    public abstract String executeCommand(String command) throws Exception;
}
