package org.zlab.upfuzz;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class CommandPool {
    public final List<Map.Entry<Class<? extends Command>, Integer>> readCommandClassList = new ArrayList<>();
    public final List<Map.Entry<Class<? extends Command>, Integer>> commandClassList = new ArrayList<>();
    // A special set of write commands: normally could be created without any
    // requirements
    public final List<Map.Entry<Class<? extends Command>, Integer>> createCommandClassList = new ArrayList<>();

    public abstract void registerReadCommands();

    public abstract void registerWriteCommands();

    public abstract void registerCreateCommands();

    public CommandPool() {
        registerReadCommands();
        registerWriteCommands();
        registerCreateCommands();
        assert !createCommandClassList.isEmpty()
                : "create command must be registered! It would be a subset of write commands";
    }
}
