package org.zlab.upfuzz.fuzzingengine.executor;

import java.util.List;

import org.zlab.upfuzz.CommandSequence;

/* NullExecutor
 * DO NOTHING
 * */
public class NullExecutor extends Executor {
  public NullExecutor() {
    super();
  }

  @Override
  public void startup() {

  }

  @Override
  public void teardown() {

  }

  @Override
  public List<String> execute(CommandSequence commandSequence,
      CommandSequence validationCommandSequence, int testId) {
    return null;
  }

  @Override
  public int saveSnapshot() {
    return 0;
  }

  @Override
  public int moveSnapShot() {
    return 0;
  }

  @Override
  public boolean upgradeTest() {
    return true;
  }

  @Override
  public List<String> executeCommands(CommandSequence commandSequence) {
    return null;
  }

  @Override
  public void upgrade() throws Exception {
  }
}
