package org.zlab.upfuzz.hbase.quotas;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.INTType;
import org.zlab.upfuzz.utils.Utilities;

public class SET_QUOTA_SPACE
        extends org.zlab.upfuzz.hbase.HBaseCommand {

    public SET_QUOTA_SPACE(HBaseState state) {
        super(state);

        Parameter tableName = chooseTable(state, this);
        this.params.add(tableName); // 0 tableName

        Parameter limit = new ParameterType.OptionalType(new INTType(1, 50),
                null).generateRandomParameter(state, this);
        this.params.add(limit); // 1 limit

        Parameter policy_type = new ParameterType.InCollectionType(
                CONSTANTSTRINGType.instance,
                (s, c) -> Utilities.strings2Parameters(
                        QUOTA_SPACE_POLICY_TYPES),
                null).generateRandomParameter(null, null);
        this.params.add(policy_type); // 0 throttle_type

    }

    @Override
    public String constructCommandString() {
        // hbase> set_quota TYPE => SPACE, TABLE => 'my_table', LIMIT => '10G'
        return String.format(
                "set_quota TYPE => SPACE, TABLE => '%s', LIMIT => '%sG', POLICY => %s",
                params.get(0), params.get(1), params.get(2));
    }

    @Override
    public void updateState(State state) {

    }
}
