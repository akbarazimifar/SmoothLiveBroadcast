package com.rustero.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.rustero.App;
import com.rustero.R;
import com.rustero.engine.Encoder;
import com.rustero.core.effects.glEffect;
import com.rustero.gadgets.Tools;
import com.rustero.kamera.Kamman;
import com.rustero.widgets.MyActivity;
import com.rustero.widgets.PanelManager;
import com.rustero.widgets.ToggleImageButton;

import java.util.ArrayList;
import java.util.List;


public class RecordActivity extends MyActivity {

	private final int LARGE_KNOB_SIZE = 120;
	private final int SMALL_KNOB_SIZE = 80;

    private ImageButton mBeginButton, mCeaseButton, mBackgroundButton, mSwitchButton;
    private ToggleImageButton mFlashButton;
    private TextView mResoFps, mZoomInfo, mRecView;
	private TextView mDurationView, mTrafficView, mBitrateView;
    private ProgressBar mLoadingBar;
	private String mResoStr="";

    private SurfaceView mSurfView;
    private ScaleGestureDetector mScaleDetector;
    private Handler mTimerHandler;
    private int mTimerCount = 0;

	private PanelManager mPanelManager;
	private View mPanelBack;

	private ImageButton mOpenEffectPanel, mCloseEffectPanel;
	private View mEffectPanel;
	private ListView mEffectPanelList;
	private EffecstPanelAdapter mEffectsAdapter;
	private ArrayList<EffectItem> mEffectPanelItems;
	private TextView mSelectedEffects;
    private View mReconnectLayout;
    private TextView mReconnectText;


	public class EffectItem {
		public int tag, icon;
		public String name;

		// Constructor.
		public EffectItem(int tag, int icon, String name) {
			this.tag = tag;
			this.icon = icon;
			this.name = name;
		}
	}



	@Override
	protected void onResume() {
		super.onResume();
		AppService.setEventer(SERVICE_EVENTER);
		mSurfView.getHolder().addCallback(new SurfaceEventer());

		startRecordService();

		updateLook();
    }


	@Override
	protected void onPause() {
		super.onPause();
		AppService.setEventer(null);
		mLoadingBar.setVisibility(View.INVISIBLE);

		if (isFinishing()) {
            mTimerHandler.removeCallbacksAndMessages(null);
			Encoder.get().cease();
			stopRecordService();
		}
	}



	private void startRecordService() {
		Intent startIntent = new Intent(this, AppService.class);
		startIntent.setAction(App.INTENT_BEGIN_SERVICE);
		startService(startIntent);
	}



	private void stopRecordService() {
		Intent stopIntent = new Intent(this, AppService.class);
		stopIntent.setAction(App.INTENT_CEASE_SERVICE);
		startService(stopIntent);
	}



	@Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            setContentView(R.layout.record_activity);

            mBeginButton = (ImageButton) findViewById(R.id.record_pubu_start);
            mBeginButton.setOnClickListener(new BeginClicker());
            App.screenScaled(mBeginButton, LARGE_KNOB_SIZE);

            mCeaseButton = (ImageButton) findViewById(R.id.record_pubu_stop);
            mCeaseButton.setOnClickListener(new CeaseClicker());
			App.screenScaled(mCeaseButton, LARGE_KNOB_SIZE);

			mBackgroundButton = (ImageButton) findViewById(R.id.record_pubu_background);
			mBackgroundButton.setOnClickListener(new BackgroundClicker());
			App.screenScaled(mBackgroundButton, SMALL_KNOB_SIZE);

			mFlashButton = (ToggleImageButton) findViewById(R.id.record_btn_flash);
			mFlashButton.setOnCheckedChangeListener(new FlashClicker());
			App.screenScaled(mFlashButton, SMALL_KNOB_SIZE);

			mSwitchButton = (ImageButton) findViewById(R.id.record_pubu_switch);
            mSwitchButton.setOnClickListener(new SwitchClicker());
			App.screenScaled(mSwitchButton, SMALL_KNOB_SIZE);

            mRecView = (TextView) findViewById(R.id.record_out_name);
			mZoomInfo = (TextView) findViewById(R.id.record_tevi_zoom_info);
            mResoFps = (TextView) findViewById(R.id.record_tevi_reso_fps);
			mLoadingBar = (ProgressBar) findViewById(R.id.record_loading_bar);
            mSurfView = (SurfaceView) findViewById(R.id.id_port_view);
            mScaleDetector = new ScaleGestureDetector(this, new ScaleListener());

			mDurationView = (TextView) findViewById(R.id.record_duration);
			mTrafficView = (TextView) findViewById(R.id.record_traffic);
			mBitrateView = (TextView) findViewById(R.id.record_bitrate);

            mReconnectLayout = findViewById(R.id.record_reconnect_layout);
            mReconnectText =  (TextView) findViewById(R.id.record_reconnect_text);

			mPanelBack = findViewById(R.id.record_panel_back);
			mPanelManager = new PanelManager(222, mPanelBack, null);
			mEffectPanel = findViewById(R.id.panel_effects);

