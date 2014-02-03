package com.adapptit.android.webparser;

import android.webkit.JavascriptInterface;

public class DataAccessJSInterface {

	private DataReceiver receiver;

	public DataAccessJSInterface(DataReceiver dr) {
		if (dr == null) {
			throw new NullPointerException("DataReceiver cannot be null");
		}
		receiver = dr;
	}

	@JavascriptInterface
	public void onClickId(String id) {
		receiver.clickId(id);
	}

	@JavascriptInterface
	public void onClickClass(String clazz) {
		receiver.clickClass(clazz);
	}

	@JavascriptInterface
	public void sendFormParameters(String formIdentificationExpr,
			String serializedForm) {
		receiver.loadFormData(formIdentificationExpr, serializedForm);
	}

	@JavascriptInterface
	public void getParameters(final String selector, String params) {
		final String[] split = params.split("\\|item\\|");
		String[] sp;
		for (int i = 0; i < split.length; i++) {
			try {
				sp = split[i].split("\\|val\\|");
				receiver.loadData(sp[0], sp[1]);
			} catch (Exception ex) {
				receiver.loadData("", "");
			}
		}

		//onClickClass(clazz);
		if (selector.startsWith("#")) {
			onClickId(selector.substring(1));
		} else {
			onClickClass(selector.replace('.', ' ').trim());
		}
	}

	public interface DataReceiver {
		public void loadFormData(String formIdentificationExpr,
				String serializedForm);

		public void loadData(String id, String value);

		public void clickClass(String clazz);

		public void clickId(String id);
		
	}
}
