package org.zlab.upfuzz.cassandra;

import org.zlab.upfuzz.*;
import org.zlab.upfuzz.utils.PAIRType;

import java.util.*;
import java.util.stream.Collectors;

public class CassandraCommands {

    public static class CREATETABLE implements Command {

        // parameters used in this command
        // they are generated following user-specified constraints

        // a parameter should correspond to one variable in the text format of this command.
        // mutating a parameter could depend on and the state updated by all nested internal commands and other parameters.
        // Note: Thus, we need to be careful to not have cyclic dependency among parameters.

        final Parameter tableName; // TEXTType

        // LinkedHashMap is a list of pairs.
        // key: TEXTType parameter columnName; value: any user defined type
        final LinkedHashMap<Parameter, Parameter> colName2Type = new LinkedHashMap<>();

        final Parameter columns;

        final LinkedHashMap<Parameter, Parameter> primaryColName2Type = new LinkedHashMap<>();

        final Parameter IF_NOT_EXIST;

        // final Command ...; // Nested commands need to be constructed first.

        public CREATETABLE(CassandraState state) {

            tableName = CassandraTypes.TEXTType.instance.generateRandomParameter(state, this);

            ParameterType.ConcreteType columnsType = // LIST<PAIR<TEXT,TYPE>>
                ParameterType.ConcreteGenericType.constructConcreteGenericType(
                    CassandraTypes.LISTType.instance,
                    ParameterType.ConcreteGenericType.constructConcreteGenericType(PAIRType.instance,
                        CassandraTypes.TEXTType.instance, CassandraTypes.TYPEType.instance));

            columns = columnsType.generateRandomParameter(state, this);

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
