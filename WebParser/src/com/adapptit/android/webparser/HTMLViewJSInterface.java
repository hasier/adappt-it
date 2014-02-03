package com.adapptit.android.webparser;

import android.util.Log;
import android.webkit.JavascriptInterface;

public class HTMLViewJSInterface {

	private static final String TAG = "HTMLViewJSInterface";

	private DataProcessor processor;
//	private boolean checkJSRemoval = false;
//	private boolean checkJSAppear = false;
//	private String removeItem;
//	private String appearItem;
	private boolean exit = false;

	public HTMLViewJSInterface(DataProcessor dp) {
		if (dp == null) {
			throw new NullPointerException("DataProcessor cannot be null");
		}
		processor = dp;
	}

//	public void checkJSRemoval(boolean checkJSRemoval) {
//		this.checkJSRemoval = checkJSRemoval;
//	}
//
//	public void checkJSAppear(boolean checkJSAppear) {
//		this.checkJSAppear = checkJSAppear;
//	}
//
//	public void setRemoveItem(String removeItem) {
//		this.removeItem = removeItem;
//	}
//
//	public void setAppearItem(String appearItem) {
//		this.appearItem = appearItem;
//	}

	public void exit() {
		exit = true;
	}

	public boolean isExit() {
		return exit;
	}

	@JavascriptInterface
	public void getHTML(String html) {
		//if (checkJSRemoval) {
		if (processor.getCheckJSRemoval()) {
			//if (html.trim().equals("") || html.contains(removeItem)) {
			if (html.trim().equals("") || html.contains(processor.getRemoveItem())) {
				Log.d(TAG, "Contains item");
				try {
					Thread.sleep(250);
				} catch (InterruptedException e) {
				}
				Thread.yield();
				processor.getHtml();
			} else {
				Log.d(TAG, "OK: Item removed");
				loadPreprocess(html);
			}
		//} else if (checkJSAppear) {
		} else if (processor.getCheckJSAppear()) {
			//if (html.trim().equals("") || !html.contains(appearItem)) {
			if (html.trim().equals("") || !html.contains(processor.getAppearItem())) {
				Log.d(TAG, "Does not contain item");
				try {
					Thread.sleep(250);
				} catch (InterruptedException e) {
				}
				Thread.yield();
				processor.getHtml();
			} else {
				Log.d(TAG, "OK: Item appeared");
				loadPreprocess(html);
			}
		} else {
			loadPreprocess(html);
		}
	}
	
	private void loadPreprocess(final String html) {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				processor.loadPreprocess(html);
			}
		}, "LoadPreprocess").start();
	}

	public interface DataProcessor {
		public void getHtml();

		public void loadPreprocess(String html);

		public boolean getCheckJSRemoval();

		public boolean getCheckJSAppear();

		public String getRemoveItem();

		public String getAppearItem();
	}
}
