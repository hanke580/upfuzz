package org.zlab.upfuzz.fuzzingengine;

import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.UnexpectedException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.ExecutionDataWriter;
import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.CustomExceptions;
import org.zlab.upfuzz.cassandra.CassandraExecutor;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.utils.Utilities;

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


	FuzzingClient() {
        init();
	}

	private void init() {
		// TODO: GC the old coverage since we already get the overall coverage.
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

	public ExecutionDataStore start(CommandSequence commandSequence) {
		try {
			System.out.println(Utilities.getMainClassName());
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Executor executor = new CassandraExecutor(null);
		int ret;
		ExecutionDataStore codeCoverage = null;
		try {
			ret = executor.execute(commandSequence);
			if (ret == 0) {
				codeCoverage = collect(executor);
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
		} catch (CustomExceptions.systemStartFailureException e) {
			System.out.println("old version system start up failed");			

			System.exit(1);
		}

		// try {
		// 	ret = executor.upgradeTest();
		// } catch (CustomExceptions.systemStartFailureException e) {
		// 	System.out.println("New version cassandra start up failed, this could be a bug");
		// } catch (Exception e) {
		// 	e.printStackTrace();
		// 	System.exit(1);
		// }
		return codeCoverage;
	}

	public ExecutionDataStore collect(Executor executor) {
		List<String> agentIdList = sessionGroup.get(executor.executorID);
		if (agentIdList == null) {
			// new UnexpectedException("No agent connection with executor " + executor.executorID.toString())
			// 		.printStackTrace();
			return null;
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
					/**
					 * astore: Map: Classname -> int[] probes.
					 * this will merge the probe of each classes.
					 */
					execStore.merge(astore);
					System.out.println("astore size: " + execStore.getContents().size());
				}
			}
			System.out.println("size: " + execStore.getContents().size());


			// Send coverage back

			return execStore;
		}
	}

	enum FuzzingClientActions {
		start, collect;
	}
}
