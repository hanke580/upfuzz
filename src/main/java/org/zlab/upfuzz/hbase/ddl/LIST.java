package org.zlab.upfuzz.hbase.ddl;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

import java.util.Random;

public class LIST extends HBaseCommand {
    private final StringBuilder sb;
    private final Random rand;

    public LIST(HBaseState state) {
        super(state);
        sb = new StringBuilder();
        rand = new Random();
        generateRegex();
    }

    @Override
    public String constructCommandString() {
        return "list"
                + (rand.nextBoolean() ? "" : (" '" + sb.toString() + "'"));
    }

    @Override
    public void updateState(State state) {
    }

    private char getRandomChar() {
        int index = rand.nextInt(HBaseCommand.REGEX_CHARS.length());
        return HBaseCommand.REGEX_CHARS.charAt(index);
    }

    @Override
    public boolean mutate(State s) throws Exception {
        // the table name of different create commands should be unique
        sb.setLength(0);
        generateRegex();
        return true;
    }

    private void generateRegex() {
        int maxLength = 3;

        switch (rand.nextInt(3)) {
        // filter start expression
        case 0:
            sb.append("uuid");
            for (int i = 0; i < rand.nextInt(maxLength) + 1; i++)
                sb.append(getRandomChar());
            sb.append(".*");
            break;
        // filter middle of expression
        case 1:
            sb.append(".*");
            for (int i = 0; i < rand.nextInt(maxLength) + 1; i++)
                sb.append(getRandomChar());
            sb.append(".*");
            break;
        case 2:
            // filter end of expression
            sb.append(".*");
            for (int i = 0; i < rand.nextInt(maxLength) + 1; i++)
                sb.append(getRandomChar());
            sb.append("$");
            break;
        }
    }
}
