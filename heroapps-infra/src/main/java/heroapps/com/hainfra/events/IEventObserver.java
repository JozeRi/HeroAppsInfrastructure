package heroapps.com.hainfra.events;

import android.os.Bundle;

/**
 * Created by Refael Ozeri on 09/04/2017.
 */
public interface IEventObserver {
  void onEvent(int eventType, Bundle payload);
}
