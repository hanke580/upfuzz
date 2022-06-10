package org.zlab.upfuzz.fuzzingengine;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.ExecutionDataWriter;
import org.jacoco.core.data.IExecutionDataVisitor;
import org.jacoco.core.data.ISessionInfoVisitor;
import org.jacoco.core.data.SessionInfo;
import org.jacoco.core.runtime.RemoteControlReader;
import org.jacoco.core.runtime.RemoteControlWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientHandler
        implements Runnable, ISessionInfoVisitor, IExecutionDataVisitor {

    static Logger logger = LogManager.getLogger(ClientHandler.class);

    private final FuzzingClient client;
    private final Socket socket;
    private String sessionId;
    private SessionInfo sesInfo;

    public CountDownLatch okCMD = new CountDownLatch(1);

    private final RemoteControlReader reader;
    private final RemoteControlWriter writer;

    private final ExecutionDataWriter fileWriter;

    private final int maxn = 10240;

    private boolean registered = false;

    private byte[] buffer;

    ClientHandler(final FuzzingClient client, final Socket socket,
            ExecutionDataWriter fileWriter) throws IOException {
        this.client = client;
        this.socket = socket;
        this.fileWriter = fileWriter;

        this.socket.setSendBufferSize(128 * 1024);
        this.socket.setReceiveBufferSize(128 * 1024);

        // Just send a valid header:
        writer = new RemoteControlWriter(socket.getOutputStream());

        reader = new RemoteControlReader(socket.getInputStream());
        reader.setSessionInfoVisitor(this);
        reader.setExecutionDataVisitor(this);
        buffer = new byte[maxn];
    }

    @Override
    public void run() {
        try {
            while (reader.read()) {
                okCMD.countDown();
            }

            logger.info("connection closed");
            socket.close();
            // synchronized (fileWriter) {
            // fileWriter.flush();
            // }
        } catch (final IOException e) {
            e.printStackTrace();
            // client.agentHandler.remove(sessionId);
        }
    }

    private void register(SessionInfo info) {
        sesInfo = info;
        sessionId = info.getId();
        String[] sessionSplit = sessionId.split("-");
        if (sessionSplit.length != 3) {
            System.err.println("Invalid sessionId " + sessionId);
            return;
        }
        logger.info("Agent " + info.getId() + " registered");
        client.agentHandler.put(sessionId, this);
        logger.info("agent handler add " + sessionId);

        String identifier = sessionSplit[0], executor = sessionSplit[1],
                index = sessionSplit[2];
        if (!client.sessionGroup.containsKey(executor)) {
            client.sessionGroup.put(executor, new ArrayList<>());
        }
        client.sessionGroup.get(executor).add(sessionId);
        registered = true;
    }

    public void visitSessionInfo(final SessionInfo info) {
        if (!registered) {
            register(info);
        } else {
            logger.info(
                    "Retrieving execution Data for session: " + info.getId());
        }
        // synchronized (fileWriter) {
        // fileWriter.visitSessionInfo(info);
        // }
    }

    public void visitClassExecution(final ExecutionData data) {
        // logger.info(sessionId + " get data");
        // logger.info(data.getName());
        if (client.agentStore.containsKey(sessionId)) {
            ExecutionDataStore store = client.agentStore.get(sessionId);

            ExecutionData preData = store.get(data.getId());
            if (preData != null) {
                // FIXME take the maxinum value when merging data
                data.merge(preData, false);
            }
            store.put(data);
            client.agentStore.put(sessionId, store);
        } else {
            ExecutionDataStore store = new ExecutionDataStore();
            store.put(data);
            client.agentStore.put(sessionId, store);
        }
        // synchronized (fileWriter) {
        // fileWriter.visitClassExecution(data);
        // }

    }

    public void collect() throws IOException {
        logger.info("handler collect " + sessionId + "...");
        writer.visitDumpCommand(true, true);
        okCMD = new CountDownLatch(1);
        synchronized (okCMD) {
            try {
                okCMD.await(1000, TimeUnit.MILLISECONDS);
                logger.info("ok");
            } catch (InterruptedException e) {
                logger.info("timeout");
            }
        }
    }
}
