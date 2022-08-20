package org.zlab.upfuzz.fuzzingengine;

public class SessionId {
    // FIXME think about it, maybe redundant
    public String executorId;
    public String agentId;
    public String version;

    public SessionId(String eid, String aid, String ver) {
        executorId = eid;
        agentId = aid;
        version = ver;
    }

    public String toString() {
        return executorId + "-" + agentId + "-" + version;
    }

    public static SessionId parse(String sessionId) {
        String[] sessionSplit = sessionId.split("-");
        if (sessionSplit.length != 3) {
            throw new IllegalArgumentException("Invalid SessionId " +
                    sessionId);
        }

        return new SessionId(sessionSplit[0], sessionSplit[1], sessionSplit[2]);
    }
}
