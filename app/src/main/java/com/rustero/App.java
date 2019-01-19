
package com.rustero;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

//import com.google.android.gms.ads.MobileAds;
import com.facebook.FacebookSdk;
import com.facebook.LoggingBehavior;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.applinks.AppLinkData;
import com.rustero.gadgets.MimeInfo;
import com.rustero.gadgets.Tools;
import com.rustero.widgets.MyActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.support.v4.content.*;



public class App extends Application {

	public static final boolean IS_DEVEL = true;
	public static final boolean DEVEL_PREMIUM = true;

	public static final String FBLOG_POKUPKA = "pokupka";
	public static final String FBLOG_OKAY = "okay";
	public static final String FBLOG_IZMAMA = "izmama";

    public static App self;
	private static MyActivity gMyActivity = null;
	public static boolean gPremium = false;

	public static boolean fb_logging = !BuildConfig.DEBUG;
//	public static boolean fb_logging = true;

    public static AppEventsLogger sFBLogger;
	public static DisplayMetrics gDisplayMetrics;

	public static int gTargetSdkVersion = 18;
    public static boolean live;
	public static Activity lockedActivity = null;

    static public ProgressDialog gWaitDlg;
	static public List<String> sMyEffects = new ArrayList<>();

    static final public String FILM_PREFIX = "stream_";

	public static int NOTIFICATION_BAR_RECORD = 7777 + 333;
	public static String INTENT_BEGIN_SERVICE = "rustero.livestreamencoder.begin.service";
	public static String INTENT_CEASE_SERVICE = "rustero.livestreamencoder.cease.service";

	static final private String FIRST_RUN_SECONDS  = "firuse";
	static final public String PREF_NOTIFY_BITRATE = "notify_bitrate";
	static final public String PREF_RTMP_URL = "rtmp_url";
	static final public String PREF_RTMP_KEY = "rtmp_key";
	static final public String PREF_LAST_URL = "last_url";
	static final public String PREF_LAST_KEY = "last_key";



	public static boolean isDevel() {
		if (!BuildConfig.DEBUG) return false;
		return IS_DEVEL;
	}


	public static boolean isPremium() {
		if (isDevel()) return DEVEL_PREMIUM;
		if (gPremium) return true;
		return false;
	}



    @Override
    public void onCreate() {
        super.onCreate();
        self = this;

		doFirstRun();
		gDisplayMetrics = getResources().getDisplayMetrics();
		gTargetSdkVersion = getTargetSdkVersion(self);

		new MimeInfo(this);

		if (fb_logging) {
			FacebookSdk.sdkInitialize(getApplicationContext());
			AppEventsLogger.activateApp(this);
			if (BuildConfig.DEBUG) {
				FacebookSdk.setIsDebugEnabled(true);
				FacebookSdk.addLoggingBehavior(LoggingBehavior.APP_EVENTS);
			}
			sFBLogger = AppEventsLogger.newLogger(this);
		}

        //App.log(Tools.getSystemInfo());
//        if (android.os.Debug.isDebuggerConnected()) {
//            Tools.delay(999);
//        }
	}


    public static void fbLog(String aEvent) {
		if (fb_logging) {
			sFBLogger.logEvent(aEvent);
		}
    }



	private  AppLinkData.CompletionHandler FBCompletionHandler = new AppLinkData.CompletionHandler() {

		public void onDeferredAppLinkDataFetched(AppLinkData appLinkData) {

		}
	};






	public static void setActivity(MyActivity aActivity) {
		if (null == aActivity)
			gMyActivity = null;
		else
			gMyActivity = aActivity;
	}


	public static MyActivity getActivity() {
		if (null == gMyActivity)
			return null;
		else
			return gMyActivity;
	}


	public static void log(String aLine) {
		if (null == aLine)
			Log.i("rusapp", "log aLine null");
		else
			Log.i("rusapp", aLine);
	}



	public static WindowManager getWindowManager() {
		WindowManager windowManager = (WindowManager) self.getSystemService(Context.WINDOW_SERVICE);
		return windowManager;
	}


	public static int getDeviceRotation() {
		int rotation = getWindowManager().getDefaultDisplay().getRotation();
		int degrees = 0;
		switch(rotation){
			case Surface.ROTATION_0:
				degrees = 0;
				break;

			case Surface.ROTATION_90:
				degrees = 90;
				break;

			case Surface.ROTATION_180:
				degrees = 180;
				break;

			case Surface.ROTATION_270:
				degrees = 270;
				break;
		}
		return degrees;
	}


