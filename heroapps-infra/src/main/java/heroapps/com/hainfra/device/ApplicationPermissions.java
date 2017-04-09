package heroapps.com.hainfra.device;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;

/**
 * Created by Refael Ozeri on 09/04/2017.
 */
public class ApplicationPermissions {

  private ApplicationPermissions(){}

  public static boolean hasPermission(Context context, String permission){
    return (context.checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
  }

  public static boolean hasFeature(Context context, String feature) {
    return context.getPackageManager().hasSystemFeature(feature);
  }

  public static boolean haveMinimumRequirements(Context context) {
    return  hasPermission(context, Manifest.permission.INTERNET) &&
        hasPermission(context, Manifest.permission.RECEIVE_BOOT_COMPLETED);
  }

  public static ArrayList<String> getPermissionsMissing(Context context) {

    ArrayList<String> missingPermissions = new ArrayList<String>();

    if (!haveMinimumRequirements(context)){

      if (!hasPermission(context, Manifest.permission.INTERNET)) {
        missingPermissions.add(Manifest.permission.INTERNET);
      }

      if (!hasPermission(context, Manifest.permission.RECEIVE_BOOT_COMPLETED)) {
        missingPermissions.add(Manifest.permission.RECEIVE_BOOT_COMPLETED);
      }
    }

    return missingPermissions;
  }


  public static boolean hasReadCallLog(Context context) {
    if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1){
      return hasPermission(context, Manifest.permission.READ_CONTACTS);
    }

    return hasPermission(context, Manifest.permission.READ_CALL_LOG);
  }

}
