package org.zlab.upfuzz.hbase;

import java.util.AbstractMap;
import org.zlab.upfuzz.CommandPool;
import org.zlab.upfuzz.hbase.configurations.UPDATE_ALL_CONFIG;
import org.zlab.upfuzz.hbase.configurations.UPDATE_CONFIG;
import org.zlab.upfuzz.hbase.general.*;
import org.zlab.upfuzz.hbase.namespace.*;
import org.zlab.upfuzz.hbase.procedures.LIST_LOCKS;
import org.zlab.upfuzz.hbase.procedures.LIST_PROCEDURES;
import org.zlab.upfuzz.hbase.quotas.*;
// import org.zlab.upfuzz.hbase.rsgroup.*;
import org.zlab.upfuzz.hbase.snapshot.*;
import org.zlab.upfuzz.hbase.tools.*;
import org.zlab.upfuzz.hbase.ddl.*;
import org.zlab.upfuzz.hbase.dml.*;

public class HBaseCommandPool extends CommandPool {
    public static final int DML_WIGHT = 50;
    public static final int DDL_WIGHT = 50;

    @Override
    public void registerReadCommands() {
        // ddl
        // readCommandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(DESCRIBE.class,
        // DDL_WIGHT));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(EXISTS.class,
                        DDL_WIGHT));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(IS_DISABLED.class,
                        DDL_WIGHT));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(LIST.class, DDL_WIGHT));
        // readCommandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(LIST_REGIONS.class,
        // DDL_WIGHT));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(LOCATE_REGION.class,
                        DDL_WIGHT));
        // readCommandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(SHOW_FILTERS.class,
        // DDL_WIGHT));
        // dml
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(COUNT.class, DML_WIGHT));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(GET.class, DML_WIGHT));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(GET_COUNTER.class,
                        DML_WIGHT));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(GET_SPLITS.class,
                        DML_WIGHT));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(SCAN.class, DML_WIGHT));
        // general
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(STATUS.class, 5));
        // readCommandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(TABLE_HELP.class, 5));
        // readCommandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(VERSION.class, 5));
        // readCommandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(WHOAMI.class, 5));
        // namespace
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(DESCRIBE_NAMESPACE.class,
                        5));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(LIST_NAMESPACE.class,
                        5));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        LIST_NAMESPACE_TABLES.class, 5));
        // procedures
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(LIST_LOCKS.class, 5));
        // readCommandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(LIST_PROCEDURES.class,
        // 5));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        LIST_QUOTA_SNAPSHOTS.class, 5));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        LIST_QUOTA_TABLE_SIZES.class, 5));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(LIST_QUOTAS.class, 5));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        LIST_SNAPSHOT_SIZES.class, 5));
        // readCommandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(GET_TABLE_RSGROUP.class,
        // 5));
        // readCommandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(LIST_GROUPS.class, 5));
        // snapshot
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(LIST_SNAPSHOTS.class,
                        5));
        // tools
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(BALANCE_SWITCH_R.class,
                        5));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(BALANCER_ENABLED.class,
                        5));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        CATALOGJANITOR_ENABLED.class, 5));
        readCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        CLEANER_CHORE_ENABLED.class, 5));
    }

    @Override
    public void registerWriteCommands() {
        // configurations
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(UPDATE_ALL_CONFIG.class,
                        5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(UPDATE_CONFIG.class, 5));
        // ddl
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(ALTER_ADD_FAMILY.class,
                        DDL_WIGHT));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(ALTER_CF_OPTION.class,
                        DDL_WIGHT));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        ALTER_DELETE_FAMILY.class, DDL_WIGHT));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(ALTER_STATUS.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CLONE_TABLE_SCHEMA.class,
                        DDL_WIGHT));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CREATE.class,
                        DDL_WIGHT));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(DISABLE.class,
                        DDL_WIGHT));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(DROP.class, DDL_WIGHT));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(ENABLE.class,
                        DDL_WIGHT));
        // dml
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(APPEND.class,
                        DML_WIGHT));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(DELETE.class,
                        DML_WIGHT));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(DELETEALL.class,
                        DML_WIGHT));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(INCR_EXISTING.class,
                        DML_WIGHT));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(INCR_NEW.class,
                        DML_WIGHT));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(PUT_MODIFY.class,
                        DML_WIGHT));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(PUT_NEW.class,
                        DML_WIGHT));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(TRUNCATE.class,
                        DML_WIGHT));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(TRUNCATE_PRESERVE.class,
                        DML_WIGHT));
        // ns
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CREATE_NAMESPACE.class,
                        5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(DROP_NAMESPACE.class,
                        5));
        // quotas
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        DISABLE_EXCEED_THROTTLE_QUOTA.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        DISABLE_RPC_THROTTLE.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        ENABLE_EXCEED_THROTTLE_QUOTA.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        ENABLE_RPC_THROTTLE.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(SET_QUOTA_SPACE.class,
                        5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        SET_QUOTA_THROTTLE_REQUEST.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        SET_QUOTA_THROTTLE_RW.class, 5));
        // rsgroup
        // commandClassList.add(
        // new AbstractMap.SimpleImmutableEntry<>(ADD_RSGROUP.class, 5));
        // snaphost
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CLONE_SNAPSHOT.class,
                        5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(DELETE_SNAPSHOT.class,
                        5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(RESTORE_SNAPSHOT.class,
                        5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(SNAPSHOT.class, 5));
        // tools
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(BALANCE_SWITCH_W.class,
                        5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(BALANCER.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CATALOGJANITOR_RUN.class,
                        5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        CATALOGJANITOR_SWITCH.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CLEANER_CHORE_RUN.class,
                        5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(
                        CLEANER_CHORE_SWITCH.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CLEAR_BLOCK_CACHE.class,
                        5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CLEAR_DEADSERVERS.class,
                        5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(COMPACT.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(COMPACT_RS.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(COMPACTION_STATE.class,
                        5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(COMPACTION_SWITCH.class,
                        5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(FLUSH.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(MAJOR_COMPACT.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(SPLIT.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(WAL_ROLL.class, 5));
        commandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(ZK_DUMP.class, 5));
    }

    @Override
    public void registerCreateCommands() {
        createCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CREATE.class, 5));
        createCommandClassList.add(
                new AbstractMap.SimpleImmutableEntry<>(CREATE_NAMESPACE.class,
                        5));
    }

}
