package com.rustero.app;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.rustero.App;
import com.rustero.engine.Encoder;
import com.rustero.BuildConfig;
import com.rustero.gadgets.Connectivity;
import com.rustero.gadgets.Tools;
import com.rustero.kamera.Kamman;
import com.rustero.widgets.MyActivity;
import com.rustero.widgets.PanelManager;

import java.util.ArrayList;
import java.util.List;
import com.rustero.R;


@SuppressWarnings("ResourceType")



public class MainActivity extends MyActivity {


//	public static final String TEST_LINK = "";
	public static MainActivity self;

	private final int ACT_RESULT_RECORD 	= 12;
	//private final int MAME_ITEM_SETTINGS 	= 1;
	private final int MAME_ITEM_ABOUT 		= 2;
	private final int MAME_ITEM_ENCOURAGE 	= 3;
	private final int MAME_ITEM_INVITE 		= 4;
	private final int MAME_ITEM_MENTION 	= 5;
//	private final int MAME_ITEM_BUY	 		= 11;

	private View mTestView;
	private PanelManager mPanelManager;
	private View mLeftPanel;
	private ImageButton mOpenPanel, mClosePanel;
	private ListView mPanelList;
	private ArrayList<MameItem> mMameItems;

	private TextView mTitle;
	private View mSigninStrip, mRtmpedStrip;

	private ActionBar mActBar;
	private Toolbar mToolbar;

	private Button mSetupUrlButton, mGoliveButton, mResetUrlButton;
	private Button mTwitchDashboard, mTwitchGuide;
	private Button mYoutubeDashboard, mYoutubeGuide;

	private View mFrontLayout, mBackLayout;
	private Spinner mFrontSpinner, mBackSpinner;

//	private View mFilmingLayout;
//	private Switch mFilmingSwitch;

	private Handler mTackHandler;
	private boolean mTacked;

	private String[] mFrontResols = new String[]{"640x480"};
	private String[] mBackResols = new String[]{"640x480"};




	public static MainActivity get() {
		return self;
	}



	@Override
	public void onResume() {
		super.onResume();
		updateLook();

//		if (App.isPremium())
//			mFilmingLayout.setVisibility(View.VISIBLE);
//		else
//			mFilmingLayout.setVisibility(View.GONE);

		if (!BuildConfig.DEBUG) {
			String msg = "";
			if (Encoder.ACTOR_ID > 0)
				msg = "Actor build!";
			if (!msg.isEmpty()) {
				App.finishActivityAlert(this, "Error!", msg);
			}
		}
	}



	@Override
	protected void onPause() {
		super.onPause();
		self = null;
	}



	@Override
	public void onStop() {
		super.onStop();
	}



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		self = this;
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

		onAttach();
		//Billing.create();
		if (App.isDevel())
			Kamman.create(new String[]{"320x240","640x480","1280x720","1920x1080","2560x1440","3840x2160"});
		else
			Kamman.create(new String[]{"320x240","640x480","1280x720","1920x1080"});

		loadHost();

		mToolbar = (Toolbar) findViewById(R.id.main_toolbar);
		setSupportActionBar(mToolbar);

		mActBar = getSupportActionBar();
		mActBar.setDisplayShowTitleEnabled(false); // Hide default toolbar title
		mTitle = (TextView) findViewById(R.id.main_title);

		mTestView = findViewById(R.id.main_test_view);
		mTestView.setOnClickListener(TestClicker);

		mSigninStrip = findViewById(R.id.main_signin_strip);
		mRtmpedStrip = findViewById(R.id.main_rtmped_golive_strip);

		mResetUrlButton = (Button) findViewById(R.id.main_reset_key_button);
		mResetUrlButton.setOnClickListener(ResetUrlClicker);

		mSetupUrlButton = (Button) findViewById(R.id.main_setup_url_button);
		mSetupUrlButton.setOnClickListener(HostClicker);

		mGoliveButton = (Button) findViewById(R.id.main_golive_button);
		mGoliveButton.setOnClickListener(GoliveClicker);

		mTwitchDashboard = (Button) findViewById(R.id.twitch_dashboard);
		mTwitchDashboard.setOnClickListener(TwitchDashboardClicker);

		mTwitchGuide = (Button) findViewById(R.id.twitch_guide);
		mTwitchGuide.setOnClickListener(TwitchGuideClicker);

