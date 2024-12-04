package org.zlab.upfuzz.hdfs.ec;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.hdfs.HdfsCommand;

import java.util.LinkedList;
import java.util.List;

/**
 * hdfs ec [generic options]
 *      [-setPolicy -path <path> [-policy <policyName>] [-replicate]]
 *      [-getPolicy -path <path>]
 *      [-unsetPolicy -path <path>]
 *      [-listPolicies]
 *      [-addPolicies -policyFile <file>]
 *      [-listCodecs]
 *      [-removePolicy -policy <policyName>]
 *      [-enablePolicy -policy <policyName>]
 *      [-disablePolicy -policy <policyName>]
 *      [-help [cmd ...]]
 *
 *      URL: https://docs.cloudera.com/HDPDocuments/HDP3/HDP-3.0.1/data-storage/content/erasure_coding_commands.html
 */
public abstract class ErasureCoding extends HdfsCommand {

    String type = "ec";

    public static List<String> policies = new LinkedList<>();

    static {
        policies.add("RS-3-2-1024k");
        policies.add("RS-6-3-1024k");
        policies.add("RS-LEGACY-6-3-1024k");
        policies.add("XOR-2-1-1024k");
    }

    public ErasureCoding(String subdir) {
        super(subdir);
    }

    @Override
    public String constructCommandString() {
        StringBuilder ret = new StringBuilder();
        ret.append("ec");
        for (Parameter p : params) {
            String ps = p.toString();
            ret.append(" ");
            ret.append(ps);
        }
        return ret.toString();
    }

    @Override
    public String toString() {
        return constructCommandString();
    }
}
