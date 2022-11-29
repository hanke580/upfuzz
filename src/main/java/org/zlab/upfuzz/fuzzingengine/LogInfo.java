package org.zlab.upfuzz.fuzzingengine;

import java.util.LinkedList;
import java.util.List;

public class LogInfo {
    public List<String> ERRORMsg = new LinkedList<>();
    public List<String> WARNMsg = new LinkedList<>();

    public LogInfo() {
    }

    public List<String> getErrorMsg() {
        return ERRORMsg;
    }

    public List<String> getWARNMsg() {
        return WARNMsg;
    }

    public void addErrorMsg(String msg) {
        ERRORMsg.add(msg);
    }

    public void addWARNMsg(String msg) {
        WARNMsg.add(msg);
    }

}
