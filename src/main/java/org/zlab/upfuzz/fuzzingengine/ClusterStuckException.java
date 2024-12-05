package org.zlab.upfuzz.fuzzingengine;

public class ClusterStuckException extends RuntimeException {
    public ClusterStuckException(String message) {
        super(message);
    }

    public ClusterStuckException(String message, Throwable cause) {
        super(message, cause);
    }
}
