package com.adapptit.android;

import org.json.JSONException;
import org.json.JSONObject;

public interface Action {

	public void doAction(ConfigurationManager confManager, JSONObject action, Object... o) throws JSONException;

	public String getActionName();

}
