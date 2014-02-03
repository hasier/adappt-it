package com.adapptit.android.webparser.actions;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.adapptit.android.ConfigurationManager;
import com.adapptit.android.webparser.SelectorUtils;

public class MenuAddAction extends com.adapptit.android.actions.MenuAddAction {

	@Override
	public void doAction(ConfigurationManager confManager, JSONObject action,
			Object... o) throws JSONException {

		if (action.isNull(ConfigurationManager.SOURCE)) {
			super.doAction(confManager, action, o);
		} else {
			String source = action.getString(ConfigurationManager.SOURCE);
			if (source.equals("PARSE")) {
				Element e = (Element) o[1];
				
				String expression = action
						.getString(ConfigurationManager.ELEMENT);
				Elements elements = SelectorUtils.resolveExpression(e,
						expression);
				String key = action.getString(ConfigurationManager.KEY);
				String value = action.getString(ConfigurationManager.VALUE);
				JSONObject obj;
				for (int i = 0; i < elements.size(); i++) {
					Element target = elements.get(i);
					obj = new JSONObject();
					obj.put(ConfigurationManager.KEY,
							SelectorUtils.resolveAttrOrValue(target, key));
					obj.put(ConfigurationManager.VALUE,
							SelectorUtils.resolveAttrOrValue(target, value));
					super.doAction(confManager, obj);
				}
			}
		}
	}
}
