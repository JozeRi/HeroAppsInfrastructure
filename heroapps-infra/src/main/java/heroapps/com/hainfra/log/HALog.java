package heroapps.com.hainfra.log;

import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

/**
 * Created by Refael Ozeri on 09/04/2017.
 */
public class HALog {

  private static int LEVEL = Log.ERROR;
  private static String TAG = "HeroApps-Tools";

  static public void setLevel(int level){
    LEVEL = level;
  }

  static public void d(String msgFormat, Object... args) {
    if (LEVEL <= Log.DEBUG) {

      String memMessage = String.format(" main:%b", Looper.myLooper() == Looper.getMainLooper());

//      Debug.MemoryInfo memoryInfo = new Debug.MemoryInfo();
//	    Debug.getMemoryInfo(memoryInfo);
//
//	    memMessage = String.format(
//		    " Memory: Pss=%.2f MB, Private=%.2f MB, Shared=%.2f MB ",
//		    memoryInfo.getTotalPss() / 1024.0,
//		    memoryInfo.getTotalPrivateDirty() / 1024.0,
//		    memoryInfo.getTotalSharedDirty() / 1024.0);
//
//      memMessage += String.format(" -- HeapSize=" + Debug.getNativeHeapSize() / (1024 * 1024));

      Log.d(TAG, getString(msgFormat, args) + memMessage);

    }
  }

  static public void e(Throwable t, String msgFormat, Object... args) {
    if (LEVEL <= Log.ERROR) {
      Log.e(TAG, getString(msgFormat, args), t);
    }
  }

  static public void e(String msgFormat, Object... args) {
    if (LEVEL <= Log.ERROR) {
      Log.e(TAG, getString(msgFormat, args));
    }
  }

  static public void i(String msgFormat, Object... args) {
    if (LEVEL <= Log.INFO) {
      Log.i(TAG, getString(msgFormat, args));
    }
  }

  private static String getString(String msgFormat, Object[] args) {
    if (args != null && args.length > 0) {
      return String.format(msgFormat, args);
    }
    return msgFormat;
  }

  static public void w(String msgFormat, Object... args) {
    if (LEVEL <= Log.WARN) {
      Log.w(TAG, getString(msgFormat, args));
    }
  }

  static public void v(String msgFormat, Object... args) {
    if (LEVEL <= Log.VERBOSE) {
      Log.v(TAG, getString(msgFormat, args));
    }
  }

  public static void track() {
    d(TAG, getLocation() + " <-- tracking");
  }

  public static void setTAG(String tag) {
    TAG = tag;
  }

  private static String getLocation() {
    final String className = HALog.class.getName();
    final StackTraceElement[] traces = Thread.currentThread().getStackTrace();

    String deeper = null;
    boolean found = false;
    int i = 0;
    for (StackTraceElement trace : traces) {
      i++;
      try {
        if (found) {
          if (!trace.getClassName().startsWith(className)) {
            Class<?> clazz = Class.forName(trace.getClassName());
            return "[" + getClassName(clazz) + ":" + trace.getMethodName() + "(" + deeper + "):" + trace.getLineNumber() + "]: ";
          }
        } else if (trace.getClassName().startsWith(className)) {
          found = true;

          StringBuilder sb = new StringBuilder();
          for (int j = i + 2; j < i + 7 && j < traces.length; j++) {
            sb.append(traces[j].getMethodName());
            sb.append(" / ");
          }
          deeper = sb.toString();
        }
      } catch (ClassNotFoundException e) {
      }
    }

    return Thread.currentThread().getName() + ":[]: ";
  }

  private static String getClassName(Class<?> clazz) {
    if (clazz != null) {
      if (!TextUtils.isEmpty(clazz.getSimpleName())) {
        return clazz.getSimpleName();
      }

      return getClassName(clazz.getEnclosingClass());
    }

    return "";
  }

}
