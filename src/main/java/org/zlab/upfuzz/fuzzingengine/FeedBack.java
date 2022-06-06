package org.zlab.upfuzz.fuzzingengine;

import java.util.List;

import org.jacoco.core.data.ExecutionDataStore;

public class FeedBack {
    ExecutionDataStore originalCodeCoverage = null;
    ExecutionDataStore upgradedCodeCoverage = null;
    List<String> originalVersionResult = null;
    String testPacketID;
}
