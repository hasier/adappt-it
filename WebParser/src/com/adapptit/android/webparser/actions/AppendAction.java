package com.adapptit.android.webparser.actions;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import android.util.Log;

import com.adapptit.android.Action;
import com.adapptit.android.ConfigurationManager;
import com.adapptit.android.webparser.SelectorUtils;
import com.adapptit.android.webparser.WebParserConfigurationManager;

public class AppendAction implements Action {

	@Override
	public void doAction(ConfigurationManager confManager, JSONObject action,
			Object... o) throws JSONException {
		Element e = (Element) o[1];
		Elements elements = SelectorUtils.resolveExpression(e,
				action.getString(ConfigurationManager.ELEMENT));
		Element toAppend = new Element(Tag.valueOf(action
				.getString(WebParserConfigurationManager.TAG_KEY)), "");
		try {
			for (int i = 1; !action.isNull(WebParserConfigurationManager.ATTR
					+ i); i++) {
				toAppend.attr(action
						.getString(WebParserConfigurationManager.ATTR + i),
						action.getString(ConfigurationManager.VALUE + i));
			}
		} catch (JSONException ex) {
			Log.e("AppendAction",
					"JSON element is not well formatted. The obtained element up to now will be appended",
					ex);
		}
		try {
			int pos = Integer.parseInt(action
					.getString(WebParserConfigurationManager.POSITION));
			for (Element el : elements) {
				el.insertChildren(pos, new Elements(toAppend));
			}
		} catch (JSONException ex) {
			for (Element el : elements) {
				el.appendChild(toAppend);
			}
		}
	}

	@Override
	public String getActionName() {
		return "APPEND";
	}

}
