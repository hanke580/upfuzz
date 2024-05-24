package org.zlab.upfuzz.hbase.namespace;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.utils.CONSTANTSTRINGType;
import org.zlab.upfuzz.utils.INTType;
import org.zlab.upfuzz.utils.UUIDType;
import org.zlab.upfuzz.utils.Utilities;

public class CREATE_NAMESPACE extends HBaseCommand {

    /**
     * optional properties: might not be exhaustive
     * hbase> create_namespace 'ns1', {'hbase.namespace.quota.maxregions'=>'10'}
     * hbase> create_namespace 'ns1', {'hbase.namespace.quota.maxtables'=>'5'}
     */

    public CREATE_NAMESPACE(HBaseState state) {
        super(state);
        ParameterType.ConcreteType nsNameType = new ParameterType.NotInCollectionType(
                new ParameterType.NotEmpty(UUIDType.instance),
                (s, c) -> ((HBaseState) s).getNamespaces(), null);
        Parameter nsName = nsNameType
                .generateRandomParameter(state, this);
        this.params.add(nsName);

        Parameter maxRegions = new ParameterType.OptionalType(
                new INTType(5, 20), null).generateRandomParameter(state, this);
        params.add(maxRegions);

        Parameter maxTables = new ParameterType.OptionalType(new INTType(3, 50),
                null).generateRandomParameter(state, this);
        this.params.add(maxTables);
    }

    @Override
    public String constructCommandString() {
        String tableName = this.params.get(0).toString();
        String maxRegions = this.params.get(1).toString();
        String maxTables = this.params.get(2).toString();
        StringBuilder sb = new StringBuilder();
        sb.append("create_namespace '").append(tableName).append("'");
        if (maxTables.isEmpty() && maxRegions.isEmpty()) {
            return sb.toString();
        }
        sb.append(", {");
        if (maxRegions.isEmpty())
            sb.append(
                    String.format("'hbase.namespace.quota.maxregions' => '%s'",
                            maxTables));
        else {
            sb.append(String.format("'hbase.namespace.quota.maxtables' => '%s'",
                    maxRegions));
            if (!maxTables.isEmpty())
                sb.append(String.format(
                        ", 'hbase.namespace.quota.maxregions' => '%s'",
                        maxTables));
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public void updateState(State state) {
        ((HBaseState) state).addNamespace(params.get(0).getValue().toString());
    }

    @Override
    public void separate(State state) {
        this.params.get(0).regenerate(null, this);
    }

}
