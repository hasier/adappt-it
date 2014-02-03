package com.adapptit.android.webparser.actions;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Element;

import com.adapptit.android.Action;
import com.adapptit.android.ConfigurationManager;
import com.adapptit.android.webparser.SelectorUtils;

public class RemoveAction implements Action {

	@Override
	public void doAction(ConfigurationManager confManager, JSONObject action,
			Object... o) throws JSONException {
		Element e = (Element) o[1];
		SelectorUtils.resolveExpression(e,
				action.getString(ConfigurationManager.ELEMENT)).remove();
	}

	@Override
	public String getActionName() {
		return "REMOVE";
	}

}
