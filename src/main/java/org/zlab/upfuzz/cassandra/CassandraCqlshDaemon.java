package org.zlab.upfuzz.cassandra;

import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

import com.google.gson.Gson;

import org.apache.commons.lang3.RandomUtils;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.utils.SystemUtil;

public class CassandraCqlshDaemon {
    private int port;
    private int MAX_RETRY = 100;
    private Process cqlsh;
    private Socket socket;

    public CassandraCqlshDaemon() throws IOException, InterruptedException {
        boolean flag = false;
        flag = true;
        for (int i = 0; i < MAX_RETRY; ++i) {
            // port = RandomUtils.nextInt(1024, 65536);
            port = 55555;

            if (testPortAvailable(port)) {
                flag = true;
                break;
            }
        }
        if (flag) {
            // port = port & 0xFFFE;
            System.out.println("Use port:" + port);
            cqlsh = SystemUtil.exec(new String[] { "python", Config.getConf().cqlshDaemonScript, "--port=" + Integer.toString(port) },
                    new File(Config.getConf().cassandraPath));

            byte[] output = new byte[10240];
            
            cqlsh.getInputStream().read(output);
            System.out.println("CQLSH OUTPUTING...");
            System.out.println(new String(output));

            Thread.sleep(1000);
            socket = new Socket("localhost", port);
        } else {
            throw new IllegalStateException("Cannot fina an available port");
        }
    }

    public CqlshPacket execute(String cmd) throws IOException {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        bw.write(cmd);
        bw.flush();
        System.out.println("executor write " + cmd);
        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        // String cqlshMess = br.readLine();
        // byte[] bytes = new byte[10240];
        // int cnt = socket.getInputStream().read(bytes);
        char[] chars = new char[10240];

        int cnt = br.read(chars);
        String cqlshMess = new String(chars, 0, cnt);

        System.out.println("receive size: " + cqlshMess.length() + " \n" + cqlshMess);

        CqlshPacket cqlshPacket = new Gson().fromJson(cqlshMess, CqlshPacket.class);
        return cqlshPacket;
    }

    public static boolean testPortAvailable(int port) {
        Process p;
        try {
            p = SystemUtil.exec(new String[] { "bin/sh", "-c", "netstat -tunlp | grep -P \":" + port + "[\\s$]\"" },
                    new File("/"));
            int ret = p.waitFor();
            return ret == 1;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static class CqlshPacket {
        public String cmd;
        public int exitValue;
        public String message;
        public double timeUsage;

        public CqlshPacket() {
            cmd = "";
            exitValue = 0;
            message = "";
            timeUsage = -1;
        }
    }
}
