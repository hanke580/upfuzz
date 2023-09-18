package org.zlab.upfuzz;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.cassandra.CassandraCommand;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.hdfs.dfs.SpecialMkdir;
import org.zlab.upfuzz.utils.INTType;
import org.zlab.upfuzz.utils.STRINGType;
import org.zlab.upfuzz.utils.Utilities;

public class CommandSequence implements Serializable {
    static Logger logger = LogManager.getLogger(CommandSequence.class);

    public final static int RETRY_GENERATE_TIME = 50;
    public final static int RETRY_MUTATE_TIME = 20;

    public List<Command> commands;
    public final List<Map.Entry<Class<? extends Command>, Integer>> commandClassList;
    public final List<Map.Entry<Class<? extends Command>, Integer>> createCommandClassList;
    public final Class<? extends State> stateClass;
    public State state;

    public CommandSequence(
            List<Command> commands,
            List<Map.Entry<Class<? extends Command>, Integer>> commandClassList,
            List<Map.Entry<Class<? extends Command>, Integer>> createCommandClassList,
            Class<? extends State> stateClass, State state) {
        this.commands = commands;
        this.commandClassList = commandClassList;
        this.createCommandClassList = createCommandClassList;
        this.stateClass = stateClass;
        this.state = state;
    }

    public void separateFromFormerTest()
            throws Exception {

        Constructor<?> constructor = stateClass.getConstructor();
        state = (State) constructor.newInstance();
        for (Command command : commands) {
            command.separate(state);
        }
        List<Command> validCommands = new LinkedList<>();

        for (int i = 0; i < commands.size(); i++) {
            boolean fixable = checkAndUpdateCommand(commands.get(i), state);
            if (fixable) {
                validCommands.add(commands.get(i));
                updateState(commands.get(i), state);
            }
        }
        this.commands = validCommands;
    }

    public void initializeTypePool() {
        for (Command command : commands) {
            command.updateTypePool();
        }
    }

    public boolean mutate()
            throws Exception {
        // Choice
        // 0: Mutate the command (Call command.mutate) // 2/3
        // 1: Insert a command 1/3
        // 2: Replace a command 0
        // 3: Delete a command 0
        separateFromFormerTest();
        initializeTypePool();

        if (CassandraCommand.DEBUG) {
            System.out.println("String Pool:" + STRINGType.stringPool);
            System.out.println("Int Pool: " + INTType.intPool);
        }

        Random rand = new Random();
        for (int mutateRetryIdx = 0; mutateRetryIdx < RETRY_MUTATE_TIME; mutateRetryIdx++) {
            try {
                int choice = rand.nextInt(3);
                // hdfs: only clear dfs state, since we will recompute
                // cassandra: clear all states
                state.clearState();

                int pos;
                if (choice == 0 || choice == 1) {
                    // Mutate a specific command
                    if (Config.getConf() != null
                            && Config.getConf().system != null
                            && Config.getConf().system.equals("hdfs")) {
                        // do not mutate the first command
                        pos = Utilities.randWithRange(rand, 1, commands.size());
                    } else {
                        pos = rand.nextInt(commands.size());
                    }
                    logger.trace("\t\tMutate Command Pos " + pos);
                    // Compute the state up to the position
                    for (int i = 0; i < pos; i++) {
                        commands.get(i).updateState(state);
                    }
                    boolean mutateStatus = commands.get(pos).mutate(state);
                    if (!mutateStatus)
                        continue;
                    boolean fixable = checkAndUpdateCommand(commands.get(pos),
                            state);
                    if (!fixable) {
                        // remove the command from command sequence
                        commands.remove(pos);
                        pos -= 1;
                    } else {
                        updateState(commands.get(pos), state);
                    }
                } else {
                    // Insert a command
                    if (Config.getConf() != null
                            && Config.getConf().system != null
                            && Config.getConf().system.equals("hdfs")) {
                        // Do not insert before the first special command
                        pos = org.zlab.upfuzz.utils.Utilities.biasRand(
                                rand, commands.size(), 5) + 1;
                    } else {
                        pos = org.zlab.upfuzz.utils.Utilities.biasRand(
                                rand, commands.size() + 1, 5);
                    }

                    // pos = rand.nextInt(commands.size() + 1);
                    logger.trace("\t\tMutate Command Pos " + pos);

                    // Compute the state up to the position
                    for (int i = 0; i < pos; i++) {
                        commands.get(i).updateState(state);
                    }
                    Command command;

                    command = generateSingleCommand(commandClassList, state);
                    while (command == null) {
                        assert !createCommandClassList.isEmpty();
                        ;
                        command = generateSingleCommand(createCommandClassList,
                                state);
                    }
                    commands.add(pos, command);
                    commands.get(pos).updateState(state);
                }
                // Check the following commands
                // There could be some commands that cannot be
                // fixed. Therefore, remove them to keep the
                // validity.
                List<Command> validCommands = new LinkedList<>();
                for (int i = 0; i < pos + 1; i++) {
                    validCommands.add(commands.get(i));
                }
                for (int i = pos + 1; i < commands.size(); i++) {
                    boolean fixable = checkAndUpdateCommand(commands.get(i),
                            state);
                    if (fixable) {
                        validCommands.add(commands.get(i));
                        updateState(commands.get(i), state);
                    }
                }
                commands = validCommands;
                this.state = state;

                ParameterType.BasicConcreteType.clearPool();

                return true;
            } catch (Exception e) {
                logger.error("CommandSequence mutation problem: " + e);
                e.printStackTrace();
                // keep retrying!
            }
        }
        // The mutation is failed.
        logger.error("Mutation Failed");
        return false;
    }

