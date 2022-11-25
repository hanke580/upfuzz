package org.zlab.upfuzz.cassandra.nodetool;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommand;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.utils.BIGINTType;
import org.zlab.upfuzz.utils.BOOLType;

public class MOVE extends CassandraCommand {

    public MOVE(CassandraState state) {

        Parameter isPositive = new BOOLType().generateRandomParameter(state,
                this);
        params.add(isPositive);

        Parameter newToken = new BIGINTType(64).generateRandomParameter(state,
                this);
        params.add(newToken);
    }

    @Override
    public String constructCommandString() {
        if (((Boolean) params.get(0).getValue())) {
            return "move" + " " + params.get(1).toString();
        } else {
            return "move" + " \\-" + params.get(1).toString();
        }
    }

    @Override
    public void updateState(State state) {

    }
}
