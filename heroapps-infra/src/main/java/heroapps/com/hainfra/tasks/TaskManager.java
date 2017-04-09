package heroapps.com.hainfra.tasks;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import heroapps.com.hainfra.log.HALog;

/**
 * Created by Refael Ozeri on 09/04/2017.
 */
public class TaskManager {

  private static final int CORE_THREAD_POOL_SIZE = 5;

  private ScheduledExecutorService mExecutionService;

  private TaskManager(){
    mExecutionService = Executors.newScheduledThreadPool(CORE_THREAD_POOL_SIZE);
  }

  private static TaskManager mInstance = new TaskManager();
  public static TaskManager getInstance(){ return mInstance; }

  public void execute(Runnable task) {
    mExecutionService.execute(new TaskWrapper(task));
  }

  public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit){
    return mExecutionService.schedule(new TaskWrapper(task), delay, unit);
  }

  public ScheduledFuture<?> scheduleAtFixedRate(Runnable task,
                                                long initialDelay,
                                                long period,
                                                TimeUnit unit){
    return mExecutionService.scheduleAtFixedRate(new TaskWrapper(task), initialDelay, period, unit);
  }

  private class TaskWrapper implements Runnable {
    private Runnable mRunnable;

    public TaskWrapper(Runnable runnable){
      mRunnable = runnable;
    }

    @Override
    public void run() {
      try {
        mRunnable.run();
      } catch (Exception ex){
        HALog.e("Failed to run task: %s", ex.getMessage());
      }
    }
  }

}
