package com.adapptit.android.webparser;

import java.util.regex.Pattern;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public final class SelectorUtils {

	private SelectorUtils() {
	}

	/**
	 * Resolves and returns the selected elements specified in the expression
	 * from the root object
	 * 
	 * @param root
	 *            The element from which to start the search
	 * @param expression
	 *            The expression to analyze
	 * @return The elements that match the expression from the root element
	 */
	public static Elements resolveExpression(Element root, String expression) {
		return resolveExpression(new Elements(root), expression);
	}

	/**
	 * Resolves and returns the selected elements specified in the expression
	 * from the root objects
	 * 
	 * @param root
	 *            The elements from which to start the search
	 * @param expression
	 *            The expression to analyze
	 * @return The elements that match the expression from the root elements
	 */
	public static Elements resolveExpression(Elements root, String expression) {
		boolean children = false;
		if (expression.endsWith("*")) {
			expression = expression.substring(0, expression.length() - 1);
			children = true;
		} else if (expression.startsWith("*")) {
			expression = expression.substring(1, expression.length());
			Element e = root.first();
			while (e.parent() != null) {
				e = e.parent();
			}
			// root = e.getAllElements();
			root = new Elements(e);
		}
		Selector selType = null;
		Mode mode;
		if (expression.startsWith("S")) {
			mode = Mode.STARTS;
			expression = expression.substring(1);
		} else if (expression.startsWith("E")) {
			mode = Mode.ENDS;
			expression = expression.substring(1);
		} else if (expression.startsWith("C")) {
			mode = Mode.CONTAINS;
			expression = expression.substring(1);
		} else {
			mode = Mode.NORMAL;
		}
		String[] split = expression.split("\\.");
		String first = split[0];
		Elements ret = null;
		try {
			int i = Integer.parseInt(first);
			ret = new Elements(root.get(i));
		} catch (NumberFormatException e) {
		}
		if (ret == null) {
			if (first.startsWith("class")) {
				selType = Selector.CLASS;
			} else if (first.startsWith("id")) {
				selType = Selector.ID;
			} else if (first.startsWith("tag")) {
				selType = Selector.TAG;
			} else if (first.startsWith("attr")) {
				selType = Selector.ATTR;
			}
			String selector = SelectorUtils.extractSelector(first);
			String key = null;
			switch (selType) {
			case CLASS:
				key = "class";
				break;
			case ID:
				if (mode != Mode.NORMAL) {
					key = "id";
				} else {
					ret = new Elements();
					for (int i = 0; i < root.size(); i++) {
						ret.add(root.get(i).getElementById(selector));
					}
					// ret = new
					// Elements(root.first().getElementById(selector));
				}
				break;
			case TAG:
				ret = new Elements();
				for (int i = 0; i < root.size(); i++) {
					ret.addAll(root.get(i).getElementsByTag(selector));
				}
				// ret = root.first().getElementsByTag(selector);
				break;
			case ATTR:
				key = selector.split(",")[0];
				selector = selector.substring(selector.indexOf(",") + 1);
				break;
			}
			if (key != null) {
				ret = new Elements();
				switch (mode) {
				case CONTAINS:
					for (int i = 0; i < root.size(); i++) {
						ret.addAll(root.get(i)
								.getElementsByAttributeValueContaining(key,
										selector));
					}
					break;
				case ENDS:
					for (int i = 0; i < root.size(); i++) {
						ret.addAll(root.get(i)
								.getElementsByAttributeValueEnding(key,
										selector));
					}
					break;
				case STARTS:
					for (int i = 0; i < root.size(); i++) {
						ret.addAll(root.get(i)
								.getElementsByAttributeValueStarting(key,
										selector));
					}
					break;
				case NORMAL:
					for (int i = 0; i < root.size(); i++) {
						ret.addAll(root.get(i).getElementsByAttributeValue(key,
								selector));
					}
					break;
				}
			}
		}
		if (split.length > 1) {
			ret = resolveExpression(ret,
					expression.substring(expression.indexOf(".") + 1));
		}
		if (children) {
			Elements es = new Elements();
			for (int i = 0; i < ret.size(); i++) {
				es.addAll(ret.get(i).getAllElements());
			}
			return es;
		} else {
			return ret;
		}
	}

	/**
	 * Extracts the selector from a class(), tag(), id() or attr() expressions
	 * 
	 * @param expression
	 *            The expression from which to extract the selector
	 * @return The selector
	 */
	public static String extractSelector(String expression) {
		expression = Pattern.quote(expression);
		return expression.replaceAll(".*\\((.*)\\).*", "$1");
	}

	/**
	 * Returns the value from the expression. If it points to an HTML attribute
	 * it will retrieve so; otherwise, the same value will be returned
	 * 
	 * @param e
	 *            The element which may have the attribute
	 * @param expression
	 *            The expression from which to extract the attribute (if any)
	 * @return The inferred value
	 */
	public static String resolveAttrOrValue(Element e, String expression) {
		if (expression.startsWith("attr")) {
			String attr = extractSelector(expression);
			return getValue(e, attr);
		} else {
			return expression;
		}
	}

	/**
	 * Returns a value from an attribute
	 * 
	 * @param e
	 *            The element from which to extract the value
	 * @param attr
	 *            The chosen attribute: text, src, href...
	 * @return The attribute value
	 */
	public static String getValue(Element e, String attr) {
		if (attr.equals("text")) {
			return e.text();
		} else {
			return e.attr(attr);
		}
	}

	// private static boolean isSelector(String expression) {
	// if (expression.startsWith("class") || expression.startsWith("id")
	// || expression.startsWith("tag")
	// || expression.startsWith("attr")) {
	// return true;
	// } else {
	// return false;
	// }
	// }

	public static String normalizeJSCommand(String selector) {
		return selector.replace("'", "\\'").replace("\\\\'", "\\'");
	}

	private static enum Mode {
		STARTS, ENDS, CONTAINS, NORMAL
	}

	private static enum Selector {
		TAG, ID, CLASS, ATTR
	}

}
