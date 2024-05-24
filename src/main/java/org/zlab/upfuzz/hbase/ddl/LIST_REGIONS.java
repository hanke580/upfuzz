package org.zlab.upfuzz.hbase.ddl;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.Utilities;

import java.util.Random;

public class LIST_REGIONS extends HBaseCommand {
    public LIST_REGIONS(HBaseState state) {
        super(state);
        Parameter tableName = chooseTable(state, this, null);
        // TODO: server_name parameter
        this.params.add(tableName); // [0] tableName
        Parameter localityThreshold = new ParameterType.OptionalType(
                new ParameterType.InCollectionType(
                        CONSTANTSTRINGType.instance,
                        (s, c_) -> Utilities
                                .strings2Parameters(
                                        LIST_REGIONS_LOCALITY_THRESHOLD),
                        null),
                null).generateRandomParameter(state, this);
        this.params.add(localityThreshold); // [1] locality threshold
    }

    @Override
    public String constructCommandString() {
        String localityThreshold = params.get(1).toString();
        return "list_regions " + "'" + params.get(0) + "'" +
                (localityThreshold.isEmpty() ? ""
                        : ", LOCALITY_THRESHOLD => " + localityThreshold);
    }

    @Override
    public void updateState(State state) {
    }
}