			mOpenEffectPanel = (ImageButton) findViewById(R.id.record_open_effect_panel);
			mOpenEffectPanel.setOnClickListener(OpenEffectClicker);
			App.screenScaled(mOpenEffectPanel, LARGE_KNOB_SIZE);

			mCloseEffectPanel = (ImageButton) findViewById(R.id.record_close_effect_panel);
			mCloseEffectPanel.setOnClickListener(ClosePanelClicker);

			mSelectedEffects = (TextView) findViewById(R.id.effects_panel_count);
			mEffectPanelItems = new ArrayList<EffectItem>();
			loadEffectList();

			mEffectPanelList = (ListView) findViewById(R.id.effects_panel_list);
			mEffectsAdapter = new EffecstPanelAdapter(this, R.layout.effect_row, mEffectPanelItems);
			mEffectPanelList.setAdapter(mEffectsAdapter);
			mEffectPanelList.setOnItemClickListener(new EffectItemClicker());

			Encoder.create();

			if (Kamman.get().getCameraCount() > 1)
				mSwitchButton.setVisibility(View.INVISIBLE);

			mTimerHandler = new Handler();
            mTimerHandler.postDelayed(timerRunnable, 999);
        } catch (Exception ex) {
            App.log( " ***_ex RecordActivity_onCreate: " + ex.getMessage());
        };
    }





	private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            mTimerCount++;
            if (1 == mTimerCount) firstTack();
            //controlsTack();
            mTimerHandler.postDelayed(this, 999);
        }
    };


    private void firstTack() {
        //App.showShortToast("firstTack");
    }




    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        ///App.log( "dispatchTouchEvent");
        mScaleDetector.onTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }






    public class ScaleListener extends  ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
