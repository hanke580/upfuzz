package org.zlab.upfuzz.hdfs.eccommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.Utilities;

import java.util.LinkedList;
import java.util.List;

public class HelpCommand extends ErasureCodingCommand {

    public static List<String> ecCommands = new LinkedList<>();

    static {
        policies.add("setPolicy");
        policies.add("getPolicy");
        policies.add("unsetPolicy");
        policies.add("listPolicies");
        policies.add("addPolicies");
        policies.add("listCodecs");
        policies.add("removePolicy");
        policies.add("enablePolicy");
        policies.add("disablePolicy");
    }

    public HelpCommand(HdfsState state) {
        super(state.subdir);

        Parameter helpCmd = new CONSTANTSTRINGType("-help")
                .generateRandomParameter(null, null);

        Parameter cmd = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities
                        .strings2Parameters((((HelpCommand) c).ecCommands)),
                null).generateRandomParameter(null, null);

        params.add(helpCmd);
        params.add(cmd);
    }

    @Override
    public void updateState(State state) {
    }

}
