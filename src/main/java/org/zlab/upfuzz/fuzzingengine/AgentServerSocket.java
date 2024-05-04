package org.zlab.upfuzz.fuzzingengine;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jacoco.core.data.ExecutionDataWriter;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;

public class AgentServerSocket extends Thread {
    static Logger logger = LogManager.getLogger(AgentServerSocket.class);

    private final Executor executor;
    private final ServerSocket server;
    private final ExecutionDataWriter fileWriter;
    private volatile boolean running = true;

    public AgentServerSocket(Executor executor) throws IOException {
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
        while (running) {
            try {
                Socket clientSocket = server.accept();
                AgentServerHandler handler = new AgentServerHandler(executor,
                        clientSocket, fileWriter);
                new Thread(handler).start();
            } catch (IOException e) {
                if (!running) { // Check if the server is supposed to be
                                // stopping
                    logger.info("Server is stopping.");
                    break;
                }
                logger.error("Error accepting client connection", e);
            }
        }
    }

    public int getPort() {
        return server.getLocalPort();
    }

    public void stopServer() {
        running = false;
        try {
            if (server != null && !server.isClosed()) {
                server.close(); // This will cause server.accept() to throw
                                // SocketException
            }
        } catch (IOException e) {
            logger.error("Failed to close server socket", e);
        }
    }
}
