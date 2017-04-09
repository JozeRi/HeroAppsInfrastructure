package heroapps.com.hainfra.events;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import heroapps.com.hainfra.tasks.ExecutorManager;

/**
 * Created by Refael Ozeri on 09/04/2017.
 */
public class EventManager {

  private static final int THREAD_POOL_SIZE = 10;
  private HashMap<Integer, List<IEventObserver>> mObservers = new HashMap<Integer, List<IEventObserver>>();
  private ExecutorManager mExecutorManager;

  private static final EventManager mInstance = new EventManager();
  private EventManager(){
    mExecutorManager = new ExecutorManager(THREAD_POOL_SIZE);
  }

  public static EventManager getInstance(){
    return mInstance;
  }

  public synchronized void subscribe(int eventType, IEventObserver observer){

    if (mObservers.containsKey(eventType) && mObservers.get(eventType).contains(observer)){
      return; //already subscribed
    }

    if (!mObservers.containsKey(eventType)){
      mObservers.put(eventType, new ArrayList<IEventObserver>());
    }

    mObservers.get(eventType).add(observer);
  }

  public synchronized void unsubscribe(int eventType, IEventObserver observer){

    if (!mObservers.containsKey(eventType) || !mObservers.get(eventType).contains(observer)){
      return;
    }

    mObservers.get(eventType).remove(observer);
  }

  public synchronized void unsubscribe(IEventObserver observer){

    for (int type : mObservers.keySet()) {
      mObservers.get(type).remove(observer);
    }
  }

  public synchronized void publish(final int eventType){
    publish(eventType, null);
  }

  public synchronized void publish(final int eventType, final Bundle data){

    if (!mObservers.containsKey(eventType)){
      return;
    }

    Iterator<IEventObserver> iterator = mObservers.get(eventType).iterator();

    while(iterator.hasNext()){
      final IEventObserver observer = iterator.next();
      mExecutorManager.execute(new Runnable() {
        @Override
        public void run() {
          observer.onEvent(eventType, data);
        }
      });
    }
  }
}
