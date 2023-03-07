package org.zlab.upfuzz.hbase.DataDefinitionCommands;

import org.zlab.upfuzz.Parameter;
import org.zlab.upfuzz.ParameterType;
import org.zlab.upfuzz.State;
import org.zlab.upfuzz.hbase.HBaseCommand;
import org.zlab.upfuzz.hbase.HBaseColumnFamily;
import org.zlab.upfuzz.hbase.HBaseState;
import org.zlab.upfuzz.hbase.HBaseTypes;
import org.zlab.upfuzz.utils.*;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class CREATE extends HBaseCommand {

    public CREATE(State state) {
        super();

        ParameterType.ConcreteType tableNameType = new ParameterType.NotInCollectionType(
                new ParameterType.NotEmpty(UUIDType.instance),
                (s, c) -> ((HBaseState) s).getTables(), null);
        Parameter tableName = tableNameType
                .generateRandomParameter(state, this);
        this.params.add(tableName); // [0]=tableName

        ParameterType.ConcreteType columnFamiliesType = // LIST<PAIR<String,TYPEType>>
                new ParameterType.NotEmpty(ParameterType.ConcreteGenericType
                        .constructConcreteGenericType(
                                HBaseTypes.MapLikeListType.instance,
                                ParameterType.ConcreteGenericType
                                        .constructConcreteGenericType(
                                                PAIRType.instance,
                                                new ParameterType.NotEmpty(
                                                        new STRINGType(20)),
                                                HBaseTypes.TYPEType.instance)));
        Parameter columnFamilies = columnFamiliesType
                .generateRandomParameter(state, this);
        params.add(columnFamilies); // [1]=columnFamilies
    }

    @Override
    public String constructCommandString() {
        // TODO: Need a helper function, add space between all strings
        Parameter tableName = params.get(0);
        Parameter columnFamilies = params.get(1); // LIST<PAIR<TEXTType,TYPE>>
        StringBuilder commandStr = new StringBuilder("CREATE " + "'" + tableName.toString() + "'");

        String columnFamiliesString = columnFamilies.toString();
        for (String colFamiStr: columnFamiliesString.split(",")){
            String colFamiName = colFamiStr.substring(0, colFamiStr.indexOf(" "));
            commandStr.append(", '"+colFamiName+"'");
        }
        return commandStr.toString();
    }

    @Override
    public void updateState(State state) {
        Parameter tableName = params.get(0);
        ((HBaseState) state).addTable(tableName.toString());

        Parameter columnFamilies = params.get(1); // LIST<PAIR<TEXTType,TYPE>>

        String columnFamiliesString = columnFamilies.toString();
        for (String colFamiStr: columnFamiliesString.split(",")){
            String columnFamilyName = colFamiStr.substring(0, colFamiStr.indexOf(" "));
            HBaseColumnFamily hBaseColumnFamily = new HBaseColumnFamily(columnFamilyName.toString(), null);
            ((HBaseState) state).addColumnFamily(tableName.toString(), columnFamilyName.toString(), hBaseColumnFamily);
        }
    }
}
