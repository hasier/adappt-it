package com.adapptit.android.webparser.actions;

import org.json.JSONException;
import org.json.JSONObject;

import com.adapptit.android.Action;
import com.adapptit.android.ConfigurationManager;
import com.adapptit.android.webparser.SelectorUtils;
import com.adapptit.android.webparser.WebParserConfigurationManager;

public class ResetElementAction implements Action {

	@Override
	public void doAction(ConfigurationManager confManager, JSONObject action,
			Object... o) throws JSONException {
		String selector = SelectorUtils.normalizeJSCommand(action
				.getString(ConfigurationManager.ELEMENT));
		String command = "javascript: $('" + selector + "').replaceWith($('"
				+ selector + "').clone(false));";
		((WebParserConfigurationManager) confManager)
				.loadControllerJSCommand(command);
	}

	@Override
	public String getActionName() {
		return "RESET_ELEMENT";
	}

}
