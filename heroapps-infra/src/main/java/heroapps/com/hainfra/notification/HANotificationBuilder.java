package heroapps.com.hainfra.notification;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Refael Ozeri on 7/27/15.
 */
public class HANotificationBuilder {

  // A fixed dedicated ID sets only 1 notification per app.
  // if you want several pushes that won't override one another
  // create a random id every time
  //public static final int NOTIFICATION_ID = randomId();

  // notification manager
  private NotificationManager mNotificationManager;

  public void sendNotification(String appName, HANotification notification, Context ctx, Class<? extends Activity> intentActivity, int icon) {
    sendNotification(appName, notification, ctx, intentActivity, new ArrayList<HANotificationAction>(), null, icon, randomId());
  }

  public void sendNotification(String appName, HANotification notification, Context ctx, Class<? extends Activity> intentActivity, @Nullable ArrayList<HANotificationAction> actions, Uri alarmSound, int icon, int id) {

    if (alarmSound == null) {
      alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    }

    mNotificationManager = (NotificationManager)
        ctx.getSystemService(Context.NOTIFICATION_SERVICE);

    Intent intent = new Intent(ctx, intentActivity);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

    PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    /**
     * "\u200E\u200E\u200E" is Unicode char that sets the text alignment from the left!
     */

    NotificationCompat.Builder mBuilder =
        new NotificationCompat.Builder(ctx)
            .setSmallIcon(icon)
            .setContentTitle(appName /*ctx.getResources().getString(R.string.app_name)*/)
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText("‎‎‎\u200E\u200E\u200E‎‎‎‎‎‎‎‎‎‎‎‎‎‎‎‎‎‎‎‎‎‎‎‎" + notification.getExpandText()))
            .setTicker("\u200E\u200E\u200E" + notification.getTicker())
            .setAutoCancel(true)
            //.setOngoing(true)
            .setSound(alarmSound)
            .setContentText("\u200E\u200E\u200E" + notification.getCollapsedText())
            .setPriority(NotificationCompat.PRIORITY_MAX);
    //.setVibrate(new long[0]);

    if (actions != null) {
      for (int i = 0; i < actions.size(); i++) {
        mBuilder.addAction(actions.get(i).getIcon(), actions.get(i).getTitle(), actions.get(i).getPendingIntent());
      }
    }

//        int NOTIFICATION_ID = randomId();

    mBuilder.setContentIntent(contentIntent);
    mNotificationManager.notify(id, mBuilder.build());
  }

  /** generates random notification id from current date, use when need random id */
  private int randomId() {
    long time = new Date().getTime();
    String tmpStr = String.valueOf(time);
    String last4Str = tmpStr.substring(tmpStr.length() - 5);

    return Integer.valueOf(last4Str);
  }

}