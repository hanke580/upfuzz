package org.zlab.upfuzz;

import org.zlab.upfuzz.cassandra.CassandraCommands;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

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
                           Class<? extends State> stateClass,
                           State state) {
        this.commands = commands;
        this.commandClassList = commandClassList;
        this.createCommandClassList = createCommandClassList;
        this.stateClass = stateClass;
        this.state = state;
    }

    public boolean separateFromFormerTest() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {


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
        return true;
    }

    public boolean mutate() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        /**
         * Choice
         * 0: Mutate the command (Call command.mutate)  // 2/3
         * 1: Insert a command                          // 1/3
         * 2: Replace a command                         // 0
         * 3: Delete a command // Temporary not chosen  // 0
         */
        separateFromFormerTest();

        Random rand = new Random();

        for (int mutateRetryIdx = 0; mutateRetryIdx < RETRY_MUTATE_TIME; mutateRetryIdx++) {
            int choice = rand.nextInt(3);
            int pos = rand.nextInt(commands.size());

            assert commands.size() > 0;
            Constructor<?> constructor = stateClass.getConstructor();
            State state = (State) constructor.newInstance(); // Recreate a state

            if (CassandraCommands.DEBUG) {
                pos = 1;
                choice = 2;
            }
            System.out.println("\tMutate Command Pos " + pos);

            // Compute the state up to the position
            for (int i = 0; i < pos; i++) {
                commands.get(i).updateState(state);
            }

            if (choice == 0 || choice == 1) {
                // Mutate a specific command
                try {

                    boolean mutateStatus = commands.get(pos).mutate(state);
                    if (!mutateStatus) continue;
                    boolean fixable = checkAndUpdateCommand(commands.get(pos), state);
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
                 * Two options
                 * 1. Purely random generate one from this state.
                 * 2. (TODO) Pick command from the command pool.
                 */
                Command command;
                List<Map.Entry<Class<? extends Command>, Integer>> tmpL = new LinkedList<>();
                tmpL.add(new AbstractMap.SimpleImmutableEntry<>(CassandraCommands.CREATE_TABLE.class, 2) );
                command = generateSingleCommand(tmpL, state);
                commands.add(pos, command);
                commands.get(pos).updateState(state);
            } else if (choice == 3) { // Disabled temporally
                // Replace a command
                /**
                 * TODO: Pick from the command pool
                 */
                Command command;
                if (pos <= 1) {
                    command = generateSingleCommand(createCommandClassList, state);
                } else {
                    command = generateSingleCommand(commandClassList, state);
                }
                commands.remove(pos);
                commands.add(pos, command);
                commands.get(pos).updateState(state);
            } else { // Disabled temporally
                // Delete a command
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
            return true;
        }
        // The mutation is failed.
        System.out.println("Mutation Failed");
        return false;
    }

    public static Command generateSingleCommand(List<Map.Entry<Class<? extends Command>, Integer>> commandClassList,
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
                int sum = commandClassList.stream().mapToInt(a -> a.getValue()).sum();

                int tmpSum = 0;
                int randInt = rand.nextInt(sum);
                int cmdIdx = 0;

                for (int j = 0; j < sum; j++) {
                    tmpSum += commandClassList.get(j).getValue();
                    if (randInt < tmpSum)
                        break;
                    cmdIdx++;
                }
                Class<? extends Command> clazz = commandClassList.get(cmdIdx).getKey();
                Constructor<?> constructor = clazz.getConstructor(State.class);

                command = (Command) constructor.newInstance(state);
                command.updateState(state);
                break;
            } catch (Exception e) {
//                e.printStackTrace(); // DEBUG
                command = null;
                continue;
            }
        }
        if (command == null) {
            System.out.println("A problem with generating single command");
            // throw new RuntimeException("A problem with generating single command");
            // System.exit(1);
        }
        return command;
    }

    /**
     * Generate a new test sequence
     * - Input: null
     * - Output: TestSequence
     */
    public static CommandSequence generateSequence(List<Map.Entry<Class<? extends Command>, Integer>> commandClassList,
                                                   List<Map.Entry<Class<? extends Command>, Integer>> createCommandClassList,
                                                   Class<? extends State> stateClass, State state)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

        assert commandClassList != null;

        Random rand = new Random();
        assert MAX_CMD_SEQ_LEN > 0;
        int len = rand.nextInt(MAX_CMD_SEQ_LEN) + 1;

        Constructor<?> constructor = stateClass.getConstructor();
        if (state == null)
            state = (State) constructor.newInstance();

//        len = 8; // Debug
        List<Command> commands = new LinkedList<>();

//        List<Class<? extends Command>> tmpCommandClassList = new LinkedList<>(); // Debug
//        tmpCommandClassList.add(CassandraCommands.ALTER_TABLE_DROP.class); // Debug
        for (int i = 0; i < len; i++) {
            if (createCommandClassList != null) {
                /**
                 * Make sure the first three columns are write related command,
                 * so that the later command can be generated more easily.
                 * [Could be changed later]
                 */
                if (i <= 2) {
                    commands.add(generateSingleCommand(createCommandClassList, state));
                    continue;
                } else {
                    Command command = generateSingleCommand(commandClassList, state);
                    if (command == null) {
                        command = generateSingleCommand(createCommandClassList, state);
                    }
                    commands.add(command);
                }
            } else {
                Command command = generateSingleCommand(commandClassList, state);
                while (command == null) {           // Might stuck here...
                    command = generateSingleCommand(commandClassList, state);
                }
                commands.add(command);
            }
        }
        return new CommandSequence(commands, commandClassList, createCommandClassList, stateClass, state);
    }

    public CommandSequence generateRelatedReadSequence() {
        /**
         * Given a command sequence, and a state from them
         * Return a read command sequence, by two ways
         */
        // Start from the state stored in current object

        List<Command> commands = new LinkedList<>();
        for (Command command : this.commands) {
            Set<Command> readCommands = command.generateRelatedReadCommand(this.state);
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

        return new CommandSequence(commands, commandClassList, createCommandClassList, stateClass, state);
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

}
