package org.zlab.upfuzz.fuzzingengine;

import java.io.IOException;
import java.rmi.UnexpectedException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jacoco.core.data.ExecutionDataStore;
import org.zlab.upfuzz.cassandra.CassandraExecutor;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.fuzzingengine.executor.NullExecutor;
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
		int ret = executor.execute();
		if (ret == 0) {
			collect(executor);
			// executor.collect();
		}
	}

	public ExecutionDataStore collect(Executor executor) {
		try {
			agentHandler.get(executor).collect();
			List<String> agentIdList = sessionGroup.get(executor.executorID.toString());
			if (agentIdList == null) {
				throw new UnexpectedException("No agent connection with executor " + executor.executorID.toString());
			} else {
				ExecutionDataStore execStore = new ExecutionDataStore();
				for (String agentId : agentIdList) {
					ExecutionDataStore astore = agentStore.get(agentId);
					if (astore == null) {
					} else {
						execStore.subtract(astore);
					}
				}
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