	public static DisplayMetrics getDisplayMetrics() {
		return gDisplayMetrics;
	}


	public static float getDensity() {
		return gDisplayMetrics.density;
	}


	public static int DipsToPixels(int aDips) {
		final float scale = gDisplayMetrics.density;
		int result = (int) (aDips * scale + 0.5f);
		return result;
	}


	public static void screenScaled(View aView, int a1000Size) {
		int ruler = Math.min(gDisplayMetrics.heightPixels, gDisplayMetrics.widthPixels);
		if (ruler < 400) return;
		float scale = 1.0f * ruler / 1000;
		ViewGroup.LayoutParams lapa = aView.getLayoutParams();
		lapa.width = (int) (scale * a1000Size);
		lapa.height = (int) (scale * a1000Size);
		aView.setLayoutParams(lapa);
	}


	public static void lockOrientation(Activity aActivity) {
		lockedActivity = aActivity;
		if (aActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			aActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		} else
			aActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	}


	public static void unlockOrientation() {
		if (null == lockedActivity) return;
		try {
			lockedActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		} catch (Exception ex) {
			App.log("***_ex unlockOrientation");
		}
	}


	public static String resstr(int aId) {
		return self.getResources().getText(aId).toString();
	}


	public static Bitmap resbmp(int aId) {
		Bitmap bitmap = BitmapFactory.decodeResource(self.getResources(), aId);
		return bitmap;
	}



	private void doFirstRun() {
		long firstRunSecs = getPrefLong(FIRST_RUN_SECONDS);
		if (firstRunSecs > 0) return;
		setDefaultPrefs();
		firstRunSecs = Tools.mills() / 1000;
		setPrefLong(FIRST_RUN_SECONDS, firstRunSecs);
		App.log(" * doFirstRun");
	}



