package org.zlab.upfuzz;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import info.debatty.java.stringsimilarity.QGram;
import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.cassandra.CassandraCommands;
import org.zlab.upfuzz.cassandra.CassandraState;

public class CommandSequenceTests {

    @Test
    public void testSequenceGeneration()
            throws InvocationTargetException, IllegalAccessException,
            NoSuchMethodException, InstantiationException {

        boolean useIdx = false;

        CassandraState state = new CassandraState();
        CommandSequence commandSequence = CommandSequence.generateSequence(
                CassandraCommands.commandClassList,
                CassandraCommands.createCommandClassList, CassandraState.class,
                null);

        List<String> l = commandSequence.getCommandStringList();
        for (int i = 0; i < l.size(); i++) {
            if (useIdx)
                System.out.println("[" + i + "]" + "\t" + l.get(i));
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
                System.out.println("[" + i + "]" + "\t" + l.get(i));
            else
                System.out.println(l.get(i));
        }
        System.out.println("command size = " + l.size());

    }

    @Test
    public void testMutation()
            throws InvocationTargetException, IllegalAccessException,
            NoSuchMethodException, InstantiationException {
        CommandSequence commandSequence = CommandTests
                .cass13939CommandSequence();
        boolean mutateStatus = commandSequence.mutate();
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
        CommandSequence commandSequence = CommandTests
                .cass13939CommandSequence();

        try {
            commandSequence.mutate();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        for (String cmdStr : commandSequence.getCommandStringList()) {
            System.out.println(cmdStr);
        }
    }

    @Test
    public void test() {
        String str0 = "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',0,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                + "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',1,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                + "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',2,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                + "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',3,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                + "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',4,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                + "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',5,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                + "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',6,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                + "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',7,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n";
        String str1 = "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',0,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                + "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',1,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                + "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',2,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                + "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',3,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                + "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',4,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                + "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',5,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                + "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',6,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                + "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',7,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                + "ALTER TABLE tb DROP population ;\n";

        String str2 = "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',0,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                + "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',1,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                + "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',2,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                + "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',3,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                + "INSERT INTO tb (species, common_name, population, average_size) VALUES ('Monkey',4,30,'AAAAAAAAAAAAAAAAAAAAAAAAAAA');\n"
                + "ALTER TABLE tb DROP population ;\n"
                + "INSERT INTO tb (species, average_size, common_name) VALUES ('Monkey','population',5);\n"
                + "INSERT INTO tb (species, average_size, common_name) VALUES ('tb','tb',2);\n"
                + "INSERT INTO tb (species, common_name, average_size) VALUES ('common_name',0,'species');\n";

        QGram l = new QGram();
        System.out.println(l.distance(str0, str1));
        System.out.println(l.distance(str0, str1));

    }

}
