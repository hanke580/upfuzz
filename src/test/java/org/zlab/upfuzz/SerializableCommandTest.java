package org.zlab.upfuzz;

import java.util.LinkedList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.ParameterType.ConcreteGenericType;
import org.zlab.upfuzz.ParameterType.ConcreteGenericTypeOne;
import org.zlab.upfuzz.ParameterType.ConcreteType;
import org.zlab.upfuzz.ParameterType.GenericType;
import org.zlab.upfuzz.cassandra.CassandraCommands;
import org.zlab.upfuzz.cassandra.CassandraParameterAdapter;
import org.zlab.upfuzz.cassandra.CassandraState;
import org.zlab.upfuzz.cassandra.CassandraTypeAdapter;
import org.zlab.upfuzz.cassandra.CassandraTypes;
import org.zlab.upfuzz.cassandra.LambdaInterfaceAdapter;
import org.zlab.upfuzz.utils.PAIRType;
import org.zlab.upfuzz.utils.STRINGType;

import junit.framework.TestCase;

public class SerializableCommandTest extends TestCase {

    public void testNothing() {

    }
}
