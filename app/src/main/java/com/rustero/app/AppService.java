package com.rustero.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.text.Html;

import com.rustero.App;
import com.rustero.R;
import com.rustero.engine.Encoder;
import com.rustero.gadgets.Tools;

import java.util.LinkedList;


public class AppService extends Service {



	public interface Events {
		void onEngineFault(final String aMessage);
		void onSessionDisconnected();
		void onEngineStatus();
		void onEngineProgress();
	}




	public static boolean running  = false;
	private static Events mEventer;
	private final Object mLock = new Object();
	private final LinkedList<Runnable> mCalls = new LinkedList<Runnable>();
    private Handler mAppHandler;


	public AppService() {}



	public static void setEventer(Events aEventer) {
		mEventer = aEventer;
	}


    public void onCreate() {
        super.onCreate();
        mAppHandler = new Handler(Looper.getMainLooper());
    }


    @Override
	public IBinder onBind(Intent intent) {
		// Used only in case of bound services.
		return null;
	}


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (null == intent) return START_STICKY;
		if (intent.getAction().equals(App.INTENT_BEGIN_SERVICE)) {
			doStartService();
		} else if (intent.getAction().equals(App.INTENT_CEASE_SERVICE)) {
			doStopService();
		}
		return START_STICKY;
	}



	private void doStartService() {
		App.log("doStartService");
		Encoder.get().attachEngine(ENGINE_EVENTER);
		startForeground(App.NOTIFICATION_BAR_RECORD, buildNotification("Recording..."));
		running = true;
	}



	private void doStopService() {
		App.log("doStopService");
		running = false;
		Encoder.get().detachEngine();
		stopForeground(true);
		stopSelf();
	}



	private Notification buildNotification(String aText) {
		Intent notificationIntent = new Intent(this, RecordActivity.class);
		notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.app_icon_96);

		String title = "" + Html.fromHtml("<b>" + App.resstr(R.string.app_name) + "</b>");
		Notification notification = new NotificationCompat.Builder(this)
				.setTicker(title)
				.setContentTitle(title)
				.setContentText(aText)
				.setSmallIcon(R.drawable.app_icon_96)
				.setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
				.setContentIntent(pendingIntent)
				.build();

		return notification;
	}



	private void updateNotification(String text) {
		if (!running) return;
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		manager.notify(App.NOTIFICATION_BAR_RECORD, buildNotification(text));
	}






	private Encoder.Events ENGINE_EVENTER = new Encoder.Events() {


		public void onFault(final String aMessage)
		{
			updateNotification("Recording error");
			try {
				if (null != mEventer)
					mEventer.onEngineFault(aMessage);
			} catch(Exception e) {}
		}


		public void onDisconnected()
		{
			updateNotification("Session is closed.");
			try {
				if (null != mEventer) {
					mEventer.onEngineStatus();
					mEventer.onSessionDisconnected();
				}
			} catch(Exception e) {}
		}


		public void onStatus() {
			if (Encoder.get().isIdle())
				updateNotification("Ready to stream");
            else if (Encoder.get().isConnecting())
                updateNotification("Connecting...");
            else if (Encoder.get().isConnecting())
                updateNotification("Connected");
            else if (Encoder.get().isWaiting())
                updateNotification("Waiting to reconnect: " + Encoder.get().getWaitCount());

			try {
				if (null != mEventer)
					mEventer.onEngineStatus();
			} catch(Exception e) {}
		}


		public void onProgress() {
			Encoder.Status status = Encoder.get().getStatus();
			String text = Tools.formatDuration(status.duration);
			text += "  " + Tools.formatTraffic(status.traffic);
            if (status.tenRate > 0)
    			text += "  " + Tools.formatBitrate(status.tenRate);
			updateNotification(text);

			try {
				if (null != mEventer)
					mEventer.onEngineProgress();
			} catch(Exception e) {}
		}


	};



}
