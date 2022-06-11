package org.zlab.upfuzz.fuzzingengine.Server;

import org.zlab.upfuzz.CommandSequence;
import org.zlab.upfuzz.fuzzingengine.Packet.StackedTestPacket;

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

    public CorpusEntry mutate() {
        return null;
    }

    public StackedTestPacket toPacket() {
        return null;
    }
}
