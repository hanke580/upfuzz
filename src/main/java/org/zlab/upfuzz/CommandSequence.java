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

    public CommandSequence(List<Command> commands,
                           List<Class<? extends Command>> commandClassList,
                           State state) {
        this.commands = commands;
        this.commandClassList = commandClassList;
        this.state = state;
    }

    /**
     * TestSequence mutate()
     * - Need to do all level mutation, including the command pool
     * - Return a new mutated test sequence
     * Decide the level of mutation.
     * 1. Insert a command.
     * 2. Replace a command.
     * 3. Delete a command.
     */
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
        choice = 3; // DEBUG

        assert commands.size() > 0;
        // pos to insert
        int pos = rand.nextInt(commands.size());
        System.out.println("\n pos = " + pos + "\n");

        state.clearState();



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
            int cmdIdx = rand.nextInt(commandClassList.size());
            Class<? extends Command> clazz = commandClassList.get(cmdIdx);
            Constructor<?> constructor = null;
            try {
                constructor = clazz.getConstructor(State.class);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }

            Command cmd = null;
            try {
                assert constructor != null;
                cmd = (Command) constructor.newInstance(state);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
            assert cmd != null;

            System.out.println("Added command : " + cmd.constructCommandString());
            System.out.println("\n");

            cmd.updateState(state);
            commands.add(pos, cmd);

            // Invoke check() function from the that position
            for (int i = pos + 1; i < commands.size(); i++) {
                commands.get(i).regenerateIfNotValid(state, commands.get(i)); // same func as regenerateIfNotValid
                commands.get(i).updateState(state);
            }
        } else if (choice == 1) {
            // Replace a command
            /**
             * 1. Generate a new command / Pick command from the command pool (Not impl yet)
             * 2. Delete that command, and add the newly generated command in that place
             */

        } else if (choice == 2) {
            // Delete a command
        } else {
            // Mutate a specific command
            try {
                commands.get(pos).mutate(state);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
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
            int cmdIdx = rand.nextInt(commandClassList.size());
            Class<? extends Command> clazz = commandClassList.get(cmdIdx);
            Constructor<?> constructor = clazz.getConstructor(State.class);

            Command cmd = (Command) constructor.newInstance(state);
            cmd.updateState(state);
            commands.add(cmd);
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
