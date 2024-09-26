package org.zlab.upfuzz.hbase.security;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.utils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class GRANT extends HBaseCommand {
    // store the permission options
    StringBuilder options;

    // TODO: allow optional tableName, row name, and columnfamily name options
    public GRANT(HBaseState state) {
        super(state);
        options = new StringBuilder();
        ParameterType.ConcreteType usernameType = new ParameterType.NotEmpty(
                new STRINGType(7));
        Parameter username = usernameType.generateRandomParameter(state, this);
        this.params.add(username);

        // permissions
        ArrayList<String> allOptions = new ArrayList<>(
                Arrays.asList(PERMISSION_OPTIONS));
        Parameter numOptions = new INTType(1, 5).generateRandomParameter(state,
                this);

        Random rand = new Random();
        for (int i = 0; i < Integer.parseInt(numOptions.toString()); i++) {
            int randomIndex = rand.nextInt(allOptions.size());
            options.append(allOptions.get(randomIndex));
            allOptions.remove(randomIndex);
        }
        Parameter tableName = chooseTable(state, this, null);
        this.params.add(tableName);
    }

    @Override
    public String constructCommandString() {
        Parameter username = this.params.get(0);
        Parameter tableName = this.params.get(1);
        return String.format(
                "grant '%s', '%s', '%s'",
                username.toString(), options.toString(), tableName);
    }

    @Override
    public void updateState(State state) {
        // nothing to do here
    }
}