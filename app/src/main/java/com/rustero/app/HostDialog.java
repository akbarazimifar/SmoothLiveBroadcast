package com.rustero.app;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.rustero.App;
import com.rustero.R;


public class HostDialog extends DialogFragment {


	public interface Events {
		void onDone(String aUrl, String aKey);
	}

	private static HostDialog mSelf;
	private static Events mEventer;


	public static void ask(FragmentActivity aActivity, Events aEventer) {
		mEventer = aEventer;
		mSelf = new HostDialog();
		FragmentManager manager = aActivity.getSupportFragmentManager();
		mSelf.show(manager, "HostDialog");
	}



	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		App.log(" * onCreateView");
		View dialogLayout = inflater.inflate(R.layout.host_dialog, container, false);
		final Dialog dialog = getDialog();
		dialog.setCanceledOnTouchOutside(false);
		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE); //set to adjust screen height automatically, when soft keyboard appears on screen

		String lastUrl = App.getPrefStr(App.PREF_LAST_URL);
		final EditText urlView = (EditText)dialogLayout.findViewById(R.id.host_dlg_url_edit);
		if (!lastUrl.isEmpty())
			urlView.setText(lastUrl);
		urlView.setSelection(urlView.length());

		String lastKey = App.getPrefStr(App.PREF_LAST_KEY);
		final EditText keyView = (EditText)dialogLayout.findViewById(R.id.host_dlg_key_edit);
		if (!lastKey.isEmpty())
			keyView.setText(lastKey);
		keyView.setSelection(keyView.length());
		//keyView.requestFocus();

		final CheckBox twitchCheck = (CheckBox)dialogLayout.findViewById(R.id.setup_dlg_twitch_check);
		final CheckBox youtubeCheck = (CheckBox)dialogLayout.findViewById(R.id.setup_dlg_youtube_check);

		twitchCheck.setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (((CheckBox) v).isChecked()) {
							youtubeCheck.setChecked(false);
							urlView.setText("live.twitch.tv/app");
							urlView.setSelection(urlView.length());
						}					}
				}
		);
		youtubeCheck.setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (((CheckBox) v).isChecked()) {
							twitchCheck.setChecked(false);
							urlView.setText("a.rtmp.youtube.com/live2");
							urlView.setSelection(urlView.length());
						}					}
				}
		);

		final Button cancelButton = (Button)dialogLayout.findViewById(R.id.host_dlg_cancel);
		final Button doneButton = (Button)dialogLayout.findViewById(R.id.host_dlg_ok);

		cancelButton.setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						dialog.dismiss();
					}
				}
		);


		doneButton.setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v)
					{
						final int MIN_LENGTH = 3;
						String url = urlView.getText().toString();
						if (url.length() < MIN_LENGTH) {
							urlView.setError("Too short");
							return;
						}

						String key = keyView.getText().toString();
						if (key.length() < MIN_LENGTH) {
							keyView.setError("Too short");
							return;
						}

						dialog.dismiss();

						App.setPrefStr(App.PREF_LAST_URL, url);
						App.setPrefStr(App.PREF_LAST_KEY, key);
						if (null != mEventer)
							mEventer.onDone(url, key);
					}
				}
		);

		return dialogLayout;
	}



	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
	}



}
