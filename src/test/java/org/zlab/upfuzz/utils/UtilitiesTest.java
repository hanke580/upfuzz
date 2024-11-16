package org.zlab.upfuzz.utils;

import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.docker.DockerMeta;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UtilitiesTest {

    @Test
    public void testMaskRubyObject() {

        String input = "Old Version Result: HOST  REGION\n" +
                " hregion2:16020 {ENCODED => 7a529323a5cf21e55f89208b99d6cc15, NAME => 'uuid2217c5c8ac544928912ffe83069309ea,,1694980141812.7a529323a5cf21e55f89208b99d6cc15.', STARTKEY => '', ENDKEY => ''}\n"
                +
                "1 row(s)\n" +
                "Took 1.6793 seconds\n" +
                "=> #<Java::OrgApacheHadoopHbase::HRegionLocation:0x7305191e>";
        String output = Utilities.maskRubyObject(input);
        System.out.println(output);
    }

    @Test
    public void testSetRandomDeleteAtLeaseOneItem() {
        Set<String> set = new HashSet<>();
        set.add("a");
        set.add("b");
        set.add("c");
        set.add("d");
        Boolean status = Utilities.setRandomDeleteAtLeaseOneItem(set);
        System.out.println(set);
        System.out.println(status);
    }

    @Test
    public void testExponentialProbabilityModel() {
        Utilities.ExponentialProbabilityModel model = new Utilities.ExponentialProbabilityModel(
                0.4, 0.1, 5);
        assert model.calculateProbability(0) == 0.4;
        System.out.println(model.calculateProbability(10));
    }

    @Test
    public void testComputeMF() {
        Map<String, Map<String, String>> oriClassInfo = new HashMap<>();
        Map<String, Map<String, String>> upClassInfo = new HashMap<>();
        oriClassInfo.put("A", new HashMap<>());
        oriClassInfo.put("B", new HashMap<>());
        oriClassInfo.put("C", new HashMap<>());

        oriClassInfo.get("A").put("f1", "int");
        oriClassInfo.get("B").put("f1", "String");
        oriClassInfo.get("C").put("f2", "List");

        upClassInfo.put("A", new HashMap<>());
        upClassInfo.put("B", new HashMap<>());
        upClassInfo.put("D", new HashMap<>());

        upClassInfo.get("A").put("f1", "int");
        upClassInfo.get("B").put("f1", "String");
        upClassInfo.get("D").put("f2", "List");

        Map<String, Map<String, String>> mf = Utilities.computeMF(oriClassInfo,
                upClassInfo);
        assert mf.containsKey("A");
        assert mf.containsKey("B");
        assert mf.get("B").containsKey("f1");
    }

    @Test
    public void testIsBlackListed() {
        String errorLog = "        at " +
                "org.apache.cassandra.db.composites.CompoundSparseCellNameType.create"
                +
                "(CompoundSparseCellNameType.java:126) " +
                "~[apache-cassandra-2.2.19-SNAPSHOT.jar:2.2.19-SNAPSHOT]";
        Set<String> blackListErrorLog = new HashSet<>();
        blackListErrorLog.add(
                "org.apache.cassandra.db.composites.CompoundSparseCellNameType.create"
                        +
                        "(CompoundSparseCellNameType.java:126)" +
                        " ~[apache-cassandra-2.2.19-SNAPSHOT.jar:2.2.19-SNAPSHOT]");

        assert DockerMeta.isBlackListed(errorLog, blackListErrorLog);
    }

    @Test
    public void testComputeChangedClasses() {
        Map<String, Map<String, String>> oriClassInfo = new HashMap<>();
        Map<String, Map<String, String>> upClassInfo = new HashMap<>();
        oriClassInfo.put("A", new HashMap<>());
        oriClassInfo.put("B", new HashMap<>());
        oriClassInfo.put("C", new HashMap<>());
        oriClassInfo.put("E", new HashMap<>());
        oriClassInfo.put("F", new HashMap<>());

        oriClassInfo.get("A").put("f1", "int");
        oriClassInfo.get("B").put("f1", "String");
        oriClassInfo.get("C").put("f2", "List");
        oriClassInfo.get("E").put("f2", "List");
        oriClassInfo.get("F").put("f1", "int");
        oriClassInfo.get("F").put("f2", "String");

        upClassInfo.put("A", new HashMap<>());
        upClassInfo.put("B", new HashMap<>());
        upClassInfo.put("D", new HashMap<>());
        upClassInfo.put("E", new HashMap<>());
        upClassInfo.put("F", new HashMap<>());

        upClassInfo.get("A").put("f1", "int");
        upClassInfo.get("B").put("f1", "String");
        upClassInfo.get("D").put("f2", "List");
        upClassInfo.get("E").put("f2", "Array");
        upClassInfo.get("F").put("f1", "int");
        upClassInfo.get("F").put("f2", "String");
        upClassInfo.get("F").put("f3", "bool");

        Set<String> changedClasses = Utilities.computeChangedClasses(
                oriClassInfo,
                upClassInfo);
        assert changedClasses.contains("C");
        assert changedClasses.contains("E");
        assert changedClasses.contains("F");
        assert !changedClasses.contains("D");
    }
}
