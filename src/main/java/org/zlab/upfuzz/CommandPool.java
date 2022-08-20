package org.zlab.upfuzz;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class CommandPool {
    public final List<Map.Entry<Class<? extends Command>, Integer>> commandClassList = new ArrayList<>();

    /**
     * public static final List<Class<? extends Command>> commandClassList = new
     * ArrayList<>(); Prioritized commands, have a higher possibility to be
     * generated in the first several commands, but share the same possibility
     * with the rest for the following commands.
     */
    public final List<Map.Entry<Class<? extends Command>, Integer>> createCommandClassList = new ArrayList<>();
    public final List<Map.Entry<Class<? extends Command>, Integer>> readCommandClassList = new ArrayList<>();
}
