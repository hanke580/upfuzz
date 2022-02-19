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
        final LinkedHashMap<Parameter, Parameter> colName2Type = new LinkedHashMap<>();

        final LinkedHashMap<Parameter, Parameter> primaryColName2Type = new LinkedHashMap<>();

        final Parameter IF_NOT_EXIST;

        // final Command ...; // Nested commands need to be constructed first.

        public CREATETABLE(CassandraState state) {

            // constraints are specified here
            tableName = new Parameter(CassandraTypes.TEXTType.instance) {
                @Override
                public void generateValue(State state, Command currCmd) {
                    value = type.constructRandomValue();
                    while (isValid(state, currCmd)) {
                        value = type.constructRandomValue();
                    }
                }

                @Override
                public boolean isValid(State state, Command currCmd) {
                    Set<String> tableNames =
                        ((CassandraState) state).tables.stream().map(s -> s.name).collect(Collectors.toSet());
                    return !tableNames.contains(value);
                }
            };
            tableName.generateValue(state, this);

            int bound = 10; // specified by user
            int len = new Random().nextInt(bound);

            for (int i = 0; i < len; i++) {

                Parameter p1 = new Parameter(CassandraTypes.TEXTType.instance) {
                    @Override
                    public void generateValue(State state, Command currCmd) {
                        // p1's value doesn't exist in currCmd.colName2Type.keySet();
                        // This might cause concurrent modification to colName2Type!
                        // There is a way to use iterator to do concurrent modification.
                    }
                };
                p1.generateValue(state, this);

                Parameter p2 = new TypeParameter();
                p2.generateValue(state, this);
                this.colName2Type.put(p1, p2);
            }


            IF_NOT_EXIST = null;
        }

        @Override
        public String constructCommandString() {
            return null;
        }

        @Override
        public void updateState() {

        }
    }

    public static class INSERT implements Command {
        @Override
        public String constructCommandString() {
            return null;
        }

        @Override
        public void updateState() {

        }
    }

}
