package org.zlab.upfuzz.cassandra;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.cassandra.CassandraCqlshDaemon.CqlshPacket;
import org.zlab.upfuzz.CustomExceptions;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.utils.Pair;
import org.zlab.upfuzz.utils.Utilities;

public class CassandraExecutor extends Executor {

    CassandraCqlshDaemon cqlsh = null;

    Process cassandraProcess;
    //    static final String jacocoOptions = "=append=false,includes=org.apache.cassandra.*,output=dfe,address=localhost,sessionid=";

    // Over the JVM start up option limitation.

    static final String jacocoOptions = "=append=false";

    static final String classToIns = "org.apache.cassandra.*";
    // static final String classToIns = "org.apache.cassandra.io.compress.CompressionMetadata:org.apache.cassandra.db.commitlog.CommitLog:org.apache.cassandra.db.IndexExpression:org.apache.cassandra.config.Schema:org.apache.cassandra.io.util.MmappedSegmentedFile:org.apache.cassandra.utils.vint.EncodedDataInputStream:org.apache.cassandra.service.PendingRangeCalculatorService:org.apache.cassandra.db.RangeTombstone$Serializer:org.apache.cassandra.schema.LegacySchemaTables:org.apache.cassandra.concurrent.DebuggableThreadPoolExecutor:org.apache.cassandra.db.HintedHandOffManager:org.apache.cassandra.db.compaction.Scrubber:org.apache.cassandra.utils.BloomFilter:org.apache.cassandra.db.ColumnFamily:org.apache.cassandra.io.util.MmappedSegmentedFile$Builder:org.apache.cassandra.db.TruncationSerializer:org.apache.cassandra.utils.FBUtilities:org.apache.cassandra.concurrent.DebuggableThreadPoolExecutor$LocalSessionWrapper:org.apache.cassandra.auth.CassandraRoleManager:org.apache.cassandra.io.util.SegmentedFile$Builder:org.apache.cassandra.net.OutboundTcpConnectionPool:org.apache.cassandra.db.ColumnIndex$Builder:org.apache.cassandra.service.StorageProxy:org.apache.cassandra.transport.Client:org.apache.cassandra.metrics.ThreadPoolMetrics:org.apache.cassandra.utils.IntervalTree$IntervalNode:org.apache.cassandra.db.commitlog.CommitLogSegmentManager:org.apache.cassandra.concurrent.DebuggableScheduledThreadPoolExecutor:org.apache.cassandra.gms.HeartBeatStateSerializer:org.apache.cassandra.io.sstable.format.SSTableReader$GlobalTidy:org.apache.cassandra.io.util.AbstractDataInput:org.apache.cassandra.db.ColumnSerializer:org.apache.cassandra.db.compaction.CompactionManager:org.apache.cassandra.metrics.ThreadPoolMetricNameFactory:org.apache.cassandra.io.sstable.IndexSummaryManager:org.apache.cassandra.io.util.UnbufferedDataOutputStreamPlus:org.apache.cassandra.concurrent.StageManager$1:org.apache.cassandra.serializers.SetSerializer:org.apache.cassandra.utils.memory.MemtablePool$SubPool:org.apache.cassandra.db.SystemKeyspace:org.apache.cassandra.io.sstable.SnapshotDeletingTask:org.apache.cassandra.db.commitlog.CommitLogSegment:org.apache.cassandra.db.SuperColumns$SCIterator:org.apache.cassandra.db.marshal.ListType:org.apache.cassandra.serializers.MapSerializer:org.apache.cassandra.utils.concurrent.Ref$Debug:org.apache.cassandra.db.marshal.AbstractCompositeType:org.apache.cassandra.db.MutationVerbHandler:org.apache.cassandra.db.index.SecondaryIndex:org.apache.cassandra.utils.concurrent.WaitQueue$AbstractSignal:org.apache.cassandra.service.CassandraDaemon$2:org.apache.cassandra.io.sstable.format.big.BigTableReader:org.apache.cassandra.triggers.TriggerExecutor:org.apache.cassandra.io.sstable.SSTableIdentityIterator:org.apache.cassandra.net.MessagingService:org.apache.cassandra.io.sstable.SSTableRewriter:org.apache.cassandra.db.commitlog.CommitLogReplayer:org.apache.cassandra.io.sstable.format.big.BigTableWriter:org.apache.cassandra.io.sstable.SSTableDeletingTask:org.apache.cassandra.utils.ByteBufferUtil:org.apache.cassandra.service.StorageService:org.apache.cassandra.db.BlacklistedDirectories:org.apache.cassandra.utils.concurrent.WaitQueue$RegisteredSignal:org.apache.cassandra.io.util.NIODataInputStream:org.apache.cassandra.utils.BackgroundActivityMonitor:org.apache.cassandra.service.CassandraDaemon:org.apache.cassandra.db.compaction.CompactionTask:org.apache.cassandra.utils.GuidGenerator:org.apache.cassandra.utils.ResourceWatcher:org.apache.cassandra.io.util.RandomAccessReader:org.apache.cassandra.db.compaction.LeveledCompactionTask:org.apache.cassandra.db.BatchlogManager$EndpointFilter:org.apache.cassandra.io.sstable.IndexSummaryBuilder:org.apache.cassandra.db.compaction.Verifier:org.apache.cassandra.concurrent.JMXEnabledThreadPoolExecutor:org.apache.cassandra.io.util.BufferedDataOutputStreamPlus:org.apache.cassandra.db.ColumnFamilyStore$Flush:org.apache.cassandra.service.AbstractReadExecutor:org.apache.cassandra.io.util.ChecksummedSequentialWriter$TransactionalProxy:org.apache.cassandra.concurrent.SEPWorker:org.apache.cassandra.gms.Gossiper:org.apache.cassandra.io.sstable.format.SSTableReader:org.apache.cassandra.io.sstable.metadata.MetadataSerializer:org.apache.cassandra.db.RangeTombstone$Tracker:org.apache.cassandra.locator.GoogleCloudSnitch:org.apache.cassandra.io.util.DataOutputStreamPlus:org.apache.cassandra.db.marshal.TypeParser:org.apache.cassandra.cql3.statements.CQL3CasRequest:org.apache.cassandra.dht.Murmur3Partitioner:org.apache.cassandra.tools.SSTableRepairedAtSetter:org.apache.cassandra.config.DatabaseDescriptor:org.apache.cassandra.thrift.ThriftServer$ThriftServerThread:org.apache.cassandra.service.ReadCallback:org.apache.cassandra.db.marshal.CompositeType:org.apache.cassandra.service.MigrationManager:org.apache.cassandra.locator.CloudstackSnitch:org.apache.cassandra.config.CFMetaData:org.apache.cassandra.db.composites.AbstractComposite:org.apache.cassandra.repair.Validator:org.apache.cassandra.serializers.CollectionSerializer:org.apache.cassandra.service.pager.SliceQueryPager:org.apache.cassandra.db.commitlog.CommitLogDescriptor:org.apache.cassandra.serializers.TimestampSerializer:org.apache.cassandra.db.ColumnFamilyStore:org.apache.cassandra.db.compaction.Upgrader:org.apache.cassandra.db.compaction.writers.CompactionAwareWriter:org.apache.cassandra.service.LoadBroadcaster:org.apache.cassandra.db.Directories$SSTableLister:org.apache.cassandra.db.Directories:org.apache.cassandra.utils.memory.MemtableCleanerThread:org.apache.cassandra.db.compaction.writers.MaxSSTableSizeWriter:org.apache.cassandra.net.OutboundTcpConnection:org.apache.cassandra.cache.AutoSavingCache$Writer:org.apache.cassandra.locator.Ec2Snitch:org.apache.cassandra.metrics.SEPMetrics:org.apache.cassandra.service.ClientWarn:org.apache.cassandra.io.sstable.SSTable:org.apache.cassandra.net.IncomingTcpConnection:org.apache.cassandra.metrics.ThreadPoolMetrics$2:org.apache.cassandra.utils.concurrent.Transactional$AbstractTransactional:org.apache.cassandra.utils.concurrent.Ref$1:org.apache.cassandra.concurrent.StageManager$ExecuteOnlyExecutor:org.apache.cassandra.locator.TokenMetadata:org.apache.cassandra.io.util.DataIntegrityMetadata$ChecksumWriter:org.apache.cassandra.db.BatchlogManager:org.apache.cassandra.utils.HeapUtils:org.apache.cassandra.utils.vint.EncodedDataOutputStream:org.apache.cassandra.utils.StatusLogger:org.apache.cassandra.utils.concurrent.WaitQueue:org.apache.cassandra.tools.SSTableExport:org.apache.cassandra.db.context.CounterContext:org.apache.cassandra.io.compress.CompressedRandomAccessReader:org.apache.cassandra.db.RowIndexEntry$Serializer:org.apache.cassandra.tracing.Tracing:org.apache.cassandra.cache.AutoSavingCache:org.apache.cassandra.db.context.CounterContext$ContextState:org.apache.cassandra.db.SuperColumns:org.apache.cassandra.tracing.TraceState:org.apache.cassandra.io.compress.CompressionMetadata$Writer:org.apache.cassandra.db.marshal.MapType:org.apache.cassandra.streaming.StreamLockfile:org.apache.cassandra.utils.ExpiringMap:org.apache.cassandra.db.WindowsFailedSnapshotTracker:org.apache.cassandra.concurrent.StageManager:org.apache.cassandra.thrift.TServerCustomFactory:org.apache.cassandra.net.MessagingService$SocketThread:org.apache.cassandra.db.BatchlogManager$Batch:org.apache.cassandra.utils.memory.MemtablePool:org.apache.cassandra.db.marshal.UserType";

