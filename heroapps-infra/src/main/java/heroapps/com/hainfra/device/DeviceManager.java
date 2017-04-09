package heroapps.com.hainfra.device;

import java.util.Observable;
import android.Manifest;
import android.accounts.Account;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Patterns;
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Observer;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import heroapps.com.hainfra.VersionUtils;
import heroapps.com.hainfra.log.HALog;
import heroapps.com.hainfra.tasks.TaskManager;

/**
 * Created by Refael Ozeri on 09/04/2017.
 */

public class DeviceManager extends Observable {

  private static final String UUID_TAG = "RNDLCLUUID";

  private DeviceManager() {}
  private static final DeviceManager mInstance = new DeviceManager();
  public static DeviceManager getInstance() { return mInstance; }

  private int mScreenWidth;
  private int mScreenHeight;
  private String mDeviceId;
  private float mDensity;
  private int mNavigationBarSize = -1;

  private static volatile boolean mInitialized = false;

  private String mAdvertisingId;
  private boolean mIsAdTrackingEnabled;
  private boolean mAdvertisingIdLookupFinished = false;
  private ScheduledFuture<?> mAdIdTimeoutFuture;

  public void initialize(Context context) {

    if (mInitialized){
      return;
    }

    mInitialized = true;

    populateAdvertiserId(context);
    populateDeviceId(context);
    populateScreenSize(context);

  }

  public boolean isReady(){
    return mAdvertisingIdLookupFinished;
  }

  public void notifyWhenReady(final Observer observer){
    if (mAdvertisingIdLookupFinished){
      TaskManager.getInstance().execute(new Runnable() {
        @Override
        public void run() {
          observer.update(null, null);
        }
      });
      return;
    }

    addObserver(observer);
  }

  private void notifyAdvertiserIdLookupFinished(){
    setChanged();
    notifyObservers();
    deleteObservers();
  }

  private void populateAdvertiserId(final Context context) {

    mAdIdTimeoutFuture = TaskManager.getInstance().schedule(new Runnable() {
      @Override
      public void run() {
        HALog.d("Timeout unique id");
        mAdvertisingIdLookupFinished = true;
        notifyAdvertiserIdLookupFinished();
      }
    }, 10, TimeUnit.SECONDS);

    TaskManager.getInstance().execute(new Runnable() {
      @Override
      public void run() {

        //long time = System.currentTimeMillis();
        HALog.d("Searching for advertiser id");

        try {
          Class<?> clazzClient = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
          Class<?> clazzInfo = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient$Info");

          Method m = clazzClient.getMethod("getAdvertisingIdInfo", Context.class);
          Object o = m.invoke(null, context);
          Object cast = clazzInfo.cast(o);
          Method getId = cast.getClass().getMethod("getId");
          Method isLimitAdTrackingEnabled = cast.getClass().getMethod("isLimitAdTrackingEnabled");

          mAdvertisingId = (String) getId.invoke(cast);
          mIsAdTrackingEnabled = !(Boolean) isLimitAdTrackingEnabled.invoke(cast);

          HALog.d("Advertiser id found");

        } catch (Exception e) {
          HALog.d("Advertiser id not found: %s", e.getMessage());
        } finally {
          //WLog.d("XXX Finished searching for adid in %d ms", (System.currentTimeMillis() - time)));
          mAdvertisingIdLookupFinished = true;
          notifyAdvertiserIdLookupFinished();
          if (mAdIdTimeoutFuture != null) {
            mAdIdTimeoutFuture.cancel(true);
            mAdIdTimeoutFuture = null;
          }
        }
      }
    });
  }

  private void populateDeviceId(Context context) {

    mDeviceId = getAndroidId(context);

    if (TextUtils.isEmpty(mDeviceId)) {
      mDeviceId = getSerial(context);
    }

    if (TextUtils.isEmpty(mDeviceId)) {
      mDeviceId = getIMEI(context);
    }

    if (TextUtils.isEmpty(mDeviceId)){
      mDeviceId = getWifiMACIfOn(context);
    }
  }

