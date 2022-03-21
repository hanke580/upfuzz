package org.zlab.upfuzz;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class CommandSequence {

    public static final int MAX_CMD_SEQ_LEN = 10;

    public final List<Command> commands;
    public final List<Class<? extends Command>> commandClassList;
    public final State state;
    // TODO: More reasoning about the state.

    public final static int RETRY_GENERATE_TIME = 20;

    public static Command generateSingleCommand(List<Class<? extends Command>> commandClassList,
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
                int cmdIdx = rand.nextInt(commandClassList.size());
                Class<? extends Command> clazz = commandClassList.get(cmdIdx);
                Constructor<?> constructor = clazz.getConstructor(State.class);

                command = (Command) constructor.newInstance(state);
                command.updateState(state);
                break;
            } catch (Exception e) {
//                    e.printStackTrace();
//                    System.out.println("Exception with forever loop");
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

    public CommandSequence(List<Command> commands,
                           List<Class<? extends Command>> commandClassList,
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
        // TODO: Impl the rest two choices.
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
                commands.get(pos).updateState(state);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        // Check the following commands
        for (int i = pos + 1; i < commands.size(); i++) {
            commands.get(i).regenerateIfNotValid(state, commands.get(i)); // same func as regenerateIfNotValid
            commands.get(i).updateState(state);
        }
        return this;
    }

    /**
     * Generate a new test sequence
     * - Input: null
     * - Output: TestSequence
     */
    public static CommandSequence generateSequence(List<Class<? extends Command>> commandClassList,
                                                   State state)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

        Random rand = new Random();
        assert MAX_CMD_SEQ_LEN > 0;
        int len = rand.nextInt(MAX_CMD_SEQ_LEN) + 1;

        len = 8; // Debug
//        State state = stateClazz.getConstructor().newInstance();

        List<Command> commands = new LinkedList<>();

        for (int i = 0; i < len; i++) {
            // TODO: Prioritize create table like function, make sure they are at front.
            commands.add(generateSingleCommand(commandClassList, state));
        }
        return new CommandSequence(commands, commandClassList, state);
    }

    public List<String> getCommandStringList() {
        List<String> commandStringList = new ArrayList<>();
        for (Command command : commands) {
            commandStringList.add(command.constructCommandString());
        }
        return commandStringList;
    }
}
