package com.adapptit.android.actions;

import org.json.JSONException;
import org.json.JSONObject;

import com.adapptit.android.Action;
import com.adapptit.android.ConfigurationManager;
import com.adapptit.android.MenuItem;
import com.adapptit.android.RegexUtils;

public class MenuAddAction implements Action {

	@Override
	public void doAction(ConfigurationManager confManager, JSONObject action,
			Object... o) throws JSONException {
		String key = action.getString(ConfigurationManager.KEY);
		String value = RegexUtils.replaceAllVars(confManager,
				action.getString(ConfigurationManager.VALUE), false);
		confManager.getMenuActions().add(new MenuItem<String>(key, value));
	}

	@Override
	public final String getActionName() {
		return "MENU_ADD";
	}

}
