package org.zlab.upfuzz.fuzzingengine;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zlab.upfuzz.AbstractTest;
import org.zlab.upfuzz.fuzzingengine.Config.Configuration;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;

public class FuzzingClientTest extends AbstractTest {

    @BeforeAll
    static public void initAll() {
        String configFile = "./config.json";
        Configuration cfg;
        try {
            cfg = new Gson().fromJson(new FileReader(configFile),
                    Configuration.class);
            Config.setInstance(cfg);
        } catch (JsonSyntaxException | JsonIOException
                | FileNotFoundException e) {
            e.printStackTrace();
            assert false;
        }
    }

    @Test
    public void testJacoco() {
        // Executor nullExecutor = new NullExecutor(null, null);
        // FuzzingClient fc = new FuzzingClient();
        // fc.start(nullExecutor);
    }

    // @Test
    // FIXME test jacoo collect
    public void testJacocoCollect() {
        // Executor nullExecutor = new NullExecutor(null, null);
        // FuzzingClient fc = new FuzzingClient();
        // byte[] bs = new byte[65536];
        // nullExecutor.executorID = "nullExecutor";
        // System.out.println("id: " + nullExecutor.executorID);
        // fc.start(nullExecutor);
        // while (true) {
        // try {
        // Thread.sleep(10000);
        // } catch (InterruptedException e) {
        // e.printStackTrace();
        // }
        // System.out.println("collect");
        // Long startTime = System.currentTimeMillis();
        // DateFormat formatter = new SimpleDateFormat(
        // "yyyy-MM-dd HH:mm:ss.SSS");
        // System.out.println(formatter.format(System.currentTimeMillis())
        // + "\n" + "ask for dump");

        // fc.collect(nullExecutor);
        // Long endTime = System.currentTimeMillis();
        // System.out.println("collect time usage: " + (endTime - startTime)
        // + "\n" + DurationFormatUtils.formatDuration(
        // endTime - startTime, "HH:mm:ss.SSS"));
        // }
    }

    // @Test
    public void testTcpPacketSize() {
        // Executor nullExecutor = new NullExecutor(null, null);
        // FuzzingClient fc = new FuzzingClient();
        // byte[] bs = new byte[65536];
        // nullExecutor.executorID = "nullExecutor";
        // System.out.println("id: " + nullExecutor.executorID);
        // fc.start(nullExecutor);

        // try {
        // Socket socket = new Socket("127.0.0.1", 6300);
        // socket.setSendBufferSize(128 * 1024);
        // socket.setReceiveBufferSize(128 * 1024);
        // System.out.println("received: " + socket.getReceiveBufferSize()
        // + "\n" + "send" + socket.getSendBufferSize());
        // bs = RandomUtils.nextBytes(65535);
        // socket.getOutputStream().write(bs);
        // } catch (IOException e) {
        // e.printStackTrace();
        // }
    }
}
