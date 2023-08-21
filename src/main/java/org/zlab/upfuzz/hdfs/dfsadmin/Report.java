package org.zlab.upfuzz.hdfs.dfsadmin;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hdfs.HdfsState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;

public class Report extends Dfsadmin {

    /*
     * Save current namespace into storage directories and reset edits log.
     * Requires safe mode. If the “beforeShutdown” option is given, the NameNode
     * does a checkpoint if and only if no checkpoint has been done during a
     * time window (a configurable number of checkpoint periods). This is
     * usually used before shutting down the NameNode to prevent potential
     * fsimage/editlog corruption.
     */
    public Report(HdfsState state) {
        super(state.subdir);

        Parameter reportCmd = new CONSTANTSTRINGType("-report")
                .generateRandomParameter(null,
                        null);

        // [-Live]
        Parameter liveOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-live"), null)
                        .generateRandomParameter(null, null);
        // [-dead]
        Parameter deadOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-dead"), null)
                        .generateRandomParameter(null, null);
        // [-decommissioning]
        Parameter decommissioningOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-decommissioning"), null)
                        .generateRandomParameter(null, null);
        // [-enteringmaintenance]
        Parameter enteringmaintenanceOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-enteringmaintenance"),
                null)
                        .generateRandomParameter(null, null);
        // [-inmaintenance]
        Parameter inmaintenanceOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-inmaintenance"), null)
                        .generateRandomParameter(null, null);
        // [-inmaintenance]
        Parameter slownodesOption = new ParameterType.OptionalType(
                new CONSTANTSTRINGType("-slownodes"), null)
                        .generateRandomParameter(null, null);

        params.add(reportCmd);
        params.add(liveOption);
        params.add(deadOption);
        params.add(decommissioningOption);
        params.add(enteringmaintenanceOption);
        params.add(inmaintenanceOption);
        params.add(slownodesOption);
    }

    @Override
    public void updateState(State state) {
    }
}
