package com.rustero.widgets;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
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


public class WaitDialog {


	private static ProgressDialog mDialog;
	private static String mMessage;



	public static void show(Activity aActivity, String aMessage) {
		if (null != mDialog) return;
		try {
			mDialog = ProgressDialog.show(aActivity, null, aMessage, true);
		} catch (Exception ex) {
			mDialog = null;
		}
	}



	public static void hide() {
		if (null == mDialog) return;
		try {
			mDialog.dismiss();
		} catch (Exception ex) {}
		mDialog = null;
	}


}