  private void populateScreenSize(Context context) {
    DisplayMetrics dm = context.getResources().getDisplayMetrics();
    WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

    Display display = windowManager.getDefaultDisplay();
    mScreenWidth = dm.widthPixels;
    mScreenHeight = dm.heightPixels;
    mDensity = dm.density;

    if (Build.VERSION.SDK_INT >= 14 && Build.VERSION.SDK_INT < 17) {
      try {
        mScreenWidth = (Integer) Display.class.getMethod("getRawWidth").invoke(display);
        mScreenHeight = (Integer) Display.class.getMethod("getRawHeight").invoke(display);
      } catch (Exception ignored) {
      }
    }

    // includes window decorations (statusbar bar/menu bar)
    if (Build.VERSION.SDK_INT >= 17) {
      try {
        Point realSize = new Point();
        Display.class.getMethod("getRealSize", Point.class).invoke(display, realSize);
        mScreenWidth = realSize.x;
        mScreenHeight = realSize.y;
      } catch (Exception ignored) {
      }
    }
  }

  public String getResolution() { return mScreenWidth + "X" + mScreenHeight; }

  public int getScreenWidth() {
    return mScreenWidth;
  }

  public int getScreenHeight() {
    return mScreenHeight;
  }

  public int getNavigationBarSize(Context context){
    if(mNavigationBarSize != -1){
      return mNavigationBarSize;
    }
    Resources resources = context.getResources();
    int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
    if (resourceId > 0) {
      mNavigationBarSize = resources.getDimensionPixelSize(resourceId);
    } else {
      mNavigationBarSize = 0;
    }
    return mNavigationBarSize;
  }

  public String getDeviceId() {
    return mDeviceId;
  }

