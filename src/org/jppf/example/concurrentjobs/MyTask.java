
package org.jppf.example.concurrentjobs;

import org.jppf.node.protocol.AbstractTask;

/**
 * A simple task used in the demo.
 */
public class MyTask extends AbstractTask<String> {
  /**
   * A string message to transform and set as result of this task.
   */
  private final String message;
  /**
   * How long this task will sleep to simulate code execution.
   */
  private final long duration;

  /**
   * Initialize this task.
   * @param message a string message to transform and set as result of this task.
   * @param duration how long this task will sleep to simulate code execution.
   */
  public MyTask(final String message, final long duration) {
    this.message = message;
    this.duration = duration;
  }

  @Override
  public void run() {
    try {
      // wait for the specified time, to simulate actual execution
      if (duration > 0) Thread.sleep(duration);
      setResult("execution success for " + message);
    } catch (Exception e) {
      setThrowable(e);
    }
  }
}
