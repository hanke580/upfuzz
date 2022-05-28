package org.zlab.upfuzz;

import org.zlab.upfuzz.cassandra.CassandraCommands;
import org.zlab.upfuzz.utils.INTType;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.STRINGType;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import javax.swing.text.Utilities;

public class CommandSequence implements Serializable {

    public static final int MAX_CMD_SEQ_LEN = 20;
    public final static int RETRY_GENERATE_TIME = 400;
    public final static int RETRY_MUTATE_TIME = 20;

    public List<Command> commands;
    public final List<Map.Entry<Class<? extends Command>, Integer>> commandClassList;
    public final List<Map.Entry<Class<? extends Command>, Integer>> createCommandClassList;
    public final Class<? extends State> stateClass;
    public State state;

    public CommandSequence(List<Command> commands,
            List<Map.Entry<Class<? extends Command>, Integer>> commandClassList,
            List<Map.Entry<Class<? extends Command>, Integer>> createCommandClassList,
            Class<? extends State> stateClass, State state) {
        this.commands = commands;
        this.commandClassList = commandClassList;
        this.createCommandClassList = createCommandClassList;
        this.stateClass = stateClass;
        this.state = state;
    }

    public boolean separateFromFormerTest()
            throws NoSuchMethodException, InvocationTargetException,
            InstantiationException, IllegalAccessException {

        for (Command command : commands) {
            command.changeKeyspaceName(); // For separation
            command.updateState(state);
        }
        Constructor<?> constructor = stateClass.getConstructor();
        State state = (State) constructor.newInstance(); // Recreate a state
        List<Command> validCommands = new LinkedList<>();
        state = (State) constructor.newInstance(); // Recreate a state
        for (int i = 0; i < commands.size(); i++) {
            boolean fixable = checkAndUpdateCommand(commands.get(i), state);
            if (fixable) {
                validCommands.add(commands.get(i));
                updateState(commands.get(i), state);
            }
        }
        this.commands = validCommands;
        // System.out.println("\nSeparated Seq");
        // for (Command command : commands) {
        // System.out.println(command.toString());
        // }
        // System.out.println("\n");
        return true;
    }

