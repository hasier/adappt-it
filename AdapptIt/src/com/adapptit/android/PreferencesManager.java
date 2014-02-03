package com.adapptit.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferencesManager {

	private static PreferencesManager instance;
//	public static final String LOGIN_KEY = "loginKey";

	public final SharedPreferences prefs;

	public static void createInstance(Context context) {
		instance = new PreferencesManager(context);
	}

	public static PreferencesManager getInstance() {
		return instance;
	}

	private PreferencesManager(Context c) {
		prefs = PreferenceManager.getDefaultSharedPreferences(c);
	}

}
