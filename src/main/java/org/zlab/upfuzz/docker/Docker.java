package org.zlab.upfuzz.docker;

import org.zlab.dinv.runtimechecker.Runtime;
import org.zlab.ocov.tracker.ObjectGraphCoverage;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.utils.Utilities;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public abstract class Docker extends DockerMeta implements IDocker {

    public abstract void chmodDir() throws IOException, InterruptedException;

    public void restart() throws Exception {
        String[] containerRecoverCMD = new String[] {
                "docker", "compose", "restart", serviceName
        };
        Process containerRecoverProcess = Utilities.exec(
                containerRecoverCMD,
                workdir);
        containerRecoverProcess.waitFor();

        // recreate connection
        start();
        logger.info(
                String.format("Node%d restart successfully!", index));
    }

    @Override
    public ObjectGraphCoverage getFormatCoverage() throws Exception {
        // execute check inv command
        Socket socket = new Socket(networkIP,
                Config.instance.formatCoveragePort);

        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println("collect format coverage"); // send a command to the server

        logger.debug("collect format coverage");
        ObjectGraphCoverage response = (ObjectGraphCoverage) in.readObject();

        logger.debug(
                "Received object coverage top object size: "
                        + response.objCoverage.keySet().size());
        // clean up resources
        out.close();
        in.close();
        socket.close();
        return response;
    }

}
