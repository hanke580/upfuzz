package org.zlab.upfuzz.hbase.namespace;

import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;

import java.util.Random;

public class LIST_NAMESPACE extends HBaseCommand {
    StringBuilder sb;
    Random random;

    public LIST_NAMESPACE(HBaseState state) {
        super(state);
        sb = new StringBuilder();
        random = new Random();
        int maxLength = 2;

        switch (random.nextInt(6)) {
        // filter start expression
        case 0:
            for (int i = 0; i < random.nextInt(maxLength) + 1; i++)
                sb.append(getRandomChar());
            sb.append(".*");
            break;
        // filter middle of expression
        case 1:
            sb.append(".*");
            for (int i = 0; i < random.nextInt(maxLength) + 1; i++)
                sb.append(getRandomChar());
            sb.append(".*");
            break;
        case 2:
            // filter end of expression
            sb.append(".*");
            for (int i = 0; i < random.nextInt(maxLength) + 1; i++)
                sb.append(getRandomChar());
            sb.append("$");

            break;
        default:
            // case 3, 4, 5:
            // nothing, in these cases, don't use the regex optional param
        }
    }

    @Override
    public String constructCommandString() {
        return "list_namespace" +
                (sb.length() == 0 ? "" : " '" + sb.toString() + "'");
    }

    @Override
    public void updateState(State state) {
    }

    private char getRandomChar() {
        int index = random.nextInt(HBaseCommand.REGEX_CHARS.length());
        return HBaseCommand.REGEX_CHARS.charAt(index);
    }
}
