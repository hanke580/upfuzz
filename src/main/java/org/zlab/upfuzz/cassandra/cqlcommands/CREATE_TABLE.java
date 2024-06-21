package org.zlab.upfuzz.cassandra.cqlcommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommand;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.cassandra.CassandraTable;
import org.zlab.upfuzz.cassandra.CassandraTypes;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.PAIRType;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.STRINGType;
import org.zlab.upfuzz.utils.Utilities;

import org.zlab.upfuzz.fuzzingengine.Config;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class CREATE_TABLE extends CassandraCommand {
    /**
     * a parameter should correspond to one variable in the text format of this command.
     * mutating a parameter could depend on and the state updated by all nested internal commands and other parameters.
     * Note: Thus, we need to be careful to not have cyclic dependency among parameters.
     */

    // final Command ...; // Nested commands need to be constructed first.

    public static List<String> speculative_retryOptions = new LinkedList<>();

    static {
        speculative_retryOptions.add("50ms");
        speculative_retryOptions.add("90MS");
        speculative_retryOptions.add("99PERCENTILE");
        speculative_retryOptions.add("40percentile");
        speculative_retryOptions.add("ALWAYS");
        speculative_retryOptions.add("always");
        speculative_retryOptions.add("NONE");
        speculative_retryOptions.add("none");
    }

    public CREATE_TABLE(CassandraState state, Object init0, Object init1,
            Object init2, Object init3, Object init4) {
        Parameter keyspaceName = chooseKeyspace(state, this, init0);
        params.add(keyspaceName); // [0]

        ParameterType.ConcreteType tableNameType = new ParameterType.NotInCollectionType(
                new ParameterType.NotEmpty(new STRINGType(10)),
                (s, c) -> Utilities
                        .strings2Parameters(((CassandraState) s).keyspace2tables
                                .get(this.params.get(0).toString()).keySet()),
                null);

        Parameter tableName = tableNameType
                .generateRandomParameter(state, this, init1);
        params.add(tableName); // [1]

        ParameterType.ConcreteType columnsType = // LIST<PAIR<String,TYPEType>>
                new ParameterType.NotEmpty(ParameterType.ConcreteGenericType
                        .constructConcreteGenericType(
                                CassandraTypes.MapLikeListColumnType.instance,
                                ParameterType.ConcreteGenericType
                                        .constructConcreteGenericType(
                                                PAIRType.instance,
                                                new ParameterType.NotEmpty(
                                                        new ParameterType.NotStartWithNumber(
                                                                new STRINGType(
                                                                        Config.getConf().CASSANDRA_COLUMN_NAME_MAX_SIZE))),
                                                CassandraTypes.TYPEType.instance)));
        Parameter columns = columnsType
                .generateRandomParameter(state, this, init2);
        params.add(columns); // [2]

        ParameterType.ConcreteType primaryColumnsType = new ParameterType.NotEmpty(
                new CassandraTypes.PartitionSubsetType<>(columnsType,
                        (s, c) -> (Collection<Parameter>) c.params.get(2)
                                .getValue(),
                        null));

        Parameter primaryColumns = primaryColumnsType
                .generateRandomParameter(state, this, init3);
        params.add(primaryColumns); // [3]

        ParameterType.ConcreteType IF_NOT_EXISTType = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("IF NOT EXISTS"), null // TODO: Make
        // a pure
        // CONSTANTType
        );
        Parameter IF_NOT_EXIST = IF_NOT_EXISTType
                .generateRandomParameter(state, this, init4);
        params.add(IF_NOT_EXIST); // [4]

        Parameter speculative_retry = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities.strings2Parameters(
                        speculative_retryOptions),
                null).generateRandomParameter(null, null);

        params.add(speculative_retry); // [5]
    }

    public CREATE_TABLE(CassandraState state) {
        Parameter keyspaceName = chooseKeyspace(state, this, null);
        params.add(keyspaceName); // [0]

        ParameterType.ConcreteType tableNameType = new ParameterType.LessLikelyMutateType(
                new ParameterType.NotInCollectionType(
                        new ParameterType.NotEmpty(new STRINGType(10)),
                        (s, c) -> Utilities
                                .strings2Parameters(
                                        ((CassandraState) s).keyspace2tables
                                                .get(this.params.get(0)
                                                        .toString())
                                                .keySet()),
                        null),
                0.1);

        Parameter tableName = tableNameType
                .generateRandomParameter(state, this);
        params.add(tableName); // [1]

        ParameterType.ConcreteType columnsType = // LIST<PAIR<String,TYPEType>>
                new ParameterType.NotEmpty(ParameterType.ConcreteGenericType
                        .constructConcreteGenericType(
                                CassandraTypes.MapLikeListColumnType.instance,
                                ParameterType.ConcreteGenericType
                                        .constructConcreteGenericType(
                                                PAIRType.instance,
                                                new ParameterType.NotEmpty(
                                                        new ParameterType.NotStartWithNumber(
                                                                new STRINGType(
                                                                        20))),
                                                CassandraTypes.TYPEType.instance)));

        Parameter columns = columnsType
                .generateRandomParameter(state, this);
        params.add(columns); // [2]

        /**
         * Bool variable check whether the previous columns has any member that's already specified as
         * Primary Key
         * - True
         *      - Shouldn't generate the third param
         * - False
         *      - Should generate
         *
         * Impl this check as a type
         * - Take a previous parameter as input
         *      - genRanParam()
         *            - whether generate() according to whether 'columns' have already 'Primary Key'
         *
         */
        ParameterType.ConcreteType primaryColumnsType = new ParameterType.NotEmpty(
                new CassandraTypes.PartitionSubsetType<>(columnsType,
                        (s, c) -> (Collection<Parameter>) c.params.get(2)
                                .getValue(),
                        null));

        Parameter primaryColumns = primaryColumnsType
                .generateRandomParameter(state, this);
        params.add(primaryColumns); // [3]

        ParameterType.ConcreteType IF_NOT_EXISTType = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("IF NOT EXISTS"), null // TODO: Make
        // a pure
        // CONSTANTType
        );
        Parameter IF_NOT_EXIST = IF_NOT_EXISTType
                .generateRandomParameter(state, this);
        params.add(IF_NOT_EXIST); // [4]

        Parameter speculative_retry = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities.strings2Parameters(
                        speculative_retryOptions),
                null).generateRandomParameter(null, null);

        params.add(speculative_retry); // [5]

    }

    @Override
    public String constructCommandString() {
        // TODO: Need a helper function, add space between all strings
        Parameter keyspaceName = params.get(0);
        Parameter tableName = params.get(1);
        Parameter columns = params.get(2); // LIST<PAIR<TEXTType,TYPE>>
        Parameter primaryColumns = params.get(3);
        Parameter IF_NOT_EXIST = params.get(4);
        Parameter speculative_retry = params.get(5);

        ParameterType.ConcreteType primaryColumnsNameType = new ParameterType.StreamMapType(
                null, (s, c) -> (Collection) c.params.get(3).getValue(),
                p -> ((Pair<Parameter, Parameter>) ((Parameter) p).value).left);
        Parameter primaryColumnsName = primaryColumnsNameType
                .generateRandomParameter(null, this);

        if (Config.getConf().CASSANDRA_ENABLE_SPECULATIVE_RETRY) {
            return "CREATE TABLE " + IF_NOT_EXIST.toString() + " "
                    + keyspaceName.toString() + "." + tableName.toString()
                    + " (" + columns.toString() + ", PRIMARY KEY ("
                    + primaryColumnsName.toString() + " )" + ")" +
                    " WITH speculative_retry = '" + speculative_retry.toString()
                    + "';";
        } else {
            return "CREATE TABLE " + IF_NOT_EXIST.toString() + " "
                    + keyspaceName.toString() + "." + tableName.toString()
                    + " (" + columns.toString() + ", PRIMARY KEY ("
                    + primaryColumnsName.toString() + " )" + ");";
        }
    }

    @Override
    public void updateState(State state) {
        Parameter keyspaceName = params.get(0);
        Parameter tableName = params.get(1);
        Parameter columns = params.get(2); // LIST<PAIR<TEXTType,TYPE>>
        Parameter primaryColumns = params.get(3);

        CassandraTable table = new CassandraTable(tableName, columns,
                primaryColumns);
        ((CassandraState) state).addTable(keyspaceName.toString(),
                tableName.toString(), table);
    }
}