		mYoutubeDashboard = (Button) findViewById(R.id.youtube_dashboard);
		mYoutubeDashboard.setOnClickListener(YoutubeDashboardClicker);

		mYoutubeGuide = (Button) findViewById(R.id.youtube_guide);
		mYoutubeGuide.setOnClickListener(YoutubeGuideClicker);

		mFrontLayout = findViewById(R.id.main_front_layout);
		mFrontSpinner = (Spinner)findViewById(R.id.main_front_spinner);
		mFrontSpinner.setOnItemSelectedListener(FRONT_CLICKER);
		if (Kamman.get().getFrontId().isEmpty())
			mFrontLayout.setVisibility(View.GONE);
		else
			mFrontResols = Kamman.get().getFrontResoList().getTextArray();
		ArrayAdapter<String> frontAdapter = new ArrayAdapter<String>(this, R.layout.reso_item, mFrontResols);
		mFrontSpinner.setAdapter(frontAdapter);
		mFrontSpinner.setSelection(Kamman.get().getWantedFrontIndex());

		mBackLayout = findViewById(R.id.main_back_layout);
		mBackSpinner = (Spinner)findViewById(R.id.main_back_spinner);
		mBackSpinner.setOnItemSelectedListener(BACK_CLICKER);
		if (Kamman.get().getBackId().isEmpty())
			mBackLayout.setVisibility(View.GONE);
		else
			mBackResols = Kamman.get().getBackResoList().getTextArray();
		ArrayAdapter<String> backAdapter = new ArrayAdapter<String>(this, R.layout.reso_item, mBackResols);
		mBackSpinner.setAdapter(backAdapter);
		mBackSpinner.setSelection(Kamman.get().getWantedBackIndex());

//		mBitrateSpinner = (Spinner)findViewById(R.id.main_bitrate_spinner);
//		mBitrateSpinner.setOnItemSelectedListener(BITRATE_CLICKER);
//		ArrayAdapter<String> bitrateAdapter = new ArrayAdapter<String>(this, R.layout.reso_item, mBitrateLabels);
//		mBitrateSpinner.setAdapter(bitrateAdapter);
//		int maxrate = Encoder.get().getMaxBitrate();
//		mBitrateSpinner.setSelection(getBitrateIndex(maxrate));



//		mFilmingLayout = findViewById(R.id.main_film_layout);
//		mFilmingSwitch = (Switch) findViewById(R.id.main_film_switch);
//		mFilmingSwitch.setChecked(App.isFilming());
//		mFilmingSwitch.setOnCheckedChangeListener(FilmClicker);

		View panelBack = findViewById(R.id.main_panel_back);
		mPanelManager = new PanelManager(222, panelBack, null);
		mLeftPanel = findViewById(R.id.main_panel_left);
		mOpenPanel = (ImageButton) findViewById(R.id.main_open_panel);
		mOpenPanel.setOnClickListener(OpenPanelClicker);
		mClosePanel = (ImageButton) findViewById(R.id.main_close_panel);
		mClosePanel.setOnClickListener(ClosePanelClicker);

		mPanelList = (ListView) findViewById(R.id.main_panel_list);
		mPanelList.setOnItemClickListener(new LeftPanelItemClicker());

		if (!havePermissions()) { return; }
		mTackHandler = new Handler();
		mTackHandler.postDelayed(clickTack, 999);

