package com.adapptit.android.actions;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Environment;
import android.util.Log;

import com.adapptit.android.Action;
import com.adapptit.android.ConfigurationManager;
import com.adapptit.android.CustomClass;

import dalvik.system.DexClassLoader;

public class CustomClassAction implements Action {

	private static final String TAG = "CustomClassAction";

	@Override
	public void doAction(ConfigurationManager confManager, JSONObject action,
			Object... o) throws JSONException {
		String className = action.getString(ConfigurationManager.VALUE);
		String jar = action.getString(ConfigurationManager.ELEMENT);
		if (!jar.endsWith(".jar")) {
			jar = jar + ".jar";
		}
		try {
			Class<?> clazz = confManager.getCustomClass(className);
			if (clazz == null) {
//				ClassLoader cl = new DexClassLoader(
//						Environment.getExternalStorageDirectory()
//								+ "/AdapptIt/configs/" + jar, confManager
//								.getActivity().getCacheDir().getAbsolutePath(),
//						null, getClass().getClassLoader());
				ClassLoader cl = new DexClassLoader(
						Environment.getExternalStorageDirectory()
								+ "/AdapptIt/configs/" + jar, confManager
								.getActivity().getCacheDir().getAbsolutePath(),
						null, ConfigurationManager.getCurrentClassLoader());
				clazz = cl.loadClass(className);
				confManager.setCustomClass(className, clazz);
			}
			((CustomClass) clazz.newInstance())
					.run(confManager, o[1]);
		} catch (Exception e) {
			Log.e(TAG, "Error loading custom class", e);
		}
	}

	@Override
	public final String getActionName() {
		return "CUSTOM_CLASS";
	}

}