    public static Command generateSingleCommand(
            List<Map.Entry<Class<? extends Command>, Integer>> commandClassList,
            State state) {
        Command command = null;
        Random rand = new Random();
        assert !commandClassList.isEmpty();
        // Set Retry time is to avoid forever loop when all
        // the commands cannot be generated correctly.
        for (int i = 0; i < RETRY_GENERATE_TIME; i++) {
            try {
                int sum = commandClassList.stream()
                        .mapToInt(Map.Entry::getValue)
                        .sum();

                int tmpSum = 0;
                int randInt = rand.nextInt(sum);
                int cmdIdx = 0;

                for (int j = 0; j < sum; j++) {
                    tmpSum += commandClassList.get(j).getValue();
                    if (randInt < tmpSum)
                        break;
                    cmdIdx++;
                }
                Class<? extends Command> clazz = commandClassList.get(cmdIdx)
                        .getKey();

                Constructor<?> constructor;
                try {
                    constructor = clazz.getConstructor(state.getClass());
                } catch (NoSuchMethodException e) {
                    constructor = clazz.getConstructor(State.class);
                }
                command = (Command) constructor.newInstance(state);
                command.updateState(state);
                break;
            } catch (Exception e) {
                command = null;
            }
        }

        return command;
    }

    public static CommandSequence generateSequence(
            List<Map.Entry<Class<? extends Command>, Integer>> commandClassList,
            List<Map.Entry<Class<? extends Command>, Integer>> createCommandClassList,
            Class<? extends State> stateClass, State state)
            throws Exception {

        assert commandClassList != null;

        Random rand = new Random();
        int len = rand
                .nextInt(Config.getConf().MAX_CMD_SEQ_LEN
                        - Config.getConf().MIN_CMD_SEQ_LEN)
                + Config.getConf().MIN_CMD_SEQ_LEN;

        Constructor<?> constructor = stateClass.getConstructor();
        if (state == null)
            state = (State) constructor.newInstance();
        List<Command> commands = new LinkedList<>();

        for (int i = 0; i < len; i++) {

            if (i == 0 && Config.getConf() != null
                    && Config.getConf().system != null
                    && Config.getConf().system.equals("hdfs")) {
                // add a mkdir command for separation
                commands.add(new SpecialMkdir((HdfsState) state));
                continue;
            }

            Command command;
            if (createCommandClassList != null) {
                // Make sure the first three columns are write related command,
                // so that the later command can be generated more easily.
                // [Could be changed later]
                if (i <= 2) {
                    command = generateSingleCommand(createCommandClassList,
                            state);
                } else {
                    command = generateSingleCommand(commandClassList, state);
                    if (command == null) {
                        command = generateSingleCommand(createCommandClassList,
                                state);
                    }
                }
            } else {
                command = generateSingleCommand(commandClassList, state);
            }
            if (command != null) {
                commands.add(command);
            }
            // The final length might be smaller than the target len since
            // some command generation might fail.
        }

        ParameterType.BasicConcreteType.clearPool();
        return new CommandSequence(commands, commandClassList,
                createCommandClassList, stateClass, state);
    }

    public CommandSequence generateRelatedReadSequence() {
        List<Command> commands = new LinkedList<>();
        for (Command command : this.commands) {
            Set<Command> readCommands = command
                    .generateRelatedReadCommand(this.state);
            if (readCommands != null) {
                for (Command readCommand : readCommands) {
                    boolean fixable = checkAndUpdateCommand(readCommand, state);
                    if (fixable) {
                        commands.add(readCommand);
                        updateState(readCommand, state);
                    }
                }
            }
        }

        return new CommandSequence(commands, commandClassList,
                createCommandClassList, stateClass, state);
    }

    public List<String> getCommandStringList() {
        List<String> commandStringList = new ArrayList<>();
        for (Command command : commands) {
            if (command != null) {
                commandStringList.add(command.constructCommandString());
            }
        }
        return commandStringList;
    }

    public static boolean checkAndUpdateCommand(Command command, State state) {
        // Check whether current command is valid. Fix if not valid.
        // TODO: What if it cannot be fixed?
        // - simple solution, just return a false, and make command sequence
        // remove it. Update the command string
        return command.regenerateIfNotValid(state);
    }

    public static boolean updateState(Command command, State state) {
        command.updateState(state);
        return true;
    }

    public int getSize() {
        if (commands != null) {
            return commands.size();
        } else {
            logger.error("Empty command sequence");
            return -1;
        }
    }
}
