package org.zlab.upfuzz.fuzzingengine;

import java.lang.management.ManagementFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.zlab.upfuzz.fuzzingengine.FuzzingClient;
import org.zlab.upfuzz.fuzzingengine.FuzzingClient.FuzzingClientActions;
import org.zlab.upfuzz.fuzzingengine.FuzzingServer.FuzzingServerActions;

public class Main {

    public static void main(String[] args) throws ParseException {
        long currentTime = System.currentTimeMillis();
        long vmStartTime = ManagementFactory.getRuntimeMXBean().getStartTime();
        System.out.println("jvm startup time million seconds: " + (currentTime - vmStartTime));
        final Options options = new Options();
        Option clazzOption = Option.builder("class").argName("type").hasArg().desc("start a dfe server or client")
                .required().build();
        Option actionOption = Option.builder("action").argName("action").hasArg().desc("start a dfe server or client")
                .required().build();
        Option portOption = Option.builder("port").argName("port").hasArg().desc("server port").build();
        options.addOption(clazzOption);
        options.addOption(actionOption);
        options.addOption(portOption);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        Config conf = new Config();

        if (cmd.hasOption(portOption)) {
            String port = cmd.getOptionValue(portOption);
            conf.serverPort = Integer.valueOf(port);
        }

        String type = cmd.getOptionValue(clazzOption);
        if (type.toLowerCase().equals("server")) {
            String act = cmd.getOptionValue(actionOption);
            FuzzingServerActions action = FuzzingServerActions.valueOf(act);
            switch (action) {
            case start: {
                new FuzzingClient(conf).start();
                break;
            }
            default:
                throw new UnsupportedOperationException(act);
            }
        } else if (type.toLowerCase().equals("client")) {
            String act = cmd.getOptionValue(actionOption);
            FuzzingClientActions action = FuzzingClientActions.valueOf(act);
            switch (action) {
            case start:{
                new FuzzingClient(conf).start();
                break;
            }
            case collect: {
                // new FuzzingClient(conf).collect();
                break;
            }
            default:
                throw new UnsupportedOperationException(act);
            }
        }
    }
}
