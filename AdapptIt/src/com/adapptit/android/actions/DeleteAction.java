package com.adapptit.android.actions;

import org.json.JSONException;
import org.json.JSONObject;

import com.adapptit.android.Action;
import com.adapptit.android.ConfigurationManager;
import com.adapptit.android.PreferencesManager;

public class DeleteAction implements Action {

	@Override
	public void doAction(ConfigurationManager confManager, JSONObject action,
			Object... o) throws JSONException {
		PreferencesManager.getInstance().prefs
				.edit()
				.remove(action.getString(ConfigurationManager.ELEMENT) + "_"
						+ confManager.getConfName()).commit();
	}

	@Override
	public final String getActionName() {
		return "DELETE";
	}

}
