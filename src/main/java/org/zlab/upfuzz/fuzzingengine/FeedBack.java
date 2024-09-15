package org.zlab.upfuzz.fuzzingengine;

import org.jacoco.core.data.ExecutionDataStore;

public class FeedBack {
    public ExecutionDataStore originalCodeCoverage;
    public ExecutionDataStore upgradedCodeCoverage;
    public ExecutionDataStore downgradedCodeCoverage;

    public FeedBack() {
        originalCodeCoverage = new ExecutionDataStore();
        upgradedCodeCoverage = new ExecutionDataStore();
        downgradedCodeCoverage = new ExecutionDataStore();
    }
}
