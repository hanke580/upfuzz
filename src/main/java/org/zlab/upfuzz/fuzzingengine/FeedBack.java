package org.zlab.upfuzz.fuzzingengine;

import java.util.List;
import org.jacoco.core.data.ExecutionDataStore;

public class FeedBack {
    public ExecutionDataStore originalCodeCoverage = null;
    public ExecutionDataStore upgradedCodeCoverage = null;
    public ExecutionDataStore downgradedCodeCoverage = null;

    public FeedBack() {
        originalCodeCoverage = new ExecutionDataStore();
        upgradedCodeCoverage = new ExecutionDataStore();
        downgradedCodeCoverage = new ExecutionDataStore();
    }
}
