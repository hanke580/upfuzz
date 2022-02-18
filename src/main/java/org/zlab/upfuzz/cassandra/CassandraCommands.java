package org.zlab.upfuzz.cassandra;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterFactory;
import org.zlab.upfuzz.SharedVPolicy;

import java.util.Map;

public class CassandraCommands {

    public static class CREATETABLE implements Command {

        // parameters used in this command
        // they are generated following user-specified constraints

        final TEXTTypeFactory.TEXTType tableName;
        final IntegerType len;
        final Map<String, ParameterFactory> colName2Type;
        final Map<String, ParameterFactory> primaryColName2Type;
        final TEXTTypeFactory.TEXTType IF_NOT_EXIST;
        // final Command ...;

        public CREATETABLE(CassandraValidState state) {
            // constraints are specified here
            SharedVPolicy policy = new SharedVPolicy<TEXTTypeFactory.TEXTType>();
            tableName = (TEXTTypeFactory.TEXTType) policy.chooseExcept(state.tables, 1).get(0);

            len = IntegerTypeFactory.constructRandom(); // IntegerType obj;
            colName2Type = null;
            primaryColName2Type = null;
            IF_NOT_EXIST = null;
        }

        @Override
        public String constructCommand() {
            return null;
        }

        @Override
        public void updateState() {

        }
    }

    public static class INSERT implements Command {
        @Override
        public String constructCommand() {
            return null;
        }

        @Override
        public void updateState() {

        }
    }

}
