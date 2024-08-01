package org.zlab.upfuzz;

import info.debatty.java.stringsimilarity.QGram;

import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.cassandra.*;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.fuzzingengine.server.Seed;

public class CommandSequenceTest extends AbstractTest {
    protected final Logger logger = LogManager.getLogger(getClass());

    @Test
    public void testSequenceGeneration()
            throws Exception {

        boolean useIdx = false;

        CommandSequence commandSequence = CommandSequence.generateSequence(
                CassandraCommand.cassandraCommandPool.commandClassList,
                CassandraCommand.cassandraCommandPool.createCommandClassList,
                CassandraState.class,
                null, false);

        List<String> l = commandSequence.getCommandStringList();
        for (int i = 0; i < l.size(); i++) {
            if (useIdx)
                System.out.println("[" + i + "]"
                        + "\t" + l.get(i));
            else
                System.out.println(l.get(i));
        }
        System.out.println("command size = " + l.size());

        if (l.size() == 0)
            return;

        System.out.println("\n-----------Sequence Mutation Start-----------");
        commandSequence.mutate();
        System.out.println("-----------Sequence Mutation End-----------\n");

        l = commandSequence.getCommandStringList();
        for (int i = 0; i < l.size(); i++) {
            if (useIdx)
                System.out.println("[" + i + "]"
                        + "\t" + l.get(i));
            else
                System.out.println(l.get(i));
        }
        System.out.println("command size = " + l.size());
    }

    @Test
    public void testMutation()
            throws Exception {
        CommandSequence commandSequence = CommandTest
                .cass13939CommandSequence();

        System.out.println("Ready to execute mutation!");

        boolean mutateStatus = commandSequence.mutate();

        // FIXME
        // Sometimes the mutation can be run in a forever loop
        // We need to find out when will it happen and fix it
        // This should be a bug in the mutation logic.

        if (!mutateStatus) {
            System.out.println("Mutate failed");
        } else {
            System.out.println("After Mutation");
            for (String cmdStr : commandSequence.getCommandStringList()) {
                System.out.println(cmdStr);
            }
        }
    }

    @Test
    public void genUUID() {
        final String uuid = UUID.randomUUID().toString().replace("-", "");
        System.out.println("uuid = " + uuid);
    }

    @Test
    public void testTypeIsValidCheck() {
        CommandSequence commandSequence = CommandTest
                .cass13939CommandSequence();

        try {
            commandSequence.mutate();
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (String cmdStr : commandSequence.getCommandStringList()) {
            System.out.println(cmdStr);
        }
    }

    @Test
    public void test() {
        String str0 = "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',0,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                +
                "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',1,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                +
                "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',2,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                +
                "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',3,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                +
                "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',4,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                +
                "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',5,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                +
                "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',6,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                +
                "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',7,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n";
        String str1 = "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',0,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                +
                "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',1,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                +
                "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',2,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                +
                "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',3,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                +
                "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',4,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                +
                "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',5,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                +
                "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',6,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                +
                "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',7,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                + "ALTER TABLE tb DROP population ;\n";

        String str2 = "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',0,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                +
                "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',1,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                +
                "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',2,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                +
                "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',3,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                +
                "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',4,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                + "ALTER TABLE tb DROP population ;\n"
                +
                "INSERT INTO tb (species, average_size, common_name) VALUES ('Monkey','population',5);\n"
                +
                "INSERT INTO tb (species, average_size, common_name) VALUES ('tb','tb',2);\n"
                +
                "INSERT INTO tb (species, common_name, average_size) VALUES ('common_name',0,'species');\n";

        QGram l = new QGram();
        System.out.println(l.distance(str0, str1));
        System.out.println(l.distance(str0, str1));
    }

    @Test
    public void testSeedGeneration() {
        Config.instance.system = "cassandra";
        CassandraCommandPool pool = new CassandraCommandPool();
        Seed seed = Seed.generateSeed(pool, CassandraState.class, -1, -1);

        boolean print = false;
        if (print) {
            if (seed != null) {
                for (String str : seed.originalCommandSequence
                        .getCommandStringList()) {
                    logger.info(str);
                }
                logger.info("\n\n");
                for (String str : seed.validationCommandSequence
                        .getCommandStringList()) {
                    logger.info(str);
                }
                logger.info("size = " + seed.validationCommandSequence
                        .getCommandStringList().size());
            }
        }
    }
}
