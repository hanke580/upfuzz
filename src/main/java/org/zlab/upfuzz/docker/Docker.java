package org.zlab.upfuzz.docker;

import org.zlab.dinv.runtimechecker.Runtime;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.utils.Utilities;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
    public boolean hasBrokenInv() throws Exception {
        // execute check inv command
        Socket socket = new Socket(networkIP,
                Config.instance.runtimeMonitorPort);

        ObjectOutputStream out = new ObjectOutputStream(
                socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        out.writeObject("collectInv"); // send a command to the server
        out.flush();
        logger.info("collect violation information");

        Runtime.ViolationInfo response = (Runtime.ViolationInfo) in
                .readObject(); // read the server response
        System.out.println("Received response: " + response.getMap());
        // clean up resources
        out.close();
        in.close();
        socket.close();
        return response.getMap().keySet().size() > 1;
    }

}
