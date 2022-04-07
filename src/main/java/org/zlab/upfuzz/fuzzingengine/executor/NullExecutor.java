package org.zlab.upfuzz.fuzzingengine.executor;

import java.util.List;

import org.zlab.upfuzz.CommandSequence;

/* NullExecutor
 * DO NOTHING
 * */
public class NullExecutor extends Executor {
  public NullExecutor(CommandSequence cmdSeq) {
    super(cmdSeq);
  }

  @Override
  public List<String>  execute(CommandSequence commandSequence, CommandSequence validationCommandSequence) {
    return null;
  }

  @Override
  public int upgradeTest(CommandSequence validationCommandSequence, List<String> oldVersionResult) {
    return 0;
  }
}
