package com.adapptit.android.webparser.actions;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.adapptit.android.Action;
import com.adapptit.android.ConfigurationManager;
import com.adapptit.android.webparser.SelectorUtils;

public class RemoveAllButAction implements Action {

	@Override
	public void doAction(ConfigurationManager confManager, JSONObject action,
			Object... o) throws JSONException {
		Element root = (Element) o[1];
		Elements es = SelectorUtils.resolveExpression(root,
				action.getString(ConfigurationManager.ELEMENT));
//		root.html(es.toString());
		root.html("");
//		for (Element e : root.children()) {
//			e.remove();
//		}
		for (Element e : es) {
			root.appendChild(e);
		}
	}

	@Override
	public String getActionName() {
		return "REMOVE_ALL_BUT";
	}

}
