package org.zlab.upfuzz.fuzzingengine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Queue;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.zlab.upfuzz.fuzzingengine.Config.Configuration;
import org.jacoco.core.data.ExecutionDataStore;
import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.cassandra.CassandraExecutor;
import org.zlab.upfuzz.fuzzingengine.FuzzingClient;
import org.zlab.upfuzz.fuzzingengine.FuzzingClient.FuzzingClientActions;
import org.zlab.upfuzz.fuzzingengine.FuzzingServer.FuzzingServerActions;
import org.zlab.upfuzz.utils.Utilities;

public class Main {

    public static void main(String[] args) throws ParseException {
        long currentTime = System.currentTimeMillis();
        long vmStartTime = ManagementFactory.getRuntimeMXBean().getStartTime();
        System.out.println("jvm startup time million seconds: " + (currentTime - vmStartTime));
        final Options options = new Options();
        Option clazzOption = Option.builder("class").argName("type").hasArg().desc("start a dfe server or client or fuzzer")
                .required().build();
        // Option actionOption = Option.builder("action").argName("action").hasArg().desc("start a dfe server or client")
        //         .required().build();
        Option configFileOption = Option.builder("config").argName("config").hasArg().desc("Configuration file location").build();
        options.addOption(clazzOption);
        // options.addOption(actionOption);
        options.addOption(configFileOption);
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if( cmd.hasOption(configFileOption) ){
            try {
                File configFile = new File(cmd.getOptionValue(configFileOption));
                Configuration cfg = new Gson().fromJson(new FileReader(configFile), Configuration.class);
                Config.setInstance(cfg);
            } catch (JsonSyntaxException | JsonIOException | FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        String type = cmd.getOptionValue(clazzOption);
        if (type.toLowerCase().equals("server")) {
            assert false;
//            String act = cmd.getOptionValue(actionOption);
//            FuzzingServerActions action = FuzzingServerActions.valueOf(act);
//            switch (action) {
//            case start: {
//                new FuzzingClient(conf).start();
//                break;
//            }
//            default:
//                throw new UnsupportedOperationException(act);
//            }
        } else if (type.toLowerCase().equals("client")) {
            assert false;
//            String act = cmd.getOptionValue(actionOption);
//            FuzzingClientActions action = FuzzingClientActions.valueOf(act);
//            switch (action) {
//            case start:{
//                new FuzzingClient(conf).start();
//                break;
//            }
//            case collect: {
//                // new FuzzingClient(conf).collect();
//                break;
//            }
//            default:
//                throw new UnsupportedOperationException(act);
//            }
        } else if (type.toLowerCase().equals("fuzzer")) {
            /**
             * We could also only save path. Queue<Path>, then when
             * need a command sequence, deserialize it then.
             * But now try with the most simple one.
             */

            // Start up, load all command sequence into a queue.
            Queue<CommandSequence> queue = new LinkedList<>();
            System.out.println("seed path = " + Config.getConf().initSeedDir);
            Path initSeedDirPath = Paths.get(Config.getConf().initSeedDir);
            File initSeedDir = initSeedDirPath.toFile();
            assert initSeedDir.isDirectory() == true;
            for (File seedFile : initSeedDir.listFiles()) {
                if (!seedFile.isDirectory()) {
                    // Deserialize current file, and add it into the queue.
                    CommandSequence commandSequence = Utilities.deserializeCommandSequence(seedFile.toPath());
                    if (commandSequence != null)
                        queue.add(commandSequence);
                }
            }

            ExecutionDataStore curCoverage = new ExecutionDataStore();
            FuzzingClient fuzzingClient = new FuzzingClient();

            // Start fuzzing process
            while(true) {
                System.out.println("[HKLOG] QUEUE SIZE = " + queue.size());
                if (queue.isEmpty()) {
                    CommandSequence commandSequence = CassandraExecutor.prepareCommandSequence();
                    Fuzzer.fuzzOne(commandSequence, curCoverage, queue, fuzzingClient, false);
                } else {
                    CommandSequence testCommandSequence = queue.poll();
                    Fuzzer.fuzzOne(testCommandSequence, curCoverage, queue, fuzzingClient, true);
                }
                break;
            }
            System.out.println("\n Fuzzing process end, have a good day \n");
        }
    }
}
