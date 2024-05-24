package org.zlab.upfuzz.hbase.dml;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.Utilities;

public class SCAN extends HBaseCommand {
    public SCAN(HBaseState state) {
        super(state);
        Parameter tableName = chooseTable(state, this, null);
        this.params.add(tableName); // 0 tableName

        Parameter formatterClass = new ParameterType.OptionalType(
                new ParameterType.InCollectionType(
                        CONSTANTSTRINGType.instance,
                        (s, c) -> Utilities
                                .strings2Parameters(
                                        FORMATTER_CLASS_TYPES),
                        null),
                null).generateRandomParameter(state, this);
        this.params.add(formatterClass);

        Parameter formatter = new ParameterType.OptionalType(
                new ParameterType.InCollectionType(
                        CONSTANTSTRINGType.instance,
                        (s, c) -> Utilities
                                .strings2Parameters(
                                        DEFAULT_FORMATTER_FUNCTIONS),
                        null),
                null).generateRandomParameter(state, this);
        this.params.add(formatter);
    }

    @Override
    public String constructCommandString() {
        Parameter tableName = this.params.get(0);
        Parameter formatterClass = this.params.get(1);
        Parameter formatter = this.params.get(2);
        StringBuilder sb = new StringBuilder();
        sb.append("scan '").append(tableName).append("'");
        if (formatter.toString().isEmpty()
                && formatterClass.toString().isEmpty()) {
            return sb.toString();
        }
        sb.append(", {");
        // TODO: some of the formatter options don't work, see why
        if (formatterClass.toString().isEmpty())
            sb.append(String.format("FORMATTER => '%s'", formatter.toString()));
        else {
            sb.append(String.format("FORMATTER_CLASS => '%s'",
                    formatterClass.toString()));
            if (!formatter.toString().isEmpty())
                sb.append(String.format(", FORMATTER => '%s'",
                        formatter.toString()));
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public void updateState(State state) {
    }
}
