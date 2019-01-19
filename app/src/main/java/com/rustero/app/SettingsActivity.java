package com.rustero.app;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.rustero.R;
import com.rustero.widgets.MyListPreference;
import com.rustero.widgets.MyNumberPreference;


@SuppressWarnings("deprecation")
public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	public static final String TAG = "SettingsActivity";


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings_screen);

		ListView listView = getListView();
		listView.setScrollbarFadingEnabled(false);
		listView.setPadding(11, 22, 11, 33);

//		MyListPreference listPreference;
//		listPreference = (MyListPreference) findPreference("front_resolution");
//		if (Mycams.hasFrontCamera()) {
//			listPreference.setEntries(Mycams.getFrontResoArray());
//			listPreference.setEntryValues(Mycams.getFrontResoArray());
//		} else {
//			listPreference.setEnabled(false);
//		}
//
//		listPreference = (MyListPreference) findPreference("back_resolution");
//		if (Mycams.hasBackCamera()) {
//			listPreference.setEntries(Mycams.getBackResoArray());
//			listPreference.setEntryValues(Mycams.getBackResoArray());
//		} else {
//			listPreference.setEnabled(false);
//		}

//		listPreference = (MyListPreference) findPreference("min_bitrate");
//		listPreference.setEntries(mMinBitrates);
//		listPreference.setEntryValues(mMinBitrates);


		initSummary(getPreferenceScreen());
	}



	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		LinearLayout root = (LinearLayout) findViewById(android.R.id.list).getParent().getParent().getParent();
		Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
		root.addView(bar, 0); // insert at top

		bar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}



	@Override
	protected void onResume() {
		super.onResume();
		// Set up a listener whenever a key changes
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}



	@Override
	protected void onPause() {
		super.onPause();
		// Unregister the listener whenever a key changes
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}


	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		updatePrefSummary(findPreference(key));
	}


	private void initSummary(Preference p) {
		//Log.i(TAG, "initSummary_11");
		if (p instanceof PreferenceGroup) {
			PreferenceGroup pGrp = (PreferenceGroup) p;
			for (int i = 0; i < pGrp.getPreferenceCount(); i++) {
				initSummary(pGrp.getPreference(i));
			}
		} else {
			updatePrefSummary(p);
		}
	}


	private void updatePrefSummary(Preference p) {
		//Log.i(TAG, "updatePrefSummary_11");
		if (p instanceof ListPreference) {
			ListPreference listPref = (ListPreference) p;
			CharSequence item = listPref.getEntry();
			if (item != null)
				p.setSummary(item);
			else
				p.setSummary("Not available");
		}

		if (p instanceof EditTextPreference) {
			EditTextPreference editTextPref = (EditTextPreference) p;
			if (p.getTitle().toString().startsWith("Password")) {
				p.setSummary("******");
			} else {
				String text = editTextPref.getText();
				if ((text != null) && (text.length() > 0))
					p.setSummary(text);
				else {
					String summary = editTextPref.getSummary().toString();
					if (!summary.startsWith(" "))
						p.setSummary(text);
				}
			}
		}

		if (p instanceof MultiSelectListPreference) {
			EditTextPreference editTextPref = (EditTextPreference) p;
			p.setSummary(editTextPref.getText());
		}

		if (p instanceof MyNumberPreference) {
			MyNumberPreference myNumberPreference = (MyNumberPreference) p;
			int value = myNumberPreference.getValue();
			String summary = "" + value;
			String key = p.getKey();
			if (key.contains("_minutes")) {
				if (value > 1)
					summary += " minutes";
				else
					summary += " minute";
			}
			p.setSummary(summary);
		}
	}



}