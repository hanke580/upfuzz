package org.zlab.upfuzz.cassandra;

import org.zlab.upfuzz.*;

import java.util.*;
import java.util.stream.Collectors;

public class CassandraCommands {

    public static class CREATETABLE implements Command {

        // parameters used in this command
        // they are generated following user-specified constraints

        // a parameter should correspond to one variable in the text format of this command.
        // mutating a parameter could depend on and the state updated by all nested internal commands and other parameters.
        // Note: Thus, we need to be careful to not have cyclic dependency among parameters.

        final Parameter tableName;

        // LinkedHashMap is a list of pairs.
        // key: TEXTType parameter columnName; value: any user defined type
        final LinkedHashMap<Parameter, Parameter> colName2Type;

        final LinkedHashMap<Parameter, Parameter> primaryColName2Type;

        final Parameter IF_NOT_EXIST;

        // final Command ...; // Nested commands need to be constructed first.

        public CREATETABLE(CassandraValidState state) {

            // constraints are specified here
            tableName = new Parameter(TEXTType.instance) {
                @Override
                public void generateValue(State state, Command currCmd) {
                    value = type.constructRandomValue();
                    Set<String> tableNames =
                            ((CassandraValidState) state).tables.stream().map(s -> s.name).collect(Collectors.toSet());
                    while (tableNames.contains(value)) {
                        value = type.constructRandomValue();
                    }
                }
            };
            tableName.generateValue(state, this);


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
