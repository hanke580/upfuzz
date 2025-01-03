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
import org.zlab.upfuzz.ozone.OzoneState;
import org.zlab.upfuzz.ozone.fs.InitialMkdir;
import org.zlab.upfuzz.utils.INTType;
import org.zlab.upfuzz.utils.STRINGType;
import org.zlab.upfuzz.utils.Utilities;

public class CommandSequence implements Serializable {
    static Logger logger = LogManager.getLogger(CommandSequence.class);

    private static final Random rand = new Random();

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

        for (Command command : commands) {
            boolean fixable = checkAndUpdateCommand(command, state);
            if (fixable) {
                validCommands.add(command);
                updateState(command, state);
            }
        }
        this.commands = validCommands;
    }

    public void initializeTypePool() {
        for (Command command : commands) {
            command.updateTypePool();
        }
    }

    public boolean mutate() throws Exception {
        // Choice
        // 0: Mutate the command 1/3
        // 1: Insert a command 2/3

        // We can disable this when eval_UnitTest
        if (Config.getConf().STACKED_TESTS_NUM != 1)
            separateFromFormerTest();
        initializeTypePool();

        if (CassandraCommand.DEBUG) {
            System.out.println("String Pool:" + STRINGType.stringPool);
            System.out.println("Int Pool: " + INTType.intPool);
        }

        for (int mutateRetryIdx = 0; mutateRetryIdx < RETRY_MUTATE_TIME; mutateRetryIdx++) {
            try {
                int choice = rand.nextInt(33);
                // hdfs: only clear dfs state, since we will recompute
                // cassandra: clear all states
                state.clearState();

                int pos;
                if (choice < 11) {
                    // Mutate a specific command
                    if (Config.getConf() != null
                            && Config.getConf().system != null
                            && Config.getConf().system.equals("hdfs")) {
                        // do not mutate the first command
                        // [1, size - 1]
                        pos = org.zlab.upfuzz.utils.Utilities.biasRand(
                                rand, commands.size() - 1, 5) + 1;
                    } else {
                        // [0, size - 1]
                        pos = org.zlab.upfuzz.utils.Utilities.biasRand(
                                rand, commands.size(), 5);

                    }
                    // Compute the state up to the position
                    for (int i = 0; i < pos; i++) {
                        assert i < commands.size();
                        commands.get(i).updateState(state);
                    }
                    boolean mutateStatus = commands.get(pos).mutate(state);
                    if (!mutateStatus) {
                        logger.error("CommandSequence mutation failed");
                        continue;
                    }
                    boolean fixable = checkAndUpdateCommand(commands.get(pos),
                            state);
                    if (!fixable) {
                        // remove the command from command sequence
                        commands.remove(pos);
                        pos -= 1;
                    } else {
                        updateState(commands.get(pos), state);
                    }
                } else if (choice < 32) {
                    // Insert a command
                    if (Config.getConf() != null
                            && Config.getConf().system != null
                            && Config.getConf().system.equals("hdfs")) {
                        // Do not insert before the first special command
                        // [1, size]
                        pos = org.zlab.upfuzz.utils.Utilities.biasRand(
                                rand, commands.size(), 5) + 1;
                    } else {
                        // [0, size]
                        pos = org.zlab.upfuzz.utils.Utilities.biasRand(
                                rand, commands.size() + 1, 5);
                    }

                    // Compute the state up to the position
                    for (int i = 0; i < pos; i++) {
                        assert i < commands.size();
                        commands.get(i).updateState(state);
                    }

                    if (Config.getConf().enableAddMultiCommandMutation
                            && rand.nextDouble() < Config
                                    .getConf().addCommandWithSameTypeProb) {
                        /* Insert multiple commands with the same type */
                        List<Command> newCommands = generateCommandsWithSameType(
                                commandClassList, state);
                        // add all commands, they must not be null
                        if (newCommands.size() != Config
                                .getConf().addCommandWithSameTypeNum)
                            continue;
                        for (Command newCommand : newCommands) {
                            commands.add(pos, newCommand);
                            pos++;
                        }
                        /* pos should point to the last command being inserted */
                        pos--;
                    } else {
                        /* Insert one command */
                        Command command = generateSingleCommand(
                                commandClassList,
                                state);
                        int count = 0;
                        while (command == null) {
                            assert !createCommandClassList.isEmpty();
                            command = generateSingleCommand(
                                    createCommandClassList,
                                    state);
                            count++;
                            if (count > 100) {
                                logger.error(
                                        "generateSingleCommand not finished after counter times");
                                break;
                            }
                        }
                        if (command == null) {
                            logger.error("generateSingleCommand failed");
                            continue;
                        }
                        commands.add(pos, command);
                        // state is already updated in generateSingleCommand!
                        // commands.get(pos).updateState(state);
                    }
                } else {
                    // Remove a command
                    if (commands.size() == 1) {
                        // Cannot remove the last command
                        continue;
                    }
                    if (Config.getConf() != null
                            && Config.getConf().system != null
                            && Config.getConf().system.equals("hdfs")) {
                        // Do not remove the first special command
                        // [1, size - 1]
                        pos = org.zlab.upfuzz.utils.Utilities.biasRand(
                                rand, commands.size() - 1, 5) + 1;
                    } else {
                        // [0, size - 1]
                        pos = org.zlab.upfuzz.utils.Utilities.biasRand(
                                rand, commands.size(), 5);
                    }
                    // Compute the state up to the position
                    for (int i = 0; i < pos; i++) {
                        assert i < commands.size();
                        commands.get(i).updateState(state);
                    }
                    commands.remove(pos);

                    // pos must be smaller than commands.size()
                    pos -= 1;
                }
                // Check the following commands
                // There could be some commands that cannot be
                // fixed. Therefore, remove them to keep the
                // validity.
                List<Command> validCommands = new LinkedList<>();
                // pos must be smaller than commands.size()
                if (pos >= commands.size())
                    throw new RuntimeException("Invalid pos during mutation");
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

                ParameterType.BasicConcreteType.clearPool();

                return true;
            } catch (Exception e) {
                logger.error("CommandSequence mutation problem: " + e);
                // print e using logger
                for (StackTraceElement ste : e.getStackTrace()) {
                    logger.error(ste);
                }
                // keep retrying!
            }
        }
        // The mutation is failed.
        logger.error("Mutation Failed");
        return false;
    }

    public static int getWeightIndex(
            List<Map.Entry<Class<? extends Command>, Integer>> commandClassList) {
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
        return cmdIdx;
    }

    public static Command generateSingleCommand(
            List<Map.Entry<Class<? extends Command>, Integer>> commandClassList,
            State state) {
        Command command = null;
        assert !commandClassList.isEmpty();
        // Set Retry time is to avoid forever loop when all
        // the commands cannot be generated correctly.
        for (int i = 0; i < RETRY_GENERATE_TIME; i++) {
            try {
                int cmdIdx = getWeightIndex(commandClassList);
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

    public static List<Command> generateCommandsWithSameType(
            List<Map.Entry<Class<? extends Command>, Integer>> commandClassList,
            State state) {
        assert !commandClassList.isEmpty();

        List<Command> commands = new LinkedList<>();
        int cmdIdx = getWeightIndex(commandClassList);
        Class<? extends Command> clazz = commandClassList.get(cmdIdx)
                .getKey();
        for (int i = 0; i < RETRY_GENERATE_TIME; i++) {
            Command command;
            try {
                Constructor<?> constructor;
                try {
                    constructor = clazz.getConstructor(state.getClass());
                } catch (NoSuchMethodException e) {
                    constructor = clazz.getConstructor(State.class);
                }
                command = (Command) constructor.newInstance(state);
            } catch (Exception e) {
                continue;
            }
            command.updateState(state);
            commands.add(command);
            if (commands.size() >= Config.getConf().addCommandWithSameTypeNum) {
                break;
            }
        }
        return commands;
    }

    public static CommandSequence generateSequence(
            List<Map.Entry<Class<? extends Command>, Integer>> commandClassList,
            List<Map.Entry<Class<? extends Command>, Integer>> createCommandClassList,
            Class<? extends State> stateClass, State state, boolean isRead)
            throws Exception {

        assert commandClassList != null;

        int len;
        if (isRead)
            len = Utilities.generateExponentialRandom(rand,
                    Config.getConf().CMD_SEQ_LEN_LAMBDA,
                    Config.getConf().MIN_READ_CMD_SEQ_LEN,
                    Config.getConf().MAX_READ_CMD_SEQ_LEN);
        else
            len = Utilities.generateExponentialRandom(rand,
                    Config.getConf().CMD_SEQ_LEN_LAMBDA,
                    Config.getConf().MIN_CMD_SEQ_LEN,
                    Config.getConf().MAX_CMD_SEQ_LEN);

        Constructor<?> constructor = stateClass.getConstructor();
        if (state == null)
            state = (State) constructor.newInstance();
        List<Command> commands = new LinkedList<>();

        for (int i = 0; i < len; i++) {
            if (i == 0 && Config.getConf() != null
                    && Config.getConf().system != null) {
                if (Config.getConf().system.equals("hdfs")) {
                    // add a mkdir command for separation
                    commands.add(new SpecialMkdir((HdfsState) state));
                } else if (Config.getConf().system.equals("ozone")) {
                    if (!isRead) {
                        // Command initialCreateBucketCmd = new
                        // InitialCreateBucket(
                        // (OzoneState) state);
                        // commands.add(initialCreateVolumeCmd);
                        // commands.add(initialCreateBucketCmd);
                        commands.add(new InitialMkdir((OzoneState) state));
                    }
                }
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