		App.fbLog("maac_oncr");
		mToolbar.post(new Runnable() {
			@Override
			public void run() {
				onCreateShow();
			}
		});
	}



	public void onCreateShow() {
		if (PanelManager.type == PanelManager.TYPE.LEFT)
			OpenPanelClicker.onClick(null);
	}



	@Override
	protected void onDestroy() {
		super.onDestroy();
		self = null;
		//Billing.delete();
	}



	public void onAttach() {
		if (App.live) return; // already created
		App.live = true;
		App.log("onAttach");
	}


	@Override
	public void onBackPressed() {
		if (mPanelManager.clear()) {
			return;
		}
		super.onBackPressed();
	}




	private boolean havePermissions() {
		if (!App.selfPermissionGranted(this, Manifest.permission.INTERNET)) {
			App.finishActivityAlert(this, "Permission needed!", "You need to allow access to internet!");
			return false;
		}

		if (!App.selfPermissionGranted(this, Manifest.permission.CAMERA)) {
			App.finishActivityAlert(this, "Permission needed!", "You need to allow access to camera!");
			return false;
		}

		if (!App.selfPermissionGranted(this, Manifest.permission.RECORD_AUDIO)) {
			App.finishActivityAlert(this, "Permission needed!", "You need to allow audio recording!");
			return false;
		}

		if (!App.selfPermissionGranted(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
			App.finishActivityAlert(this, "Permission needed!", "You need to allow storage reading!");
			return false;
		}

		if (!App.selfPermissionGranted(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
			App.finishActivityAlert(this, "Permission needed!", "You need to allow storage writing!");
			return false;
		}

		return true;
	}



	private Runnable clickTack = new Runnable() {
		@Override
		public void run() {
			mTackHandler.postDelayed(this, 999);
			firstTack();
		}
	};



	void firstTack() {
		if (mTacked) return;
		mTacked = true;

//		RtmpJni.selfTest();
	}











	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		App.log("onCreateOptionsMenu");
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);

		return true;
	}



	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		updateMenu();
		return super.onPrepareOptionsMenu(menu);
	}



	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		App.log("onOptionsItemSelected");
		switch (item.getItemId()) {
//			case R.id.main_meit_share:
//				shareFilm();
//				return true;
		}
		return super.onOptionsItemSelected(item);
	}





	// * doers


	private void loadHost() {
		Encoder.hostUrl = App.getPrefStr(App.PREF_RTMP_URL);
		Encoder.hostKey = App.getPrefStr(App.PREF_RTMP_KEY);
		if (App.isDevel()) {
//			Encoder.hostUrl = "a.rtmp.youtube.com/live2"; Encoder.hostKey = "rtky-yz1r-rj17-4f96";
//			Encoder.hostUrl = "live.twitch.tv/app"; Encoder.hostKey = "live_111622524_8dD6qWqX2Qg7PeIDtBWQAlj2Fv7JO2";
		}
	}



	private void saveHost() {
		App.setPrefStr(App.PREF_RTMP_URL, Encoder.hostUrl);
		App.setPrefStr(App.PREF_RTMP_KEY, Encoder.hostKey);
	}



	private void updateLook() {
		mSigninStrip.setVisibility(View.GONE);
		mRtmpedStrip.setVisibility(View.GONE);

		if (!Encoder.hostKey.isEmpty()) {
			String title = Encoder.get().getCastHost();
			mTitle.setText(title);
			Tools.SlowShow(mRtmpedStrip, 500);
			mResetUrlButton.setVisibility(View.VISIBLE);
		} else {
			mTitle.setText("Set up the broadcast url");
			Tools.SlowShow(mSigninStrip, 500);
			mResetUrlButton.setVisibility(View.GONE);
		}

		updateStatus();
		updateMenu();
	}



	private void updateStatus() {
//        TextView view = (TextView) findViewById(R.id.main_tevi_status);
//        if (mTotalFiles == 0) {
//            view.setText("No videos found");
//        } else {
//            String text;
//            text = mTotalFiles + " " + App.resstr(R.string.videos) + ", " + App.resstr(R.string.total_size) + " " + Tools.formatSize(mTotalBytes);
//            view.setText(text);
//        }
	}



	public void updateMenu() {
//		if (null == mDeleteMeit) return;
		boolean haveWifi = Connectivity.isConnectedWifi(this);
	}





	// * clickers




	View.OnClickListener TestClicker = new View.OnClickListener() {
		@Override
		public void onClick(final View v) {
			App.log("TestClicker_1");
		}
	};



	CompoundButton.OnCheckedChangeListener FilmClicker = new CompoundButton.OnCheckedChangeListener() {

			public void onCheckedChanged(CompoundButton aSender, boolean aChecked) {
				//App.log("FilmClicker: " + aChecked);
				App.enableFilming(aChecked);
		}
	};



	View.OnClickListener ResetUrlClicker = new View.OnClickListener() {
		@Override
		public void onClick(final View v) {
			Encoder.hostUrl = "";
			Encoder.hostKey = "";
			saveHost();
			updateLook();
		}
	};





	View.OnClickListener TwitchDashboardClicker = new View.OnClickListener() {
		@Override
		public void onClick(final View v) {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.twitch.tv/dashboard"));
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			startActivity(intent);
		}
	};

	View.OnClickListener TwitchGuideClicker = new View.OnClickListener() {
		@Override
		public void onClick(final View v) {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://rustero.com/twitchguide/index.html#login"));
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			startActivity(intent);
		}
	};




	View.OnClickListener YoutubeDashboardClicker = new View.OnClickListener() {
		@Override
		public void onClick(final View v) {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/live_dashboard"));
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			startActivity(intent);
		}
	};

	View.OnClickListener YoutubeGuideClicker = new View.OnClickListener() {
		@Override
		public void onClick(final View v) {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://rustero.com/youtubeguide/index.html#howto"));
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			startActivity(intent);
		}
	};




	View.OnClickListener HostClicker = new View.OnClickListener() {
		@Override
		public void onClick(final View v) {
			askSetup();
		}
	};



	View.OnClickListener GoliveClicker = new View.OnClickListener() {
		@Override
		public void onClick(final View v) {
			Intent intent = new Intent(MainActivity.this, RecordActivity.class);
			startActivityForResult(intent, ACT_RESULT_RECORD);
		}
	};





	AdapterView.OnItemSelectedListener FRONT_CLICKER = new AdapterView.OnItemSelectedListener() {

		public void onNothingSelected(AdapterView<?> parent) {}

		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			String resoText = mFrontResols[position];
			Kamman.get().setWantedFrontReso(resoText);
		}
	};



	AdapterView.OnItemSelectedListener BACK_CLICKER = new AdapterView.OnItemSelectedListener() {

		public void onNothingSelected(AdapterView<?> parent) {}

		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			String resoText = mBackResols[position];
			Kamman.get().setWantedBackReso(resoText);
		}
	};