  @SuppressLint("NewApi")
  public static boolean hasNavigationBar(Context context){
    boolean res = false;
    if(VersionUtils.hasIceCreamSandwich()) {
      boolean hasMenuKey = ViewConfiguration.get(context).hasPermanentMenuKey();
      boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);

      res = !hasMenuKey && !hasBackKey;
    }
    if(!res && VersionUtils.hasJellyBeanMR1()){
      WindowManager windowManager = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE));
      Display display = windowManager.getDefaultDisplay();
      Point realSize = new Point();
      Point size = new Point();
      display.getRealSize(realSize);
      display.getSize(size);

      res = realSize.x != size.x || realSize.y != size.y;
    }
    return res;
  }

  public static Account[] getAllAccounts(Context context){
    //TODO - Edit to support android M permissions.
//    if (!ApplicationPermissions.hasPermission(context, Manifest.permission.GET_ACCOUNTS)){
//      RLog.i("No permission to get accounts");
//      return new Account[0];
//    }
//
//    Account[] accounts = AccountManager.get(context).getAccounts();
//
//    return accounts;

    return new Account[0];
  }

  public static List<String> getEmailAccounts(Context context) {
    Pattern emailPattern = Patterns.EMAIL_ADDRESS;
    List<String> emails = new ArrayList<String>();
    Account[] accounts = getAllAccounts(context);
    if (accounts == null){
      return emails;
    }

    for (Account account : accounts) {
      if (emailPattern.matcher(account.name).matches()) {
        emails.add(account.name);
      }
    }

    return emails;
  }

  public static String getGmailAccount(Context context){
    String gmailTag = "gmail";
    Pattern emailPattern = Patterns.EMAIL_ADDRESS;
    Account[] accounts = getAllAccounts(context);
    for (Account account : accounts) {
      if (emailPattern.matcher(account.name).matches()) {
        if (account.name.contains(gmailTag)){
          return account.name;
        }
      }
    }

    return null;
  }

  public static String getWifiMACIfOn(Context context) {
    //TODO - Edit to support android M permissions.
//    if (!ApplicationPermissions.hasPermission(context, Manifest.permission.ACCESS_WIFI_STATE)){
//      RLog.i("No permission to access wifi state");
//      return "";
//    }
//
//    WifiManager wifiManager = getWifiManager(context);
//    if (wifiManager != null){
//      WifiInfo wifiInf = wifiManager.getConnectionInfo();
//      return wifiInf.getMacAddress();
//    }

    return "";
  }

  public static String getIMEI(Context context) {
    TelephonyManager telephonyManager = getTelephonyManager(context);
    if (telephonyManager != null) {
      return telephonyManager.getDeviceId();
    }

    return "";
  }

  public float getDensity() {
    return mDensity;
  }

  public String getDeviceLanguage(){
    return Locale.getDefault().getDisplayLanguage();
  }

  public static String getNetworkCountryCode(Context context){
    TelephonyManager telephonyManager = DeviceManager.getTelephonyManager(context);
    if (telephonyManager == null){
      return "";
    }

    if (telephonyManager.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) {
      return telephonyManager.getNetworkCountryIso();
    }

    return "";
  }

  /**
   * Gets device country code
   * @param context
   * @return the country code ISO for the device according to the language that is set on the device,
   * This won't work in countries for which Android does not have a Locale. For example,
   * in Switzerland, the language is likely to be set to German or French.
   * This method will give you Germany or France, not Switzerland
   */
  public static String getDeviceCountryCode(Context context){
    return context.getResources().getConfiguration().locale.getCountry();
  }

  public static TelephonyManager getTelephonyManager(Context context){

    if (!ApplicationPermissions.hasPermission(context, Manifest.permission.READ_PHONE_STATE)){
      HALog.i("No permission to read telephony info");
      return null;
    }

    return (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
  }

  public static WifiManager getWifiManager(Context context){

    if (!ApplicationPermissions.hasPermission(context, Manifest.permission.ACCESS_WIFI_STATE)) {
      HALog.i("No permission to read wifi info");
      return null;
    }

    return (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
  }

  @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  public static String getOwnerName(Context context){
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH ||
        !ApplicationPermissions.hasPermission(context, Manifest.permission.READ_CONTACTS)/* ||
        !ApplicationPermissions.hasPermission(context, Manifest.permission.READ_PROFILE)*/){
      return null;
    }

    String ownerName = null;
    Cursor cursor = null;

    try {
      cursor = context.getContentResolver().query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);
      if (cursor != null && cursor.getCount() > 0){
        cursor.moveToFirst();
        ownerName = cursor.getString(cursor.getColumnIndex("display_name"));
      }

    }catch (Exception e){
      HALog.e("Failed to get owner info");
    }finally {
      try { if (cursor != null){ cursor.close(); } }catch (Exception ignore){}
    }

    return ownerName;
  }

  public static boolean isScreenOn(Context context){
    PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    return powerManager.isScreenOn();
  }

  public static void setScreenBrightness(Window window, float value){
    if (window == null){
      return;
    }

    WindowManager.LayoutParams lp = window.getAttributes();
    lp.screenBrightness = value;
    window.setAttributes(lp);
  }

  public static float getScreenBrightness(Context context) throws Settings.SettingNotFoundException {
    return android.provider.Settings.System.getFloat(context.getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS);
  }

  public static void setScreenOffTimeout(Context context, int value){
    android.provider.Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, value);
  }

  public static int getScreenOffTimeout(Context context, int defaultValue){
    return Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, defaultValue);
  }

  public int convertDPtoPixels(int dp) {
    return Math.round((float) dp * mDensity);
  }

  public static void showKeyboard(Context context) {
    InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT , 0);
  }

  public static void hideKeyboard(Context context, View view){
    InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
  }

  public static String getSerial(Context context){
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
      return Build.SERIAL;
    }

    return "";
  }

  public static String getAndroidId(Context context) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
      return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    return "";
  }

  public static boolean isNetworkAvailable(Context context) {
    ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
  }

  public static boolean getLockscreenSoundEnabled(Context context, boolean defaultValue) {
    return Settings.System.getInt(context.getContentResolver(), "lockscreen_sounds_enabled", defaultValue ? 1 : 0) == 1;
  }

  public static void setLockscreenSoundEnabled(Context context, boolean value){
    Settings.System.putInt(context.getContentResolver(), "lockscreen_sounds_enabled", value ? 1 : 0);
  }

  public String getAdvertisingId() {
    return mAdvertisingId;
  }

  public boolean isAdTrackingEnabled() {
    return mIsAdTrackingEnabled;
  }

  public static String getRandomLocalDeviceId(Context context) {

//    SharedPreferences prefs = context.getSharedPreferences("brains." + context.getPackageName(), Context.MODE_PRIVATE);
//    String uuid = prefs.getString(UUID_TAG, null);
//
//    if (TextUtils.isEmpty(uuid)){
//      uuid = UUID.randomUUID().toString();
//      SharedPreferences.Editor editor = prefs.edit();
//      editor.putString(UUID_TAG, uuid);
//      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
//        editor.commit();
//      } else {
//        editor.apply();
//      }
//    }
//
    return "empty";
  }

  public static boolean isConnectedToPower(Context context) {
    Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
    return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB;
  }

}
