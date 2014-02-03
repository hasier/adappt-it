package com.adapptit.android.actions;

import org.json.JSONException;
import org.json.JSONObject;

import com.adapptit.android.Action;
import com.adapptit.android.ConfigurationManager;
import com.adapptit.android.PreferencesManager;
import com.adapptit.android.RegexUtils;

public class SaveAction implements Action {

	@Override
	public void doAction(ConfigurationManager confManager, JSONObject action,
			Object... o) throws JSONException {
		String url = (String) o[0];
		String expression = action.getString(ConfigurationManager.VALUE);
		String key = action.getString(ConfigurationManager.ELEMENT);
		key += "_" + confManager.getConfName();
		// String last = action.getString(ConfigurationManager.VALUE);
		// String field = RegexUtils.extractFieldFromExpression(value);
		// String fieldValue = (String) confManager.getVar(field);
		// value = value.replace(RegexUtils.VAR_DELIMIT[0] + field
		// + RegexUtils.VAR_DELIMIT[1], fieldValue);
		boolean isRegex = false;
		if (expression.contains("(")) {
			isRegex = true;
		}
		expression = RegexUtils
				.replaceAllVars(confManager, expression, isRegex);
		String value = expression;
		if (isRegex) {
			value = url.replaceAll(expression, "$1");
		}
		PreferencesManager.getInstance().prefs.edit().putString(key, value)
				.commit();
		confManager.setVar(key, value);
	}

	@Override
	public final String getActionName() {
		return "SAVE";
	}

}
