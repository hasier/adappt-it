package com.adapptit.android;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;

public abstract class CustomFragment extends Fragment {

	private MainActivity activity;
	private ConfigurationManager confManager;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.activity = (MainActivity) activity;
		confManager = this.activity.getConfManager();
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		activity.fragmentReady();
	}

	protected MainActivity getMainActivity() {
		return activity;
	}

	protected ConfigurationManager getConfManager() {
		return confManager;
	}

	/**
	 * 
	 * @return true if the default action at onBackPressed should be called in
	 *         the parent Activity
	 */
	public abstract boolean onBackPressed();

}
