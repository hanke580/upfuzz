package org.zlab.upfuzz.cassandra.cqlcommands;

import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCommands;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.cassandra.CassandraTable;
import org.zlab.upfuzz.cassandra.CassandraTypes;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.PAIRType;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.STRINGType;

import java.util.Collection;

public class CREATE_TABLE extends CassandraCommands {
    /**
     * a parameter should correspond to one variable in the text format of this command.
     * mutating a parameter could depend on and the state updated by all nested internal commands and other parameters.
     * Note: Thus, we need to be careful to not have cyclic dependency among parameters.
     */

    // final Command ...; // Nested commands need to be constructed first.

    public CREATE_TABLE(State state, Object init0, Object init1,
            Object init2, Object init3, Object init4) {
        super();

        assert state instanceof CassandraState;
        CassandraState cassandraState = (CassandraState) state;

        Parameter keyspaceName = chooseKeyspace(state, this, init0);
        params.add(keyspaceName); // [0]

        ParameterType.ConcreteType tableNameType = new ParameterType.NotInCollectionType(
                new ParameterType.NotEmpty(STRINGType.instance),
                (s, c) -> ((CassandraState) s).keyspace2tables
                        .get(this.params.get(0).toString()).keySet(),
                null);

        Parameter tableName = tableNameType
                .generateRandomParameter(cassandraState, this, init1);
        params.add(tableName); // [1]

        ParameterType.ConcreteType columnsType = // LIST<PAIR<String,TYPEType>>
                new ParameterType.NotEmpty(ParameterType.ConcreteGenericType
                        .constructConcreteGenericType(
                                CassandraTypes.MapLikeListType.instance,
                                ParameterType.ConcreteGenericType
                                        .constructConcreteGenericType(
                                                PAIRType.instance,
                                                new ParameterType.NotEmpty(
                                                        STRINGType.instance),
                                                CassandraTypes.TYPEType.instance)));

        Parameter columns = columnsType
                .generateRandomParameter(cassandraState, this, init2);
        params.add(columns); // [2]

        ParameterType.ConcreteType primaryColumnsType = new ParameterType.NotEmpty(
                new ParameterType.SubsetType(columnsType,
                        (s, c) -> (Collection<Parameter>) c.params.get(2)
                                .getValue(),
                        null));

        Parameter primaryColumns = primaryColumnsType
                .generateRandomParameter(cassandraState, this, init3);
        params.add(primaryColumns); // [3]

        ParameterType.ConcreteType IF_NOT_EXISTType = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("IF NOT EXISTS"), null // TODO: Make
        // a pure
        // CONSTANTType
        );
        Parameter IF_NOT_EXIST = IF_NOT_EXISTType
                .generateRandomParameter(cassandraState, this, init4);
        params.add(IF_NOT_EXIST); // [4]

        updateExecutableCommandString();
    }

    public CREATE_TABLE(State state) {
        super();

        assert state instanceof CassandraState;
        CassandraState cassandraState = (CassandraState) state;

        Parameter keyspaceName = chooseKeyspace(state, this, null);
        params.add(keyspaceName); // [0]

        ParameterType.ConcreteType tableNameType = new ParameterType.NotInCollectionType(
                new ParameterType.NotEmpty(STRINGType.instance),
                (s, c) -> ((CassandraState) s).keyspace2tables
                        .get(this.params.get(0).toString()).keySet(),
                null);

        Parameter tableName = tableNameType
                .generateRandomParameter(cassandraState, this);
        params.add(tableName); // [1]

        ParameterType.ConcreteType columnsType = // LIST<PAIR<String,TYPEType>>
                new ParameterType.NotEmpty(ParameterType.ConcreteGenericType
                        .constructConcreteGenericType(
                                CassandraTypes.MapLikeListType.instance,
                                ParameterType.ConcreteGenericType
                                        .constructConcreteGenericType(
                                                PAIRType.instance,
                                                new ParameterType.NotEmpty(
                                                        STRINGType.instance),
                                                CassandraTypes.TYPEType.instance)));

        Parameter columns = columnsType
                .generateRandomParameter(cassandraState, this);
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
                new ParameterType.SubsetType(columnsType,
                        (s, c) -> (Collection<Parameter>) c.params.get(2)
                                .getValue(),
                        null));

        Parameter primaryColumns = primaryColumnsType
                .generateRandomParameter(cassandraState, this);
        params.add(primaryColumns); // [3]

        ParameterType.ConcreteType IF_NOT_EXISTType = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("IF NOT EXISTS"), null // TODO: Make
        // a pure
        // CONSTANTType
        );
        Parameter IF_NOT_EXIST = IF_NOT_EXISTType
                .generateRandomParameter(cassandraState, this);
        params.add(IF_NOT_EXIST); // [4]

        updateExecutableCommandString();
    }

    @Override
    public String constructCommandString() {
        // TODO: Need a helper function, add space between all strings
        Parameter keyspaceName = params.get(0);
        Parameter tableName = params.get(1);
        Parameter columns = params.get(2); // LIST<PAIR<TEXTType,TYPE>>
        Parameter primaryColumns = params.get(3);
        Parameter IF_NOT_EXIST = params.get(4);

        ParameterType.ConcreteType primaryColumnsNameType = new ParameterType.StreamMapType(
                null, (s, c) -> (Collection) c.params.get(3).getValue(),
                p -> ((Pair<Parameter, Parameter>) ((Parameter) p).value).left);
        Parameter primaryColumnsName = primaryColumnsNameType
                .generateRandomParameter(null, this);

        String ret = "CREATE TABLE " + IF_NOT_EXIST.toString() + " "
                + keyspaceName.toString() + "." + tableName.toString()
                + " (" + columns.toString() + ",\n PRIMARY KEY ("
                + primaryColumnsName.toString() + " )" + ");";

        return ret;
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
