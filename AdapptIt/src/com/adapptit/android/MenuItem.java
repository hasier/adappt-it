package com.adapptit.android;

import java.util.Map.Entry;

public class MenuItem<U> implements Entry<String, U> {
	private String key;
	private U value;

	public MenuItem(String key, U value) {
		if (key == null || "".equals(key)) {
			throw new NullPointerException("Key cannot be empty nor null");
		}
		this.key = key;
		setValue(value);
	}

	@Override
	public String getKey() {
		return key;
	}

	@Override
	public U getValue() {
		return value;
	}

	@Override
	public U setValue(U object) {
		U s = value;
		value = object;
		return s;
	}
}