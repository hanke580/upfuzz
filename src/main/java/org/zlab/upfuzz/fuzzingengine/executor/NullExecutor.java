package org.zlab.upfuzz.fuzzingengine.executor;

import java.util.List;
import org.zlab.upfuzz.Command;
import org.zlab.upfuzz.fuzzingengine.Config;
import org.zlab.upfuzz.CommandSequence;

/* NullExecutor
 * DO NOTHING
 * */
public class NullExecutor extends Executor {
  public NullExecutor(Config conf, CommandSequence cmdSeq) {
    super(conf, cmdSeq);
  }

  @Override
  public int execute(CommandSequence commandSequence) {
    return 0;
  }
}
