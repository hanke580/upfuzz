package org.zlab.upfuzz;

import org.zlab.upfuzz.cassandra.CassandraCommands;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class CommandSequence {

    public static final int MAX_CMD_SEQ_LEN = 20;

    public final List<Command> commands;
    public final List<Map.Entry<Class<? extends Command>, Integer>> commandClassList;
    public final State state;

    public final static int RETRY_GENERATE_TIME = 20;

    public CommandSequence(List<Command> commands,
                           List<Map.Entry<Class<? extends Command>, Integer>> commandClassList,
                           State state) {
        this.commands = commands;
        this.commandClassList = commandClassList;
        this.state = state;
    }

    public CommandSequence mutate(State state) {

        Random rand = new Random();

        int choice = rand.nextInt(4);
        /**
         * 0: Insert a command
         * 1: Replace a command
         * 2: Delete a command
         * 3: Mutate the command (Call command.mutate)
         */
//        choice = 3; // DEBUG
        switch (choice) {
            case 0:
                System.out.println("insert a command");
                break;
            case 1:
                System.out.println("replace a command");
                break;
            case 2:
                System.out.println("delete a command");
                break;
            case 3:
                System.out.println("mutate a specific command");
        }

        assert commands.size() > 0;
        // pos to insert
        int pos = rand.nextInt(commands.size());
//        pos = 1; // DEBUG
        System.out.println("Mutate Command Index = " + pos);

        state.clearState();

        // Compute the state up to the position
        for (int i = 0; i < pos; i++) {
            commands.get(i).updateState(state);
        }

        if (choice == 0) {
            // Insert a command
            /**
             * Now regenerate the state from beginning to the mutated command.
             * - Opt1: Save the state for each command.
             *      - PRO: Eliminate the overhead computing state from 0 to i - 1
             *      - CON: Takes more space
             * - Opt2: Re-compute from beginning. Takes more time, but save space.
             *      - (Use this for now)
             */
            // TODO: Need more reasoning whether use this: compute state from beginning
            // generate a new command
            /**
             * Two options
             * 1. Purely random generate one from this state.
             * 2. (TODO) Pick command from the command pool
             */
            Command command = generateSingleCommand(commandClassList, state);

            System.out.println("Added command : " + command.constructCommandString());
            System.out.println("\n");

            commands.add(pos, command);
            commands.get(pos).updateState(state);

        } else if (choice == 1) {
            // Replace a command
            /**
             * TODO: Pick from the command pool
             */
            Command command = generateSingleCommand(commandClassList, state);
            commands.remove(pos);
            commands.add(command);
        } else if (choice == 2) {
            // Delete a command
            commands.remove(pos);
            pos -= 1;
        } else {
            // Mutate a specific command
            try {
                commands.get(pos).mutate(state);

                checkAndUpdateCommand(commands.get(pos), state);
                updateState(commands.get(pos), state);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        // Check the following commands
        /**
         * TODO: There could be some commands that cannot be fixed.
         * These commands need to be removed/should be handled
         * by some other ways...
         * Like regenerate a new one
         */
        for (int i = pos + 1; i < commands.size(); i++) {
            checkAndUpdateCommand(commands.get(i), state);
            updateState(commands.get(i), state);
        }
        return this;
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
            System.exit(1);
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
                                                   State state)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

        Random rand = new Random();
        assert MAX_CMD_SEQ_LEN > 0;
        int len = rand.nextInt(MAX_CMD_SEQ_LEN) + 1;

//        len = 8; // Debug
//        State state = stateClazz.getConstructor().newInstance();

        List<Command> commands = new LinkedList<>();


//        List<Class<? extends Command>> tmpCommandClassList = new LinkedList<>(); // Debug
//        tmpCommandClassList.add(CassandraCommands.ALTER_TABLE_DROP.class); // Debug

        for (int i = 0; i < len; i++) {
            if (i <= 1) {
                commands.add(generateSingleCommand(createCommandClassList, state));
                continue;
            } else {
                commands.add(generateSingleCommand(commandClassList, state));
            }
        }
        return new CommandSequence(commands, commandClassList, state);
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
        command.regenerateIfNotValid(state);
        command.updateExecutableCommandString();;
        return true;
    }

    public static boolean updateState(Command command, State state) {
        command.updateState(state);
        return true;
    }

}
