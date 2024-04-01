package org.zlab.upfuzz.cassandra;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import junit.framework.TestCase;
import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.cassandra.CassandraCqlshDaemon.CqlshPacket;

public class CassandraCqlshDaemonTest extends TestCase {
    protected void setUp() {
    }

    @Test
    public void testFromJson() {
        String jsonString = "{\"cmd\":\"CREATE KEYSPACE IF NOT EXISTS OXBJMXLSGDFXBKX WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 2 };\",\"exitValue\":0,\"timeUsage\":9.5367431640625e-07,\"message\":\"Test\"}";
        CqlshPacket cqlshPacket = new Gson().fromJson(jsonString,
                CqlshPacket.class);
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
        System.out.println(gson.toJson(cqlshPacket, CqlshPacket.class));
    }

    @Test
    public void testCqlshPythonScript() {
        // System.out.println(CassandraCqlshDaemon.cqlshPython2Script);
        // System.out.println(CassandraCqlshDaemon.cqlshPython3Script);
    }
}
