package com.adapptit.android;

import java.util.regex.Pattern;

public final class RegexUtils {

	public static final String[] VAR_DELIMIT = new String[] { "{{", "}}" };

	private RegexUtils() {
	}

	public static String replaceAllVars(ConfigurationManager confManager,
			String expression, boolean isRegex) {
		String last;
		do {
			last = new String(expression);
			String field = RegexUtils.extractFieldFromExpression(expression);
			String fieldValue = (String) confManager.getVar(field);
			if (fieldValue != null) {
				if (isRegex) {
					fieldValue = Pattern.quote(fieldValue);
				}
				expression = expression.replace(VAR_DELIMIT[0] + field
						+ VAR_DELIMIT[1], fieldValue);
			}
		} while (!expression.equals(last));
		return last;
	}

	public static String extractFieldFromExpression(String value) {
		// return value.replaceAll("@(.*)@", "$1");
		// return value.replaceAll(".*@(.*)@.*", "$1");
		return value.replaceAll(".*" + Pattern.quote(VAR_DELIMIT[0]) + "(.*)"
				+ Pattern.quote(VAR_DELIMIT[1]) + ".*", "$1");
	}
}
