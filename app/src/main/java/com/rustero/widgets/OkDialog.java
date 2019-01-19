package com.rustero.widgets;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.rustero.R;


public class OkDialog extends DialogFragment {


	private static boolean sDialogShown = false;
	private static OkDialog mSelf;
	private static String mTitle;
	private static String mMessage;


	public static void ask(FragmentActivity aActivity, String aTitle, String aMessage) {
		if (sDialogShown) return;
		mTitle = aTitle;
		mMessage = aMessage;

		mSelf = new OkDialog();
		FragmentManager manager = aActivity.getSupportFragmentManager();
		mSelf.show(manager, "OkDialog");
	}



	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		sDialogShown = true;
		View dialogLayout = inflater.inflate(R.layout.ok_dialog, container, false);
		final Dialog dialog = getDialog();
		dialog.setCanceledOnTouchOutside(false);

		TextView tvTitle = (TextView) dialogLayout.findViewById(R.id.ok_dialog_title);
		tvTitle.setText(mTitle);

		TextView tvMessage = (TextView) dialogLayout.findViewById(R.id.ok_dialog_message);
		tvMessage.setText(mMessage);

		Button okButton = (Button) dialogLayout.findViewById(R.id.ok_dialog_ok);
		okButton.setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v)	{
						dialog.dismiss();
					}
				}
		);

		return dialogLayout;
	}



	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		sDialogShown = false;
	}


}
