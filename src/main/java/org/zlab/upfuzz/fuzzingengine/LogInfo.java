package org.zlab.upfuzz.fuzzingengine;

import java.util.LinkedList;
import java.util.List;

public class LogInfo {
    List<String> ERRORMsg = new LinkedList<>();
    List<String> WARNMsg = new LinkedList<>();

    public LogInfo() {
    }

    public List<String> getErrorMsg() {
        return ERRORMsg;
    }

    public List<String> getWARNMsg() {
        return ERRORMsg;
    }

    public void addErrorMsg(String msg) {
        ERRORMsg.add(msg);
    }

    public void addWARNMsg(String msg) {
        ERRORMsg.add(msg);
    }

}