//
//	AdapterView.OnItemSelectedListener BITRATE_CLICKER = new AdapterView.OnItemSelectedListener() {
//
//		public void onNothingSelected(AdapterView<?> parent) {}
//
//		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//			int bitrate = mBitrateValues[position];
//			Encoder.get().setMaxBitrate(bitrate);
//		}
//	};




	private void showSettings() {
//		Mycams.verifySelectedResolutions();
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}



//	private void buyPremium() {
//		Intent intent = new Intent(this, BuyActivity.class);
//		startActivityForResult(intent, 0);
//	}



	private void showAbout() {
        //App.fbLog("showAbout");
		Intent intent = new Intent(this, AboutActivity.class);
		startActivityForResult(intent, 0);
	}



	private void showEncourage() {
		Uri uri = Uri.parse("market://details?id=" + getPackageName());
		Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);
		try {
			startActivity(myAppLinkToMarket);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(this, " unable to find market app", Toast.LENGTH_LONG).show();
		}
	}



	private void showMention() {
		try {
			String urlToShare = "https://play.google.com/store/apps/details?id=rustero.live.streaming.encoder";
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_TEXT, urlToShare);

			// As fallback, launch sharer.php in a browser
			String fbPackage = getFacebookPackage(intent);
			if ("" == fbPackage) {
				String sharerUrl = "https://www.facebook.com/sharer/sharer.php?u=" + urlToShare;
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse(sharerUrl));
			} else
				intent.setPackage(fbPackage);

			startActivity(intent);

		} catch (Exception e) {	}
	}



	private String getFacebookPackage(Intent intent) {
		String result = "";
		List<ResolveInfo> matches = getPackageManager().queryIntentActivities(intent, 0);
		for (ResolveInfo info : matches) {
			if (info.activityInfo.packageName.toLowerCase().startsWith("com.facebook.katana")) {
				result = info.activityInfo.packageName;
				break;
			}
		}
		return result;
	}



	private void showInvite() {
		try {
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_SUBJECT, "Stream live on YouTube with special effects.");
			intent.putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=rustero.live.streaming.encoder");
			startActivity(Intent.createChooser(intent, "Share with ..."));
		} catch (Exception e) {
		}
	}





	@Override
	protected void onActivityResult(int aSender, int resultCode, Intent aIntent) {
		super.onActivityResult(aSender, resultCode, aIntent);

		switch (aSender) {
			case ACT_RESULT_RECORD:
				onRecordIntentResult(resultCode);
				break;
		}
	}




	public void onRecordIntentResult(int aResultCode) {}




	private void ask5stars() {
		if (!App.want5stars()) return;
		long nowSecs = Tools.mills() / 1000;
		App.setPrefLong("rate5secs", nowSecs);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Please encourage us with 5 stars rating! \n\n");

		builder.setNegativeButton(App.resstr(R.string.not_now), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		builder.setPositiveButton(R.string.rate_5_stars, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				App.setPrefBln("rate5done", true);
				showEncourage();
			}
		});

		AlertDialog dialog = builder.create();
		dialog.setCancelable(false);
		dialog.show();
	}





	View.OnClickListener OpenPanelClicker = new View.OnClickListener() {
		@Override
		public void onClick(final View v) {
			mMameItems = new ArrayList<MameItem>();
			///if (!App.isPremium()) mMameItems.add(new MameItem(MAME_ITEM_BUY, R.drawable.premium_upgrade_54, App.resstr(R.string.premium_upgrade)));
			mMameItems.add(new MameItem(MAME_ITEM_ENCOURAGE, R.drawable.star_54, App.resstr(R.string.encaurageus)));
			mMameItems.add(new MameItem(MAME_ITEM_INVITE, R.drawable.thumb_54, App.resstr(R.string.invite_friend)));
			mMameItems.add(new MameItem(MAME_ITEM_MENTION, R.drawable.facebook_54, App.resstr(R.string.mention_facebook)));
			mMameItems.add(new MameItem(MAME_ITEM_ABOUT, R.drawable.about_54, App.resstr(R.string.about)));

			LeftPanelAdapter adapter = new LeftPanelAdapter(MainActivity.this, R.layout.menu_row, mMameItems);
			mPanelList.setAdapter(adapter);

			if (null == v)
				mPanelManager.quick = true;
			mPanelManager.openLeft(mLeftPanel);
		}
	};



	View.OnClickListener ClosePanelClicker = new View.OnClickListener() {
		@Override
		public void onClick(final View v) {
			mPanelManager.clear();
		}
	};




	private class LeftPanelItemClicker implements ListView.OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			int tag = 0;
			if (position < mMameItems.size())
				tag = mMameItems.get(position).tag;
			mPanelManager.clear();
			mPanelList.setItemChecked(position, false);

			//App.showShortToast("tag: " + tag);
			switch (tag) {
//				case MAME_ITEM_BUY:
//					buyPremium();
//					return;
				case MAME_ITEM_ABOUT:
					showAbout();
					return;
				case MAME_ITEM_ENCOURAGE:
					showEncourage();
					return;
				case MAME_ITEM_INVITE:
					showInvite();
					return;
				case MAME_ITEM_MENTION:
					showMention();
					return;
			}
		}

	}



	public class LeftPanelAdapter extends ArrayAdapter<MameItem> {
		Context mContext;
		int layoutResourceId;
		ArrayList<MameItem> mItems = null;

		public LeftPanelAdapter(Context mContext, int layoutResourceId, ArrayList<MameItem> data) {
			super(mContext, layoutResourceId, data);
			this.layoutResourceId = layoutResourceId;
			this.mContext = mContext;
			this.mItems = data;
		}


		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View listItem = convertView;
			LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
			listItem = inflater.inflate(layoutResourceId, parent, false);

			ImageView imageViewIcon = (ImageView) listItem.findViewById(R.id.menu_row_icon);
			TextView textViewName = (TextView) listItem.findViewById(R.id.menu_row_name);

			MameItem item = mItems.get(position);
			imageViewIcon.setImageResource(item.icon);
			textViewName.setText(item.name);

			return listItem;
		}
	}



	public class MameItem {
		public int tag, icon;
		public String name;

		// Constructor.
		public MameItem(int tag, int icon, String name) {
			this.tag = tag;
			this.icon = icon;
			this.name = name;
		}
	}








	private void askSetup() {
		HostDialog.ask(this, new HostDialogEventer());
	}



	public class HostDialogEventer implements HostDialog.Events {

		public void onDone(String aUrl, String aKey) {
			//App.log("SetupDialogEventer: " + aUrl);
			Encoder.hostUrl = aUrl;
			Encoder.hostKey = aKey;
			saveHost();
			updateLook();
		}
	}




}
