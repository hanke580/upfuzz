package org.zlab.upfuzz.fuzzingengine.Server;

import org.zlab.upfuzz.CommandSequence;

public class CorpusEntry {
    // Write commands
    public CommandSequence originalCommandSequence;
    public CommandSequence upgradedCommandSequence;
    // Read Commands
    public CommandSequence validationCommandSequnece;

    public CorpusEntry(CommandSequence originalCommandSequence,
            CommandSequence validationCommandSequnece) {
        this.originalCommandSequence = originalCommandSequence;
        this.validationCommandSequnece = validationCommandSequnece;
    }
}
