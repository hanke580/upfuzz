package org.zlab.upfuzz.fuzzingengine;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.ExecutionDataWriter;
import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.CustomExceptions;
import org.zlab.upfuzz.cassandra.CassandraExecutor;
import org.zlab.upfuzz.fuzzingengine.executor.Executor;
import org.zlab.upfuzz.utils.Pair;
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

	public static int crashID;


	FuzzingClient() {
        init();
	}

	private void init() {
		// TODO: GC the old coverage since we already get the overall coverage.
		agentStore = new HashMap<>();
		agentHandler = new HashMap<>();
		sessionGroup = new HashMap<>();
		crashID = 0;
		try {
			clientSocket = new ClientSocket(this);
			clientSocket.setDaemon(true);
			clientSocket.start();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public ExecutionDataStore start(CommandSequence commandSequence, CommandSequence validationCommandSequence) {

		try {
			System.out.println("Main Class Name: " + Utilities.getMainClassName());
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Executor executor = new CassandraExecutor(commandSequence, validationCommandSequence);
		List<String> oldVersionResult = null;
		ExecutionDataStore codeCoverage = null;
		try {
			oldVersionResult = executor.execute();
			if (oldVersionResult != null) {
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

		// 1. Upgrade check
		// 2. Read sequence check
		try {
			boolean ret = executor.upgradeTest();
			if (ret == false) {
				/**
				 * An inconsistency has been found
				 * 1. It could be exception during the upgrade process
				 * 2. The result is different between two versions
				 * Serialize them into the folder, 2 sequences + failureType + failureInfo
				*/

				while(Paths.get(Config.getConf().crashDir, "crash_" + Integer.toString(crashID) + ".report" ).toFile().exists()) {
					crashID++;
				}
				/**
				 * 1. Pair of sequences
				 * 2. String format of sequences
				 * 3. FailureType
				 * 4. FailureInfo
				 */
				Pair<CommandSequence, CommandSequence> commandSequencePair = new Pair<>(commandSequence, validationCommandSequence);
				Path commandSequencePairPath = Paths.get(Config.getConf().crashDir, "crash_" + Integer.toString(crashID) + ".ser");

				try {
					FileOutputStream fileOut =
							new FileOutputStream(commandSequencePairPath.toFile());
					ObjectOutputStream out = new ObjectOutputStream(fileOut);
					out.writeObject(commandSequencePair);
					out.close();
					fileOut.close();
				} catch (IOException i) {
					i.printStackTrace();
				}

				StringBuilder sb = new StringBuilder();
				sb.append("Failure Type: " + executor.failureType + "\n");
				sb.append("Failure Info: " + executor.failureInfo + "\n");
				sb.append("old version command list\n");
				for (String commandStr : commandSequence.getCommandStringList()) {
					sb.append(commandStr);
					sb.append("\n");
				}
				sb.append("\n\n");
				sb.append("new version command list\n");
				for (String commandStr : validationCommandSequence.getCommandStringList()) {
					sb.append(commandStr);
					sb.append("\n");
				}
				Path crashReportPath = Paths.get(Config.getConf().crashDir, "crash_" + Integer.toString(crashID) + ".report");
				try {
					FileOutputStream fileOut =
							new FileOutputStream(crashReportPath.toFile());
					ObjectOutputStream out = new ObjectOutputStream(fileOut);
					out.writeObject(sb.toString());
					out.close();
					fileOut.close();
				} catch (IOException i) {
					i.printStackTrace();
				}
				crashID++;
			}
		} catch (CustomExceptions.systemStartFailureException e) {
			System.out.println("New version cassandra start up failed, this could be a bug");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}


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
