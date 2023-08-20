package org.zlab.upfuzz;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.ParameterType.ConcreteGenericType;
import org.zlab.upfuzz.ParameterType.ConcreteType;
import org.zlab.upfuzz.cassandra.CassandraTypes;
import org.zlab.upfuzz.utils.STRINGType;

public class BuildCommandWithInitialValueTest extends AbstractTest {
    protected void setUp() {
    }

    @Test
    public void testBuildString() {
        ParameterType t2 = new STRINGType();
        String v2 = "hello";
        Parameter p1 = new Parameter((ConcreteType) t2, (Object) v2);
    }

    @Test
    public void testBuildStringList() {
        ParameterType t2 = new STRINGType();
        String v2 = "hello";
        Parameter p1 = new Parameter((ConcreteType) t2, (Object) v2);

        ParameterType t22 = new STRINGType();
        String v22 = "world";
        Parameter p11 = new Parameter((ConcreteType) t22, (Object) v22);

        ConcreteGenericType t1 = ConcreteGenericType
                .constructConcreteGenericType(
                        new CassandraTypes.LISTType(), new STRINGType());
        // List<Parameter<String>>{ Parameter<int> Parameter<string> } ;
        // List<Parameter>
        Parameter p0 = new Parameter(t1, Arrays.asList(p1, p11));
        System.out.println("list<string>: " + p0.toString());
    }
}
