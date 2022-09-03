package org.zlab.upfuzz.fuzzingengine;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jacoco.core.data.ExecutionDataWriter;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;

public class AgentServerSocket extends Thread {
    static Logger logger = LogManager.getLogger(AgentServerSocket.class);

    final Executor executor;
    final ServerSocket server;
    final ExecutionDataWriter fileWriter;

    public AgentServerSocket(Executor executor)
            throws UnknownHostException, IOException {
        this.executor = executor;
        this.server = new ServerSocket(0, 0, InetAddress.getByName("0.0.0.0"));
        logger.info("Executor: " + executor.executorID
                + "  Client socket Server start at: " +
                this.server.getLocalSocketAddress());
        this.fileWriter = new ExecutionDataWriter(
                new FileOutputStream("./zlab-jacoco.exec"));
    }

    @Override
    public void run() {
        AgentServerHandler handler;
        while (true) {
            try {
                handler = new AgentServerHandler(executor, server.accept(),
                        fileWriter);
                new Thread(handler).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public int getPort() {
        logger.debug(server.getLocalSocketAddress());
        logger.debug(server.getLocalPort());
        return server.getLocalPort();
    }
}
