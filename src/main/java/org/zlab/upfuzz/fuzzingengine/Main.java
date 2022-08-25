package org.zlab.upfuzz.fuzzingengine;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zlab.upfuzz.fuzzingengine.Config.Configuration;
import org.zlab.upfuzz.fuzzingengine.Server.FuzzingServer;

public class Main {

    static Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args)
            throws ParseException, InterruptedException {
        final Options options = new Options();
        Option clazzOption = Option.builder("class")
                .argName("type")
                .hasArg()
                .desc("start a dfe server or client or fuzzer")
                .required()
                .build();
        // Option actionOption =
        // Option.builder("action").argName("action").hasArg().desc("start a dfe
        // server or client")
        // .required().build();
        Option configFileOption = Option.builder("config")
                .argName("config")
                .hasArg()
                .desc("Configuration file location")
                .build();
        options.addOption(clazzOption);
        // options.addOption(actionOption);
        options.addOption(configFileOption);
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(configFileOption)) {
            try {
                File configFile = new File(
                        cmd.getOptionValue(configFileOption));
                Configuration cfg = new Gson().fromJson(
                        new FileReader(configFile), Configuration.class);
                Config.setInstance(cfg);
            } catch (JsonSyntaxException | JsonIOException
                    | FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        Config.getConf().checkNull();

        if (Config.getConf().corpusDir == null) {
            // create a corpus dir in the folder
            Path corpusPath = Paths.get(System.getProperty("user.dir"))
                    .resolve("corpus");
            corpusPath.toFile().mkdir();
            Config.instance.corpusDir = corpusPath.toString();
        }

        if (Config.getConf().crashDir == null) {
            // create a corpus dir in the folder
            Path crashPath = Paths.get(System.getProperty("user.dir"))
                    .resolve("crash");
            crashPath.toFile().mkdir();
            Config.instance.crashDir = crashPath.toString();
        }

        String type = cmd.getOptionValue(clazzOption);
        logger.info("start " + type);
        if (type.toLowerCase().equals("server")) {
            new FuzzingServer().start();
            // String act = cmd.getOptionValue(actionOption);
            // FuzzingServerActions action = FuzzingServerActions.valueOf(act);
            // switch (action) {
            // case start: {
            // new FuzzingClient(conf).start();
            // break;
            // }
            // default:
            // throw new UnsupportedOperationException(act);
            // }
        } else if (type.toLowerCase().equals("client")) {
            new FuzzingClient().start();
        } else if (type.toLowerCase().equals("fuzzer")) {
            logger.error("equal fuzzer");
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        Thread.sleep(200);
                        logger.info("Fuzzing process end, have a good day ...");
                        // some cleaning up code...
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        e.printStackTrace();
                    }
                }
            });

            // Start fuzzing process
            Fuzzer fuzzer = new Fuzzer();
            // fuzzer.start();
        }
    }
}
