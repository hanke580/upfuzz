package org.zlab.upfuzz.docker;

import org.zlab.upfuzz.utils.Utilities;

import java.io.IOException;

public abstract class Docker extends DockerMeta implements IDocker {

    public abstract void chmodDir() throws IOException, InterruptedException;

    public void restart() throws Exception {
        String[] containerRecoverCMD = new String[] {
                "docker-compose", "restart", serviceName
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

}
