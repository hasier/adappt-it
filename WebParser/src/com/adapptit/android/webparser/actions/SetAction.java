package com.adapptit.android.webparser.actions;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.util.Log;

import com.adapptit.android.ConfigurationManager;
import com.adapptit.android.webparser.SelectorUtils;

public class SetAction extends com.adapptit.android.actions.SetAction {

	public static final String TAG = "SetAction";

	@Override
	public void doAction(ConfigurationManager confManager, JSONObject action,
			Object... o) throws JSONException {
		if (action.isNull(ConfigurationManager.SOURCE)) {
			super.doAction(confManager, action, o);
		} else {
			String source = action.getString(ConfigurationManager.SOURCE);
			if (source.equals("PARSE")) {
				Element root = (Element) o[1];
//				Log.v(TAG, root.toString());
//				Log.v(TAG, root.toString().contains("course-topbanner-logo-name") + "");
				String value = action.getString(ConfigurationManager.VALUE);
				String[] split = value.split(" OR ");
				for (String s : split) {
					boolean correct = true;
					int index = s.lastIndexOf(".");
					Elements es = null;
					try {
						String expression = s.substring(0, index);
						es = SelectorUtils.resolveExpression(root, expression);
					} catch (Exception ex) {
						Log.w(TAG, "Expression '" + s
								+ "' is not valid for setting", ex);
						correct = false;
					}
					if (correct && es != null && !es.isEmpty()) {
						value = SelectorUtils.getValue(es.first(),
								s.substring(index + 1));
						JSONObject obj = new JSONObject();
						obj.put(ConfigurationManager.ELEMENT,
								action.getString(ConfigurationManager.ELEMENT));
						obj.put(ConfigurationManager.VALUE, value);
						super.doAction(confManager, obj);
						break;
					}
				}
			}
		}
	}

}
