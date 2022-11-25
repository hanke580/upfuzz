package org.zlab.upfuzz.cassandra.cqlcommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommand;
import org.zlab.upfuzz.utils.STRINGType;

public class ALTER_ROLE extends CassandraCommand {
    /**
     * ALTER ROLE role_name
     * [WITH [PASSWORD = 'password']
     *    [LOGIN = true | false]
     *    [SUPERUSER = true | false]
     *    [OPTIONS = map_literal]]
     *
     * ALTER ROLE coach WITH PASSWORD='bestTeam';
     */

    public ALTER_ROLE(State state) {
        super();

        ParameterType.ConcreteType roleNameType = new ParameterType.NotEmpty(
                new STRINGType(10));
        Parameter roleName = roleNameType.generateRandomParameter(state, this);
        params.add(roleName);

        ParameterType.ConcreteType passwordType = new ParameterType.NotEmpty(
                new STRINGType(10));
        Parameter password = passwordType.generateRandomParameter(state, this);
        params.add(password);
    }

    @Override
    public String constructCommandString() {

        return "ALTER ROLE " +
                params.get(0).toString() + " WITH " +
                "PASSWORD = '" + params.get(1).toString() + "';";
    }

    @Override
    public void updateState(State state) {
    }
}