//            mFactor = detector.getScaleFactor();
//            App.log( "onScale: " + mFactor);
            float prev = detector.getPreviousSpan();
            float curr = detector.getCurrentSpan();
            if (curr > prev) {
                App.log( "onScale inc");
				Encoder.get().incZoom();
            } else if (curr < prev) {
                App.log( "onScale dec");
                Encoder.get().decZoom();
            }
            String zoin = String.format("%4.1fx", Encoder.get().getZoom());
            mZoomInfo.setText(zoin);
            App.log( "onScale: " + Encoder.get().getZoom());
            return true;
        }



        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            //App.log( "onScaleBegin");
            return true;
        }



        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            //App.log( "onScaleEnd: " + Encoder.get().getZoom());
        }

    }




    private void doBegin() {
        String folder = App.makeOutputFolder();
        if (!Tools.folderExists(folder)) {
            App.showAlert(RecordActivity.this, "Something is wrong!", "The output directory does not exist!");
            return;
        }
        Encoder.get().begin();
    }



	private class BeginClicker implements View.OnClickListener {
		public void onClick(View v) {
            doBegin();
		}
	}



	private class CeaseClicker implements View.OnClickListener {

        public void onClick(View v) {
            Encoder.get().cease();
        }
    }



	private class SwitchClicker implements View.OnClickListener {

        public void onClick(View v) {
			Kamman.get().switchFrontBack();
			mResoStr = Kamman.get().getCurrentResoText();
			mResoFps.setText(mResoStr);

			stopRecordService();
			startRecordService();
        }
    }



	private class BackgroundClicker implements View.OnClickListener {
		public void onClick(View v) {
			moveTaskToBack(true);
		}
	}




	public class FlashClicker implements ToggleImageButton.OnCheckedChangeListener {
		public void onCheckedChanged(ToggleImageButton buttonView, boolean isChecked) {
			if (isChecked)
				Encoder.get().turnFlash(true);
			else
				Encoder.get().turnFlash(false);
		}
	}




	private boolean hasFlash() {
		boolean result = Encoder.get().hasFlash();
		return result;
	}




	public void updateLook() {
        if (Encoder.get().isWaiting()) {
            mReconnectLayout.setVisibility(View.VISIBLE);
            mReconnectText.setText("Waiting to reconnect: " + Encoder.get().getWaitCount());
            mBeginButton.setVisibility(View.INVISIBLE);
            mCeaseButton.setVisibility(View.INVISIBLE);
            mBackgroundButton.setVisibility(View.INVISIBLE);
            mSwitchButton.setVisibility(View.INVISIBLE);
        } else {
            mReconnectLayout.setVisibility(View.GONE);
        }

        if (Encoder.get().isIdle()) {
            mBeginButton.setVisibility(View.VISIBLE);
            mCeaseButton.setVisibility(View.INVISIBLE);
            mBackgroundButton.setVisibility(View.INVISIBLE);
            mSwitchButton.setVisibility(View.VISIBLE);
        } else {
            mBeginButton.setVisibility(View.INVISIBLE);
            mCeaseButton.setVisibility(View.VISIBLE);
            mBackgroundButton.setVisibility(View.VISIBLE);
            mSwitchButton.setVisibility(View.INVISIBLE);
        }

		if (Encoder.get().isConnecting()) {
			mLoadingBar.setVisibility(View.VISIBLE);
			mBitrateView.setText("Connecting");
		}
		else {
			mLoadingBar.setVisibility(View.INVISIBLE);
			if (Encoder.get().isConnected()) {
				mBitrateView.setText("Connected");
			} else {
				mBitrateView.setText("Not connected");
			}
		}

		mResoStr = Kamman.get().getCurrentResoText();
		mResoFps.setText(mResoStr);

		if (hasFlash()  &&  App.isPremium())
			mFlashButton.setVisibility(View.VISIBLE);
		else
			mFlashButton.setVisibility(View.GONE);
    }



	public void updateProgress() {
		if (Encoder.get().isIdle()) return;
		String text;
		Encoder.Status status = Encoder.get().getStatus();

		text = Tools.formatDuration(status.duration);
		mDurationView.setText(text);

		text = Tools.formatTraffic(status.traffic);
		mTrafficView.setText(text);

		if (status.tenRate > 0) {
				text = mResoStr + "@" + status.frameFps;
				text += "-" + status.loopFps;
				mResoFps.setText(text);
			text = Tools.formatBitrate(status.tenRate);
			text += "-" + Encoder.get().getQueueCount();
			text += "-" + status.peakTenco;
			mBitrateView.setText(text);
		}
	}




	View.OnClickListener OpenEffectClicker = new View.OnClickListener() {
		@Override
		public void onClick(final View v) {
			if (null == v)
				mPanelManager.quick = true;
			mPanelManager.openLeft(mEffectPanel);
			updateEffectsCount();
		}
	};



	View.OnClickListener ClosePanelClicker = new View.OnClickListener() {
		@Override
		public void onClick(final View v) {
			mPanelManager.clear();
		}
	};



	private void loadEffectList() {
		List<String> list = glEffect.getEffects();
		for (String name : list) {
			mEffectPanelItems.add(new EffectItem(1, R.drawable.settings_54, name));
		}
	}



	private void updateEffectsCount() {
		mEffectsAdapter.notifyDataSetChanged();
		mSelectedEffects.setText("Selected: " + App.sMyEffects.size());
	}



	private class EffectItemClicker implements ListView.OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			EffectItem item = (EffectItem) view.getTag();
			if (null == item) return;
			boolean have = App.sMyEffects.contains(item.name);
			if (have)
				App.sMyEffects.remove(item.name);
			else
				App.sMyEffects.add(item.name);
			Encoder.get().setEffects(App.sMyEffects);
			updateEffectsCount();
		}
	}






	public class EffecstPanelAdapter extends ArrayAdapter<EffectItem> {

		Context mContext;
		int layoutResourceId;
		ArrayList<EffectItem> mEffectList = null;

		public EffecstPanelAdapter(Context mContext, int layoutResourceId, ArrayList<EffectItem> data) {
			super(mContext, layoutResourceId, data);
			this.layoutResourceId = layoutResourceId;
			this.mContext = mContext;
			this.mEffectList = data;
		}


		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View itemView = convertView;
			if (itemView == null) {
				LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
				itemView = inflater.inflate(layoutResourceId, parent, false);
			}

			EffectItem item = mEffectList.get(position);
			itemView.setTag(item);

			TextView textViewName = (TextView) itemView.findViewById(R.id.effect_row_name);
			textViewName.setText(item.name);

			CheckBox cb = (CheckBox) itemView.findViewById(R.id.effect_row_check);
			if (App.sMyEffects.contains(item.name))
				cb.setChecked(true);
			else
				cb.setChecked(false);

			return itemView;
		}
	}





	// * SurfaceHolder.Callback



	private class SurfaceEventer implements  SurfaceHolder.Callback {


		@Override
		public void surfaceCreated(SurfaceHolder aHolder) {
			App.log( "surfaceCreated");
			Encoder.get().attachScreen(aHolder);
		}



		@Override
		public void surfaceChanged(SurfaceHolder aHolder, int format, int aWidth, int aHeight) {
			App.log( "surfaceChanged fmt=" + format + " size=" + aWidth + "x" + aHeight);
			Encoder.get().changeScreen(aWidth, aHeight);
		}



		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			App.log( "Surface destroyed");
			Encoder.get().detachScreen();
		}
	}







	// * Service eventer


	private AppService.Events SERVICE_EVENTER = new AppService.Events() {

		public void onEngineFault(final String aMessage) {
			RecordActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
                    updateLook();
                    App.finishActivityAlert(RecordActivity.this, "Error", aMessage);
				}
			});
		}


		public void onSessionDisconnected() {
			RecordActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
//                    showReconnection();
                      App.showAlert(RecordActivity.this, "Error!", "Session was closed!");
				}
			});
		}


		public void onEngineStatus() {
			RecordActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					updateLook();
				}
			});
		}


		public void onEngineProgress() {
			RecordActivity.this.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					updateProgress();
				}
			});
		}

	};




}
