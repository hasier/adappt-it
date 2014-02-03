package com.adapptit.android.webparser.actions;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import com.adapptit.android.Action;
import com.adapptit.android.ConfigurationManager;
import com.adapptit.android.webparser.SelectorUtils;

public class AttrAction implements Action {

	@Override
	public void doAction(ConfigurationManager confManager, JSONObject action,
			Object... o) throws JSONException {
		Element e = (Element) o[1];
		Elements es = SelectorUtils.resolveExpression(e,
				action.getString(ConfigurationManager.ELEMENT));
		String key = action.getString(ConfigurationManager.KEY);
		String value = action.getString(ConfigurationManager.VALUE);
		boolean direct = true;
		if (key.equals("onload")) {
			if (es.size() > 1 || !es.first().tagName().equals("body")) {
				direct = false;
			}
		}
		if (direct) {
			es.attr(key, value);
		} else {
			Element script = new Element(Tag.valueOf("script"), "");
			script.attr("type", "text/javascript");
			script.append(value);
			for (Element el : es) {
				el.appendChild(script);
			}
		}
		// FIXME Cuidado con lo del video por si no es texo plano y hay que
		// resolveExpression
	}

	@Override
	public String getActionName() {
		return "ATTR";
	}

}
