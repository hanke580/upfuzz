package org.zlab.upfuzz.fuzzingengine;

import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.UnexpectedException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.ExecutionDataWriter;
import org.zlab.upfuzz.cassandra.CassandraExecutor;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.utils.SystemUtil;

public class FuzzingClient {

	/**
	 * key: String -> agentId
	 * value: Codecoverage for this agent
	 */
	public Map<String, ExecutionDataStore> agentStore;

	/* key: String -> agent Id
	 * value: ClientHandler -> the socket to a agent */
	public Map<String, ClientHandler> agentHandler;

	/* key: UUID String -> executor Id
	 * value: List<String> -> list of all alive agents with the executor Id */
	public Map<String, List<String>> sessionGroup;

	/* socket for client and agents to communicate*/
	public ClientSocket clientSocket;

	public final Config conf;

	FuzzingClient(Config conf) {
		this.conf = conf;
	}

	private void init() {
		agentStore = new HashMap<>();
		agentHandler = new HashMap<>();
		sessionGroup = new HashMap<>();
		try {
			clientSocket = new ClientSocket(this);
			clientSocket.setDaemon(true);
			clientSocket.start();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void start() {
		try {
			System.out.println(SystemUtil.getMainClassName());
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		init();
		Executor executor = new CassandraExecutor(conf, null);
		executor.startup();
		// collect(executor);
		// try {
		// 	Thread.sleep(5000);
		// } catch (InterruptedException e1) {
		// 	e1.printStackTrace();
		// }
		executor.teardown();
		int ret = 0;
		// int ret = executor.execute();
		if (ret == 0) {
			ExecutionDataStore codeCoverage = collect(executor);
			String destFile = executor.getSysExecID() + ".exec";
			try {
				FileOutputStream localFile = new FileOutputStream(destFile);
				ExecutionDataWriter localWriter = new ExecutionDataWriter(localFile);
				codeCoverage.accept(localWriter);
				// localWriter.visitClassExecution(codeCoverage);
				System.out.println("write codecoverage to " + destFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public ExecutionDataStore collect(Executor executor) {
		try {
			List<String> agentIdList = sessionGroup.get(executor.executorID);
			if (agentIdList == null) {
				throw new UnexpectedException("No agent connection with executor " + executor.executorID.toString());
			} else {
				// for (String agentId : agentIdList) {
				// 	System.out.println("collect conn " + agentId);
				// 	ClientHandler conn = agentHandler.get(agentId);
				// 	if (conn != null) {
				// 		conn.collect();
				// 	}
				// }
				ExecutionDataStore execStore = new ExecutionDataStore();
				for (String agentId : agentIdList) {
					System.out.println("get coverage from " + agentId);
					ExecutionDataStore astore = agentStore.get(agentId);
					if (astore == null) {
						System.out.println("no data");
					} else {
						execStore.merge(astore);
						System.out.println("astore size: " + execStore.getContents().size());
					}
				}
				System.out.println("size: " + execStore.getContents().size());
				return execStore;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	enum FuzzingClientActions {
		start, collect;
	}
}