	public static String readAssetFile(String aPath) {
		String result = "";
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(self.getAssets().open(aPath)));
			StringBuilder sb = new StringBuilder();
			String mLine = reader.readLine();
			while (mLine != null) {
				sb.append(mLine); // process line
				mLine = reader.readLine();
			}
			reader.close();
			result = sb.toString();
		} catch (Exception ex) {}
		return result;
	}



    static public boolean isNewbie() {
		long mins = getEverInstallSecs() / 60;
		if (mins < 120) return true;
		return false;
    }



	static public boolean want5stars() {
		if (getPrefBln("rate5done")) return false;
		if (isPremium()) return false;
		return true;
	}




	public static long getEverInstallSecs() {
		String folderPath = makeOutputFolder();
		File outFolder = new File(folderPath);
		long folderSecs = outFolder.lastModified() / 1000;

		long firstSecs = getPrefLong(FIRST_RUN_SECONDS);
		if (folderSecs < firstSecs)
			firstSecs = folderSecs;

		long result = Tools.mills()/1000 - firstSecs;
		return result;
	}



	public static long getLastInstallSecs() {
		long firstSecs = getPrefLong(FIRST_RUN_SECONDS);
		long result = Tools.mills()/1000 - firstSecs;
		return result;
	}



	static public String makeOutputFolder() {
		String result = getOutputFolderPath();
		File folder = new File(result);
		folder.mkdirs();
		if (folder.isDirectory())
			return result;

		result = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();
		return result;
	}



	static public String getOutputFolderPath() {
		String result = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();
		result += "/live_video_stream";
		return result;
	}



	public static String getImalayPath() {
		if (!isPremium()) return "";
		String result = "";
		result = self.getPrefStr("imalay_path");
		if (App.isDevel())
			result = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/logo2.png";
		return result;
	}



	public static void setImalayPath(String aPath) {
		self.setPrefStr("imalay_path", aPath);
	}



	public static boolean canImalay() {
		if (!isPremium()) return false;
		return self.getPrefBln("can_imalay");
	}


	public static void enableImalay(boolean aYes) {
		self.setPrefBln("can_imalay", aYes);
	}



	static public String getDefaultFolder() {
		String result = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();
        result += "/live_video_stream";
        return result;
    }



	public static boolean isFilming() {
		if (!isPremium()) return false;
		return self.getPrefBln("is_filming");
	}



	public static void enableFilming(boolean aYes) {
		self.setPrefBln("is_filming", aYes);
	}



	public static String takeNameCount() {
		int count = getPrefInt("film_name_count");
		count++;
		setPrefInt("film_name_count", count);
		String result = String.format("%04d", count);
		return result;
	}



	public static boolean hasFlash() {
		boolean result = self.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
		return result;
	}



    public static void showShortToast(String aText) {
        if (null == self) return;
        Toast.makeText(self, aText, Toast.LENGTH_SHORT).show();
    }



    public static void showLongToast(String aText) {
        if (null == self) return;
        Toast.makeText(self, aText, Toast.LENGTH_LONG).show();
    }



    static public void showAlert(Context aContext, String aTitle, String aText) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(aContext);
        alertDialog.setIcon(R.drawable.app_icon_96);
        alertDialog.setPositiveButton("Ok", null);
        alertDialog.setMessage(aText);
        alertDialog.setTitle(aTitle);
        alertDialog.show();
    }



    static public void finishActivityAlert(final Activity aActivity, String aTitle, String aText) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(aActivity);
        alertDialog.setIcon(R.drawable.app_icon_96);
        alertDialog.setMessage(aText);
        alertDialog.setTitle(aTitle);
        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                aActivity.finish();
            }
        });
        alertDialog.show();
    }



    static public void showWaitDlg(Context aContext, String aMessage) {
        gWaitDlg = new ProgressDialog(aContext);
        gWaitDlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        gWaitDlg.setMessage(aMessage);
        gWaitDlg.setCanceledOnTouchOutside(false);
        gWaitDlg.setCancelable(false);
        gWaitDlg.show();
    }


    static public void hideWaitDlg() {
        if (null == gWaitDlg) return;
        gWaitDlg.dismiss();
        gWaitDlg = null;
    }



	static  private void setDefaultPrefs() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(self);

		preferences.edit().putBoolean(PREF_NOTIFY_BITRATE, true).commit();

        String text = getDefaultFolder();
        preferences.edit().putString("output_folder", text).commit();

   }


	static public void setPrefStr(String aName, String aValue) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(self);
		preferences.edit().putString(aName, aValue).commit();
	}


    static public void setPrefBln(String aName, boolean aValue) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(self);
        preferences.edit().putBoolean(aName, aValue).commit();
    }


	static public void setPrefInt(String aName, int aValue) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(self);
		preferences.edit().putInt(aName, aValue).commit();
	}


	static public void setPrefLong(String aName, long aValue) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(self);
		preferences.edit().putLong(aName, aValue).commit();
	}


	static public String getPrefStr(String aName)
    {
        String result = "";
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(self);
        result = prefs.getString(aName, "");
        return result;
    }


    static public int getPrefInt(String aName)
    {
        int result = 0;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(self);
        result = prefs.getInt(aName, 0);
        return result;
    }


	static public long getPrefLong(String aName)
	{
		long result = 0;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(self);
		result = prefs.getLong(aName, 0);
		return result;
	}


	static public boolean getPrefBln(String aName)
    {
        boolean result = false;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(self);
        result = prefs.getBoolean(aName, false);
        return result;
    }
	static public boolean getPrefBlnTrue(String aName)
	{
		boolean result = false;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(self);
		result = prefs.getBoolean(aName, true);
		return result;
	}


    static public int getPrefAsInt(String aName, int aDefault)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(self);
        String text = prefs.getString(aName, ""+aDefault);
        int result = aDefault;
        try {
            result = Integer.parseInt(text);
        } catch(Exception ex) {
			result = aDefault;
		}
        return result;
    }



    public static boolean selfPermissionGranted(Context context, String permission) {
        // For Android < Android M, self permissions are always granted.
        boolean result = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (gTargetSdkVersion >= Build.VERSION_CODES.M) {
                // targetSdkVersion >= Android M, we can use Context#checkSelfPermission
                result = (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
            } else {
                // targetSdkVersion < Android M, we have to use PermissionChecker
                result = (PermissionChecker.checkSelfPermission(context, permission) == PermissionChecker.PERMISSION_GRANTED);
            }
        }
        return result;
    }



	public static int getTargetSdkVersion(Context context)
	{
		int result = 18;
		try {
			final PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			result = info.applicationInfo.targetSdkVersion;
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		return result;
	}



}