    public boolean mutate()
            throws NoSuchMethodException, InvocationTargetException,
            InstantiationException, IllegalAccessException {
        /**
         * Choice
         * 0: Mutate the command (Call command.mutate)  // 2/3
         * 1: Insert a command                          // 1/3
         * 2: Replace a command                         // 0
         * 3: Delete a command // Temporary not chosen  // 0
         */
        separateFromFormerTest();
        addParamToTypePool();

        if (CassandraCommands.DEBUG) {
            System.out.println("String Pool:" + STRINGType.stringPool);
            System.out.println("Int Pool: " + INTType.intPool);
        }

        Random rand = new Random();
        for (int mutateRetryIdx = 0; mutateRetryIdx < RETRY_MUTATE_TIME; mutateRetryIdx++) {
            int choice = rand.nextInt(3);

            assert commands.size() > 0;
            Constructor<?> constructor = stateClass.getConstructor();
            State state = (State) constructor.newInstance(); // Recreate a state

            if (CassandraCommands.DEBUG) {
                choice = 2;
            }

            int pos;
            if (choice == 0 || choice == 1) {
                // Mutate a specific command

                // Compute the state up to the position
                pos = rand.nextInt(commands.size());
                System.out.println("\t\tMutate Command Pos " + pos);
                for (int i = 0; i < pos; i++) {
                    commands.get(i).updateState(state);
                }
                try {
                    boolean mutateStatus = commands.get(pos).mutate(state);
                    if (!mutateStatus)
                        continue;
                    boolean fixable = checkAndUpdateCommand(commands.get(pos),
                            state);
                    if (!fixable) {
                        // remove the command from command sequence...
                        commands.remove(pos);
                        pos -= 1;
                    } else {
                        updateState(commands.get(pos), state);
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            } else if (choice == 2) {
                /**
                 * Insert a command
                 */
                // Compute the state up to the position
                pos = org.zlab.upfuzz.utils.Utilities.biasRand(rand,
                        commands.size() + 1, 5);
                // pos = rand.nextInt(commands.size() + 1);
                System.out.println("\t\tMutate Command Pos " + pos);
                for (int i = 0; i < pos; i++) {
                    commands.get(i).updateState(state);
                }
                Command command;
                command = generateSingleCommand(commandClassList, state);
                while (command == null) {
                    command = generateSingleCommand(createCommandClassList,
                            state);
                }
                commands.add(pos, command);
                commands.get(pos).updateState(state);
            } else if (choice == 3) { // Disabled temporally
                // Replace a command
                // Compute the state up to the position
                pos = rand.nextInt(commands.size());
                for (int i = 0; i < pos; i++) {
                    commands.get(i).updateState(state);
                }
                Command command;
                if (pos <= 1) {
                    command = generateSingleCommand(createCommandClassList,
                            state);
                } else {
                    command = generateSingleCommand(commandClassList, state);
                }
                while (command == null) {
                    command = generateSingleCommand(createCommandClassList,
                            state);
                }
                commands.remove(pos);
                commands.add(pos, command);
                commands.get(pos).updateState(state);
            } else { // Disabled temporally
                // Delete a command
                // Compute the state up to the position
                pos = rand.nextInt(commands.size());
                for (int i = 0; i < pos; i++) {
                    commands.get(i).updateState(state);
                }
                commands.remove(pos);
                pos -= 1;
            }
            // Check the following commands
            /**
             * There could be some commands that cannot be
             * fixed. Therefore, remove them to keep the
             * validity.
             */
            List<Command> validCommands = new LinkedList<>();
            for (int i = 0; i < pos + 1; i++) {
                validCommands.add(commands.get(i));
            }
            for (int i = pos + 1; i < commands.size(); i++) {
                boolean fixable = checkAndUpdateCommand(commands.get(i), state);
                if (fixable) {
                    validCommands.add(commands.get(i));
                    updateState(commands.get(i), state);
                }
            }
            commands = validCommands;
            this.state = state;

            cleanTypePool();

            return true;
        }
        // The mutation is failed.
        System.out.println("Mutation Failed");
        return false;
    }

    public static Command generateSingleCommand(
            List<Map.Entry<Class<? extends Command>, Integer>> commandClassList,
            State state) {
        Command command = null;
        Random rand = new Random();
        assert commandClassList.isEmpty() == false;
        /**
         * Set Retry time is to avoid forever loop when all
         * the commands cannot be generated correctly.
         */
        for (int i = 0; i < RETRY_GENERATE_TIME; i++) {
            try {
                int sum = commandClassList.stream().mapToInt(a -> a.getValue())
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
                Constructor<?> constructor = null;
                try {
                    // TODO use state.getClass()
                    constructor = clazz.getConstructor(state.getClass());
                } catch (NoSuchMethodException e) {
                    constructor = clazz.getConstructor(State.class);
                }
                command = (Command) constructor.newInstance(state);
                command.updateState(state);
                break;
            } catch (Exception e) {
                e.printStackTrace(); // DEBUG
                command = null;
                break;
                // continue;
            }
        }

        return command;
    }

    public static CommandSequence generateSequence(
            List<Map.Entry<Class<? extends Command>, Integer>> commandClassList,
            List<Map.Entry<Class<? extends Command>, Integer>> createCommandClassList,
            Class<? extends State> stateClass, State state)
            throws NoSuchMethodException, InvocationTargetException,
            InstantiationException, IllegalAccessException {

        assert commandClassList != null;

        Random rand = new Random();
        assert MAX_CMD_SEQ_LEN > 0;
        int len = rand.nextInt(MAX_CMD_SEQ_LEN) + 1;

        Constructor<?> constructor = stateClass.getConstructor();
        if (state == null)
            state = (State) constructor.newInstance();

        // len = 8; // Debug
        List<Command> commands = new LinkedList<>();

        // List<Class<? extends Command>> tmpCommandClassList = new
        // LinkedList<>(); // Debug
        // tmpCommandClassList.add(CassandraCommands.ALTER_TABLE_DROP.class); //
        // Debug
        for (int i = 0; i < len; i++) {

            if (createCommandClassList != null) {
                /**
                 * Make sure the first three columns are write related command,
                 * so that the later command can be generated more easily.
                 * [Could be changed later]
                 */
                if (i <= 2) {
                    commands.add(generateSingleCommand(createCommandClassList,
                            state));
                    continue;
                } else {
                    Command command = generateSingleCommand(commandClassList,
                            state);
                    if (command == null) {
                        command = generateSingleCommand(createCommandClassList,
                                state);
                    }

                    commands.add(command);
                }
            } else {
                Command command = generateSingleCommand(commandClassList,
                        state);
                if (command == null) {
                    command = generateSingleCommand(commandClassList, state);
                }

                commands.add(command);
            }

        }

        cleanTypePool();

        return new CommandSequence(commands, commandClassList,
                createCommandClassList, stateClass, state);
    }

    public CommandSequence generateRelatedReadSequence() {
        /**
         * Given a command sequence, and a state from them
         * Return a read command sequence, by two ways
         */
        // Start from the state stored in current object

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
            commandStringList.add(command.toString());
        }
        return commandStringList;
    }

    public static boolean checkAndUpdateCommand(Command command, State state) {
        /**
         * Check whether current command is valid. Fix if not valid.
         * TODO: What if it cannot be fixed?
         * - simple solution, just return a false, and make command sequence remove it.
         * Update the command string
         */
        boolean fixable = command.regenerateIfNotValid(state);
        if (fixable)
            command.updateExecutableCommandString();
        return fixable;
    }

    public static boolean updateState(Command command, State state) {
        command.updateState(state);
        return true;
    }

    public static void cleanTypePool() {
        STRINGType.cleanPool();
        INTType.cleanPool();
    }

    public void addParamToTypePool() {
        for (Command command : commands) {
            // For each parameter, if the type is String or Int
            // Add them to the pool
            for (Parameter param : command.params) {
                Object val = param.getValue();
                add2Pool(val);
            }
        }
    }

    public void add2Pool(Object val) {
        if (val instanceof List) {
            List<Parameter> list = (List<Parameter>) val;
            for (Parameter param : list) {
                add2Pool(param.getValue());
            }
        } else if (val instanceof Pair) {
            Pair<Parameter, Parameter> pair = (Pair<Parameter, Parameter>) val;
            add2Pool(pair.left.getValue());
            add2Pool(pair.right.getValue());
        } else if (val instanceof String) {
            if (!((String) val).contains(" "))
                STRINGType.stringPool.add((String) val);
        } else if (val instanceof Integer) {
            INTType.intPool.add((Integer) val);
        }
    }

}
