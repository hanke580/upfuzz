package org.zlab.upfuzz;

import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.cassandra.CassandraState;

public class ConfigurableTypeTests extends AbstractTest {
    @Test
    public void testNotInCollection() {
        CassandraState s = new CassandraState();
        /**
         * Add several existing tables.
         * Make the tableName generate like existing tables.
         * Check whether NotInCollectionType run in a correct way.
         *
         * Before testing, make the STRINGType can only choose
         * from Tx, x = 1,2,...5
         */
        // s.addTable(new CassandraTable(new Parameter(STRINGType.instance,
        // "T1"), null, null));
        // s.addTable(new CassandraTable(new Parameter(STRINGType.instance,
        // "T2"), null, null));
        // s.addTable(new CassandraTable(new Parameter(STRINGType.instance,
        // "T3"), null, null));
        //
        // CassandraCommand.CREATETABLE cmd = new
        // CassandraCommand.CREATETABLE(s);
        // System.out.println(cmd.constructCommandString());
    }
}
