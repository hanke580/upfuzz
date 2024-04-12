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
import org.zlab.upfuzz.fuzzingengine.server.FuzzingServer;

public class Main {

    static Logger logger = LogManager.getLogger(Main.class);

    public static Path upfuzzConfigFilePath = null;

    public static void main(String[] args)
            throws ParseException, InterruptedException {
        final Options options = new Options();
        Option clazzOption = Option.builder("class")
                .argName("type")
                .hasArg()
                .desc("start a dfe server or client or fuzzer")
                .required()
                .build();
        Option configFileOption = Option.builder("config")
                .argName("config")
                .hasArg()
                .desc("Configuration file location")
                .build();
        Option flagOption = Option.builder("flag")
                .argName("flag")
                .hasArg()
                .desc("Fuzzing Client type")
                .build();
        Option downgradeSupportOption = Option.builder("downgrade")
                .argName("downgrade")
                .hasArg()
                .desc("Is downgrade supported for this version pair")
                .build();
        options.addOption(clazzOption);
        options.addOption(configFileOption);
        options.addOption(flagOption);
        options.addOption(downgradeSupportOption);
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(configFileOption)) {
            try {
                upfuzzConfigFilePath = Paths
                        .get(cmd.getOptionValue(configFileOption));
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

        // construct failure folder
        if (Config.getConf().failureDir == null) {
            // create a corpus dir in the folder
            Path path = Paths.get(System.getProperty("user.dir"))
                    .resolve("failure");
            path.toFile().mkdir();
            Config.instance.failureDir = path.toString();
        }

        String type = cmd.getOptionValue(clazzOption);
        logger.info("start " + type);
        if (type.equalsIgnoreCase("server")) {
            new FuzzingServer().start();
        } else if (type.equalsIgnoreCase("client")) {
            FuzzingClient fuzzingClient = new FuzzingClient();
            if (cmd.hasOption(flagOption)) {
                StringBuilder optionBuilder = new StringBuilder();
                optionBuilder.append(cmd.getOptionValue(flagOption));
                System.out.println(optionBuilder.toString());
                int group = (optionBuilder.toString()
                        .equals("group1"))
                                ? 1
                                : 2;
                fuzzingClient.group = group;
            }
            if (cmd.hasOption(downgradeSupportOption)) {
                StringBuilder optionBuilder = new StringBuilder();
                optionBuilder
                        .append(cmd.getOptionValue(downgradeSupportOption));
                System.out.println(optionBuilder.toString());
                fuzzingClient.isDowngradeSupported = (optionBuilder.toString()
                        .equals("Y"))
                                ? true
                                : false;
            }
            fuzzingClient.start();
        } else if (type.equalsIgnoreCase("fuzzer")) {
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
        }
    }
}
