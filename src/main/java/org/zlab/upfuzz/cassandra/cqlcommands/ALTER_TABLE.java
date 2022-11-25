package org.zlab.upfuzz.cassandra.cqlcommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommand;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.Utilities;

public class ALTER_TABLE extends CassandraCommand {
    public ALTER_TABLE(CassandraState state) {

        Parameter keyspaceName = chooseKeyspace(state, this, null);
        this.params.add(keyspaceName); // 0

        Parameter TableName = chooseTable(state, this, null);
        this.params.add(TableName); // 1

        Parameter speculative_retry = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities.strings2Parameters(
                        CREATE_TABLE.speculative_retryOptions),
                null).generateRandomParameter(null, null);

        params.add(speculative_retry); // [2]
    }

    @Override
    public String constructCommandString() {
        return "ALTER TABLE " + params.get(0).toString() + "."
                + params.get(1).toString() + " " +
                "WITH" + " speculative_retry = '" + params.get(2).toString()
                + "';";
    }

    @Override
    public void updateState(State state) {
    }
}
