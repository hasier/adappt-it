package com.adapptit.android.actions;

import org.json.JSONException;
import org.json.JSONObject;

import com.adapptit.android.Action;
import com.adapptit.android.ConfigurationManager;
import com.adapptit.android.RegexUtils;

public class SetAction implements Action {

	@Override
	public void doAction(ConfigurationManager confManager, JSONObject action,
			Object... o) throws JSONException {
		confManager.setVar(
				action.getString(ConfigurationManager.ELEMENT),
				RegexUtils.replaceAllVars(confManager,
						action.getString(ConfigurationManager.VALUE), false));
	}

	@Override
	public final String getActionName() {
		return "SET";
	}

}
