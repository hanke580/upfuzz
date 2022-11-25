package org.zlab.upfuzz.cassandra.nodetool;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommand;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.Utilities;

import java.util.LinkedList;
import java.util.List;

public class REMOVENODE extends CassandraCommand {

    public static List<String> options = new LinkedList<>();

    static {
        options.add("status");
        options.add("force");
    }

    public REMOVENODE(CassandraState state) {
        Parameter operation = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities.strings2Parameters(
                        (((REMOVENODE) c).options)),
                null).generateRandomParameter(state, this);
        params.add(operation);
    }

    @Override
    public String constructCommandString() {
        return "removenode" + " " + params.get(0).toString();
    }

    @Override
    public void updateState(State state) {

    }

}
