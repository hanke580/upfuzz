package org.zlab.upfuzz.cassandra.cqlcommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommand;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.utils.INTType;

/**
 * ALTER  KEYSPACE keyspace_name
 *    WITH REPLICATION = {
 *       'class' : 'SimpleStrategy', 'replication_factor' : N
 *      | 'class' : 'NetworkTopologyStrategy', 'dc1_name' : N [, ...]
 *    }
 *    [AND DURABLE_WRITES =  true|false] ;
 */
public class ALTER_KEYSPACE extends CassandraCommand {
    /**
     * a parameter should correspond to one variable in the text format of this command.
     * mutating a parameter could depend on and the state updated by all nested internal commands and other parameters.
     * Note: Thus, we need to be careful to not have cyclic dependency among parameters.
     */

    // final Command ...; // Nested commands need to be constructed first.

    public ALTER_KEYSPACE(State state) {
        super();

        assert state instanceof CassandraState;
        CassandraState cassandraState = (CassandraState) state;

        Parameter keyspaceName = chooseKeyspace(state, this, null);
        params.add(keyspaceName); // [0]

        ParameterType.ConcreteType replicationFactorType = new INTType(1,
                4);
        Parameter replicationFactor = replicationFactorType
                .generateRandomParameter(state, this);
        this.params.add(replicationFactor); // [1]
    }

    @Override
    public String constructCommandString() {
        return "ALTER KEYSPACE" + " " + this.params.get(0).toString()
                + " " +
                "WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' :"
                + " " +
                this.params.get(1).toString() + " " + "};";
    }

    @Override
    public void updateState(State state) {
    }
}