package org.zlab.upfuzz.utils;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
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
}