    public CassandraExecutor(CommandSequence commandSequence,
            CommandSequence validationCommandSequence) {
        super(commandSequence, validationCommandSequence, "cassandra");
    }

    public CassandraExecutor() {
        super("cassandra");
    }

    public boolean isCassandraReady(String oldSystemPath) {
        //    ProcessBuilder isReadyBuilder = new ProcessBuilder();
        Process isReady;
        int ret = 0;
        try {
            isReady = Utilities.exec(
                    new String[] { "bin/cqlsh", "-e", "describe cluster" },
                    oldSystemPath);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(isReady.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                // System.out.println(line);
            }
            isReady.waitFor();
            in.close();
            ret = isReady.exitValue();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return ret == 0;
    }

    @Override
    public void startup() {

        // May change classToIns according to the system...
        System.out.println("[Old Version] Cassandra Start...");

        ProcessBuilder cassandraProcessBuilder = new ProcessBuilder(
                "bin/cassandra", "-f");
        Map<String, String> env = cassandraProcessBuilder.environment();
        env.put("JAVA_TOOL_OPTIONS",
                "-javaagent:" + Config.getConf().jacocoAgentPath + jacocoOptions
                        + ",includes=" + classToIns
                        + ",output=dfe,address=localhost,sessionid=" + systemID
                        + "-" + executorID + "_original");
        cassandraProcessBuilder
                .directory(new File(Config.getConf().oldSystemPath));
        cassandraProcessBuilder.redirectErrorStream(true);
        cassandraProcessBuilder.redirectOutput(
                Paths.get(Config.getConf().oldSystemPath, "logs.txt").toFile());
        try {
            long startTime = System.currentTimeMillis();
            cassandraProcess = cassandraProcessBuilder.start();
            // byte[] out = cassandraProcess.getInputStream().readAllBytes();
            // BufferedReader in = new BufferedReader(new InputStreamReader(cassandraProcess.getInputStream()));
            // String line;
            // while ((line = in.readLine()) != null) {
            //     System.out.println(line);
            //     System.out.flush();
            // }
            // in.close();
            // cassandraProcess.waitFor();
            System.out.println("cassandra " + executorID + " started");
            while (!isCassandraReady(Config.getConf().oldSystemPath)) {
                if (!cassandraProcess.isAlive()) {
                    // System.out.println("cassandra process crushed\nCheck " + Config.getConf().cassandraOutputFile
                    //         + " for details");
                    // System.exit(1);
                    throw new CustomExceptions.systemStartFailureException(
                            "Cassandra Start fails", null);
                }
                System.out.println("Wait for " + systemID + " ready...");
                Thread.sleep(2000);
            }
            long endTime = System.currentTimeMillis();
            System.out
                    .println("cassandra " + executorID + " ready \n time usage:"
                            + (endTime - startTime) / 1000. + "\n");

            cqlsh = new CassandraCqlshDaemon(Config.getConf().oldSystemPath);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void teardown() {
        ProcessBuilder pb = new ProcessBuilder("bin/nodetool", "stopdaemon");
        pb.directory(new File(Config.getConf().oldSystemPath));
        Process p;
        try {
            p = pb.start();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
                System.out.flush();
            }
            p.waitFor();
            in.close();
            assert !cassandraProcess.isAlive();
            System.out.println(
                    "cassandra " + executorID + " shutdown successfully");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        // Remove the data folder
        pb = new ProcessBuilder("rm", "-rf", "data", "logs.txt");
        pb.directory(new File(Config.getConf().oldSystemPath));
        try {
            pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.cqlsh.destroy();
    }

    public void upgradeteardown() {
        ProcessBuilder pb = new ProcessBuilder("bin/nodetool", "stopdaemon");
        pb.directory(new File(Config.getConf().newSystemPath));
        Process p;
        try {
            p = pb.start();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                // System.out.println(line);
                // System.out.flush();
            }
            p.waitFor();
            in.close();
            System.out.println(
                    "new cassandra " + executorID + " shutdown successfully");

            // p.wait();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        // Remove the data folder
        pb = new ProcessBuilder("rm", "-rf", "data", "logs.txt");
        pb.directory(new File(Config.getConf().newSystemPath));
        try {
            pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Upgrade folder has been removed");

        // Stop all running cassandra instances
        // pgrep -u vagrant -f cassandra | xargs kill -9
        pb = new ProcessBuilder("pgrep", "-u", "vagrant", "cassandra",
                "| xargs kill -9");
        pb.directory(new File(Config.getConf().newSystemPath));
        Utilities.runProcess(pb, "kill cassandra instances");

        this.cqlsh.destroy();

    }

    public Pair<CommandSequence, CommandSequence> prepareCommandSequence() {
        CommandSequence commandSequence = null;
        CommandSequence validationCommandSequence = null;
        try {
            commandSequence = CommandSequence.generateSequence(
                    CassandraCommands.commandClassList,
                    CassandraCommands.createCommandClassList,
                    CassandraState.class, null);
            // TODO: If it's generating read with a initial state, no need to generate with createTable...
            validationCommandSequence = CommandSequence.generateSequence(
                    CassandraCommands.readCommandClassList, null,
                    CassandraState.class, commandSequence.state);
        } catch (NoSuchMethodException | InvocationTargetException
                | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return new Pair<>(commandSequence, validationCommandSequence);
    }

    public static CommandSequence prepareValidationCommandSequence(
            State state) {
        CommandSequence validationCommandSequence = null;
        try {
            validationCommandSequence = CommandSequence.generateSequence(
                    CassandraCommands.readCommandClassList, null,
                    CassandraState.class, state);
        } catch (NoSuchMethodException | InvocationTargetException
                | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return validationCommandSequence;
    }

    @Override
    public List<String> executeCommands(CommandSequence commandSequence) {
        //        commandSequence = prepareCommandSequence();
        List<String> commandList = commandSequence.getCommandStringList();
        List<String> ret = new LinkedList<>();
        try {
            if (cqlsh == null)
                cqlsh = new CassandraCqlshDaemon(
                        Config.getConf().oldSystemPath);
            for (String cmd : commandList) {
                // System.out
                //         .println("\n\n------------------------------------------------------------\nexecutor command:\n"
                //                 + cmd + "\n------------------------------------------------------------\n");
                long startTime = System.currentTimeMillis();
                CqlshPacket cp = cqlsh.execute(cmd);
                long endTime = System.currentTimeMillis();

                ret.add(cp.message);
                // System.out.println("ret is: " + cp.exitValue + "\ntime: " + cp.timeUsage + "\ntime usage(network):"
                //         + (endTime - startTime) / 1000. + "\n");
            }
            //            cqlsh.destroy();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        return ret;
    }

    public List<String> newVersionExecuteCommands(
            CommandSequence commandSequence) {
        //        commandSequence = prepareCommandSequence();
        List<String> commandList = commandSequence.getCommandStringList();
        List<String> ret = new LinkedList<>();
        try {
            // TODO: Put the cqlsh daemon outside, so that one instance for one cqlsh daemon

            if (cqlsh == null)
                cqlsh = new CassandraCqlshDaemon(
                        Config.getConf().newSystemPath);
            for (String cmd : commandList) {
                // System.out
                //         .println("\n\n------------------------------------------------------------\nexecutor command:\n"
                //                 + cmd + "\n------------------------------------------------------------\n");
                long startTime = System.currentTimeMillis();
                CqlshPacket cp = cqlsh.execute(cmd);
                long endTime = System.currentTimeMillis();

                ret.add(cp.message);
                // System.out.println("ret is: " + cp.exitValue + "\ntime: " + cp.timeUsage + "\ntime usage(network):"
                //         + (endTime - startTime) / 1000. + "\n");
            }
            //            cqlsh.destroy();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        return ret;
    }

    @Override
    public int saveSnapshot() {
        // Flush
        ProcessBuilder pb = new ProcessBuilder("bin/nodetool", "flush");
        pb.directory(new File(Config.getConf().oldSystemPath));
        Utilities.runProcess(pb, "[Executor] Old Version System Flush");
        return 0;
    }

    @Override
    public int moveSnapShot() {
        // Copy the data dir
        Path oldFolderPath = Paths.get(Config.getConf().oldSystemPath, "data");
        Path newFolderPath = Paths.get(Config.getConf().newSystemPath);

        ProcessBuilder pb = new ProcessBuilder("cp", "-r",
                oldFolderPath.toString(), newFolderPath.toString());
        pb.directory(new File(Config.getConf().oldSystemPath));
        Utilities.runProcess(pb,
                "[Executor] Copy the data folder to the new version");
        return 0;
    }

    /**
     * 1. Move the data folder to the new version Cassandra
     * 2. Start the new version cassandra with the Upgrade symbol
     * 3. Check whether there is any exception happen during the
     * 4. Run some commands, check consistency
     *
     * upgrade process...
     * Also need to control the java version.
     */
    @Override
    public boolean upgradeTest() {
        /**
         * Data consistency check
         * If the return size is different, exception when executing the commands, or the results are different.
         * Record both results, report as a potential bug.
         * 
         * A crash seed should contain:
         * 1. Two command sequences.
         * 2. The results on old and new version.
         * 3. The reason why it's different
         *      - Upgrade process throw an exception
         *      - The result of a specific command is different
         */
        // If there is any exception when executing the commands, it should also be caught

        // Upgrade Startup
        ProcessBuilder pb = new ProcessBuilder("bin/cassandra");
        // Map<String, String> env = pb.environment();
        // env.put("JAVA_TOOL_OPTIONS",
        //         "-javaagent:" + Config.getConf().jacocoAgentPath + jacocoOptions
        //                 + ",includes=" + classToIns
        //                 + ",output=dfe,address=localhost,sessionid=" + systemID
        //                 + "-" + executorID + "_upgraded");

        pb.directory(new File(Config.getConf().newSystemPath));
        pb.redirectOutput(
                Paths.get(Config.getConf().newSystemPath, "logs.txt").toFile());
        // long startTime = System.currentTimeMillis();
        Utilities.runProcess(pb, "Upgrade Cassandra");
        // Process upgradeCassandraProcess = Utilities.runProcess(pb, "Upgrade Cassandra");

        // Add a retry time here
        boolean started = false;
        int RETRY_START_UPGRADE = 25;
        for (int i = 0; i < RETRY_START_UPGRADE; i++) {
            if (isCassandraReady(Config.getConf().newSystemPath)) {
                started = true;
                break;
            }
            try {
                System.out.println("Upgrade System Waiting...");
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        FailureType failureType = null;
        String failureInfo = null;

        if (!started) {
            // Retry the upgrade, clear the folder, kill the hang process
            System.out.println("[FAILURE LOG] New version cannot start");
            failureType = FailureType.UPGRADE_FAIL;
            failureInfo = "New version cassandra cannot start\n";

            testId2Failure.put(-1, new Pair<>(failureType, failureInfo));
            return false;
        }

        // long endTime = System.currentTimeMillis();
        // System.out.println("Upgrade System Start Time = " + (endTime - startTime)/1000.  + "s");
        try {
            this.cqlsh = new CassandraCqlshDaemon(
                    Config.getConf().newSystemPath);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // while (!isNewCassandraReady(Config.getConf().newSystemPath)) {
        //     // Problem : why would the process be dead?
        //     // TODO: Enable this checking, add a retry times
        //     if (!upgradeCassandraProcess.isAlive()) {
        //         // Throw a specific exception, if this is upgrade, it means we met a bug
        //         System.out.println("[FAILURE LOG] New version cannot start");
        //         failureType = FailureType.UPGRADE_FAIL;
        //         failureInfo = "New version cassandra cannot start";

        //         return false;
        //     }
        //     try {
        //         System.out.println("Upgrade System Waiting...");
        //         Thread.sleep(1000);
        //     } catch (InterruptedException e) {
        //         e.printStackTrace();
        //     }
        // }

        // startTime = System.currentTimeMillis();
        for (Integer testId : testId2commandSequence.keySet()) {
            testId2newVersionResult.put(testId, newVersionExecuteCommands(
                    testId2commandSequence.get(testId).right));
        }

        // Iterate all results, find out the one with difference
        for (Integer testId : testId2commandSequence.keySet()) {
            // System.out.println("\n\t testId = " + testId);
            boolean ret = true;
            failureType = null;
            failureInfo = null;

            List<String> oldVersionResult = testId2oldVersionResult.get(testId);
            List<String> newVersionResult = testId2newVersionResult.get(testId);

            // System.out.println("old version size = " + oldVersionResult.size() + " new version size = " + newVersionResult.size());

            // System.out.println("new version result:");
            // for (String str: newVersionResult) {
            //     System.out.println(str);
            // }
            System.out.println("new size = " + newVersionResult.size());
            if (newVersionResult.size() != oldVersionResult.size()) {
                failureType = FailureType.RESULT_INCONSISTENCY;
                failureInfo = "The result size is different, old version result size = "
                        + oldVersionResult.size()
                        + "  while new version result size"
                        + newVersionResult.size();
                ret = false;
            } else {
                for (int i = 0; i < newVersionResult.size(); i++) {

                    if (oldVersionResult.get(i)
                            .compareTo(newVersionResult.get(i)) != 0) {

                        // SyntaxException
                        if (oldVersionResult.get(i).contains("SyntaxException")
                                && newVersionResult.get(i)
                                        .contains("SyntaxException")) {
                            continue;
                        }

                        // InvalidRequest
                        if (oldVersionResult.get(i).contains("InvalidRequest")
                                && newVersionResult.get(i)
                                        .contains("InvalidRequest")) {
                            continue;
                        }

                        if (oldVersionResult.get(i).contains("0 rows")
                                && newVersionResult.get(i).contains("0 rows")) {
                            continue;
                        }

                        // System.out.println("old version result: " + oldVersionResult.get(i));
                        // System.out.println("new version result: " + newVersionResult.get(i));

                        failureType = FailureType.RESULT_INCONSISTENCY;

                        String errorMsg = "Result not the same at read sequence id = "
                                + i + "\n" + "Old Version Result: "
                                + oldVersionResult.get(i) + "  "
                                + "New Version Result: "
                                + newVersionResult.get(i) + "\n";

                        if (failureInfo == null) {
                            failureInfo = errorMsg;
                        } else {
                            failureInfo += errorMsg;
                        }
                        ret = false;
                        //                        break; // Try to log all the difference for this instance
                    }
                }
            }
            if (!ret) {
                testId2Failure.put(testId,
                        new Pair<>(failureType, failureInfo));
            }
        }

        // endTime = System.currentTimeMillis();
        // System.out.println("Upgrade System comparing results = " + (endTime - startTime)/1000.  + "s");
        // Shutdown
        // startTime = System.currentTimeMillis();
        upgradeteardown();
        // endTime = System.currentTimeMillis();
        // System.out.println("New version Stop = " + (endTime - startTime)/1000.  + "s");

        // true means upgrade test succeeded, false means an inconsistency exists
        return testId2Failure.isEmpty();

    }

    @Override
    public void upgrade() throws Exception {
        // TODO Auto-generated method stub
    }

}
