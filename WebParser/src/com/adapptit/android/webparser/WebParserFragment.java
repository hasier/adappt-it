package com.adapptit.android.webparser;

import java.util.EmptyStackException;
import java.util.Stack;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.CookieSyncManager;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.adapptit.android.CustomFragment;
import com.adapptit.android.webparser.DataAccessJSInterface.DataReceiver;
import com.adapptit.android.webparser.HTMLViewJSInterface.DataProcessor;

public class WebParserFragment extends CustomFragment {

	private static final String TAG = "WebParserFragment";

	private HTMLViewJSInterface htmlInterface;
	private WebView controller;
	private WebView view;
	private boolean loading = false;
	private boolean showController = false;

	// public WebParserFragment() {
	// createWebkits();
	// }

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		createWebkits();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		LinearLayout l = new LinearLayout(getActivity());
		l.setLayoutParams(new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		view.setLayoutParams(new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		controller.setLayoutParams(new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		controller.setVisibility(View.GONE);

		l.addView(view);
		l.addView(controller);

		return l;
	}

	@Override
	public boolean onBackPressed() {
		boolean defaultOnBack = false;
		if (getMainActivity().getMenu().isMenuShowing()) {
			getMainActivity().getMenu().showContent();
		} else {
			// try {
			// getConfManager().getNavigationStack().pop();
			// } catch (EmptyStackException ex) {
			// }
			// if (getConfManager().getNavigationStack().empty()) {
			// defaultOnBack = true;
			// } else {
			// // loadControllerUrl(navigationStack.peek(), false);
			// getConfManager().goBack();
			// }
			Stack<String> stack = getConfManager().getNavigationStack();
			boolean cont = true;
			do {
				try {
					stack.pop();
				} catch (EmptyStackException ex) {
				}
				if (stack.empty()) {
					defaultOnBack = true;
					cont = false;
				} else {
					if (stack.peek() != null) {
						getConfManager().goBack();
						cont = false;
					}
				}
			} while (cont && stack.peek() == null);
		}
		return defaultOnBack;
	}

	@Override
	public void onResume() {
		CookieSyncManager.getInstance().startSync();
		super.onResume();
	}

	@Override
	public void onPause() {
		CookieSyncManager.getInstance().stopSync();
		super.onPause();
	}

	public boolean isLoading() {
		return loading;
	}

	public void showController() {
		showController = true;
		view.setVisibility(View.GONE);
		controller.setVisibility(View.VISIBLE);
		getMainActivity().setFullscreen();
	}

	public void checkView() {
		if (showController) {
			view.setVisibility(View.VISIBLE);
			controller.setVisibility(View.GONE);
			getMainActivity().undoFullscreen();
			showController = false;
		}
	}

	@SuppressLint("SetJavaScriptEnabled")
	private void createWebkits() {
		CookieSyncManager.createInstance(getMainActivity());
		view = new WebView(getMainActivity());
		view.getSettings().setJavaScriptEnabled(true);
		view.getSettings().setGeolocationEnabled(false);
		view.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
		view.getSettings().setSaveFormData(false);
		view.getSettings().setSavePassword(false);
		view.getSettings().setPluginState(PluginState.ON);
		view.getSettings()
				.setUserAgentString(
						"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:20.0) Gecko/20100101 Firefox/20.0");
		view.addJavascriptInterface(new DataAccessJSInterface(
				(DataReceiver) getConfManager()), "DataAccess");
		view.setWebViewClient(new WebViewClient() {

			@Override
			public void onPageFinished(WebView view, String url) {
				Log.d(TAG, "Show page finished: " + url);
				loading = false;
				getMainActivity().showContent();
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				Log.d(TAG, "Forward URL: " + url);
				loading = true;
				if (!url.contains((String) getConfManager().getVar(
						WebParserConfigurationManager.INTERNAL_TOKEN))) {
					view.getContext().startActivity(
							new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
					return true;
				}
				((WebParserConfigurationManager) getConfManager())
						.loadControllerUrl(url, true);
				return true;
			}
		});
		view.setWebChromeClient(new WebChromeClient() {
			@Override
			public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
				Log.w(TAG + "/WvView",
						consoleMessage.message() + " at "
								+ consoleMessage.sourceId() + ": "
								+ consoleMessage.lineNumber());
				return true;
			}
		});

		controller = new WebView(getMainActivity());
		controller.getSettings().setJavaScriptEnabled(true);
		controller.getSettings().setGeolocationEnabled(false);
		controller.getSettings()
				.setJavaScriptCanOpenWindowsAutomatically(false);
		controller.getSettings().setSaveFormData(false);
		controller.getSettings().setSavePassword(false);
		controller.getSettings().setPluginState(PluginState.ON);
		// controller.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);
		controller.getSettings().setSupportZoom(true);
		controller.getSettings().setBuiltInZoomControls(true);
		controller.getSettings().setUseWideViewPort(true);
		// controller.setInitialScale(100);
		controller
				.getSettings()
				.setUserAgentString(
						"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:20.0) Gecko/20100101 Firefox/20.0");
		htmlInterface = new HTMLViewJSInterface(
				(DataProcessor) getConfManager());
		controller.addJavascriptInterface(htmlInterface, "HTMLView");
		controller.setWebViewClient(new WebViewClient() {

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				Log.d(TAG, "Override URL load: " + url);
				loading = true;
				return super.shouldOverrideUrlLoading(view, url);
			}

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				Log.d(TAG, "Page started: " + url);
				getMainActivity().hideContent();
				super.onPageStarted(view, url, favicon);
			}

			@Override
			public void onLoadResource(WebView view, String url) {
				Log.d(TAG, "Load resource: " + url);
				((WebParserConfigurationManager) getConfManager()).onLoad(url);
				super.onLoadResource(view, url);
			}

			@Override
			public void doUpdateVisitedHistory(WebView view, String url,
					boolean isReload) {
				Log.d(TAG, "Update visited history: " + url);
				super.doUpdateVisitedHistory(view, url, isReload);
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				Log.d(TAG, "Page finished: " + url);
				CookieSyncManager.getInstance().sync();
				((WebParserConfigurationManager) getConfManager())
						.pageLoaded(url);
			}

		});
		controller.setWebChromeClient(new WebChromeClient() {
			@Override
			public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
				Log.w(TAG + "/WvController",
						consoleMessage.message() + " at "
								+ consoleMessage.sourceId() + ": "
								+ consoleMessage.lineNumber());
				return true;
			}
		});
	}

	public WebBackForwardList getControllerHistory() {
		return controller.copyBackForwardList();
	}

	public HTMLViewJSInterface getHtmlInterface() {
		return htmlInterface;
	}

	public void loadControllerUrl(String url) {
		controller.loadUrl(url);
	}

	public void displayContent(final String baseUrl, final String html) {
		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				// ((WebView) getActivity().findViewById(R.id.view))
				view.loadDataWithBaseURL(baseUrl, html, "text/html", "utf-8",
						null);
			}
		});
	}

}
