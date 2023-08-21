package org.zlab.upfuzz.hdfs.dfsadmin;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class SaveNamespace extends Dfsadmin {

    /*
     * Save current namespace into storage directories and reset edits log.
     * Requires safe mode. If the “beforeShutdown” option is given, the NameNode
     * does a checkpoint if and only if no checkpoint has been done during a
     * time window (a configurable number of checkpoint periods). This is
     * usually used before shutting down the NameNode to prevent potential
     * fsimage/editlog corruption.
     */
    public SaveNamespace(HdfsState hdfsState) {
        super(hdfsState.subdir);

        Parameter saveNamespaceCmd = new CONSTANTSTRINGType("-saveNamespace")
                .generateRandomParameter(null, null);

        Parameter beforeShutdownOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-beforeShutdown"), null)
                        .generateRandomParameter(null, null);

        params.add(saveNamespaceCmd);
        params.add(beforeShutdownOption);
    }

    @Override
    public void updateState(State state) {
    }
}
