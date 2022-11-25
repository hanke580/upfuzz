package org.zlab.upfuzz.cassandra.nodetool;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommand;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.Utilities;

public class REBUILD_INDEX extends CassandraCommand {

    public REBUILD_INDEX(CassandraState state) {

        Parameter keyspaceName = chooseKeyspace(state, this, null);
        params.add(keyspaceName); // P0

        Parameter TableName = chooseTable(state, this, null);
        params.add(TableName); // P1

        ParameterType.ConcreteType indexNameType = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities
                        .strings2Parameters(((CassandraState) s).keyspace2tables
                                .get(c.params.get(0).toString())
                                .get(c.params.get(1).toString()).indexes),
                null);

        Parameter indexName = indexNameType.generateRandomParameter(state,
                this);
        params.add(indexName); // P1
    }

    @Override
    public String constructCommandString() {
        return "rebuild_index" + " " + params.get(0).toString() + " "
                + params.get(1).toString() + " " + params.get(2).toString();
    }

    @Override
    public void updateState(State state) {

    }
}
