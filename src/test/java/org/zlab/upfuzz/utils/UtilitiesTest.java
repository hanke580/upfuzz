package org.zlab.upfuzz.utils;

import org.junit.jupiter.api.Test;

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
}
