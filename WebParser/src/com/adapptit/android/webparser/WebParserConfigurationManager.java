package com.adapptit.android.webparser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.webkit.WebBackForwardList;

import com.adapptit.android.Action;
import com.adapptit.android.ConfigurationManager;
import com.adapptit.android.MainActivity;
import com.adapptit.android.MenuItem;
import com.adapptit.android.PreferencesManager;
import com.adapptit.android.RegexUtils;
import com.adapptit.android.webparser.DataAccessJSInterface.DataReceiver;
import com.adapptit.android.webparser.HTMLViewJSInterface.DataProcessor;
import com.adapptit.android.webparser.actions.AppendAction;
import com.adapptit.android.webparser.actions.AttrAction;
import com.adapptit.android.webparser.actions.MenuAddAction;
import com.adapptit.android.webparser.actions.RemoveAction;
import com.adapptit.android.webparser.actions.RemoveAllButAction;
import com.adapptit.android.webparser.actions.ResetElementAction;
import com.adapptit.android.webparser.actions.SetAction;

public class WebParserConfigurationManager extends ConfigurationManager
		implements DataProcessor, DataReceiver {

	private static final String MATCH_ORDER = "matchOrder";
	private static final String TAG = "WebParserConfigurationManager";

	static {
		Log.d(TAG, "Module loaded");
		ConfigurationManager.addModule("WebParser",
				WebParserConfigurationManager.class);
	}

	public static final String SESSION = "session";
	public static final String LOGIN = "login";
	public static final String LOGOUT = "logout";
	public static final String USER_TOKEN = "userToken";
	public static final String TEMPLATE = "template";
	public static final String INTERNAL_TOKEN = "internalToken";
	public static final String URL_TO_INSERT = "urlToInsert";
	public static final String USER_ID = "userId";
	public static final String APPEAR_ITEM = "appearItem";
	public static final String REMOVE_ITEM = "removeItem";
	public static final String CHECK_JS_REMOVE = "checkJSRemove";
	public static final String CHECK_JS_APPEAR = "checkJSAppear";
	public static final String CURRENT_URL = "currentUrl";
	public static final String VIDEO_SCHEME = "video://";
	public static final String URL_TO_HISTORY = "historyEnabled";

	public static final String DEFAULT = "DEFAULT";
	public static final String CODE_TO_INSERT = "codeToInsert";
	public static final String ONLOAD = "ONLOAD";
	public static final String URLS = "URLS";
	public static final String SHOW = "show";
	public static final String TAG_KEY = "tag";
	public static final String POSITION = "position";
	public static final String ATTR = "attr";
	public static final String TRIGGER = "trigger";
	public static final String VIDEO = "isVideo";

	private WebParserFragment fragment;
	// private ArrayList<String> onLoad = new ArrayList<String>();
	private ArrayList<URLEntry<JSONObject>> onLoad = new ArrayList<URLEntry<JSONObject>>();
	private ArrayList<String> ignore = new ArrayList<String>();
	private ArrayList<String> notHistory = new ArrayList<String>();
	// private boolean stopLoad = false;
	private ArrayList<String> matchOrder = null;

	/**
	 * Maps URL values in the JSON to their regex values
	 */
	// private BiMap<String, String> urlMaps = new BiMap<String, String>();
	private List<URLEntry<String>> urlList = new ArrayList<URLEntry<String>>();

	public WebParserConfigurationManager(JSONObject j, MainActivity a) {
		super(j, a, "com.adapptit.android.webparser.WebParserFragment");

		JSONObject obj;
		try {
			if (!j.isNull(SESSION)) {
				obj = j.getJSONObject(SESSION);
				vars.put(LOGIN, obj.getString(LOGIN));
				vars.put(LOGOUT, obj.getString(LOGOUT));
				vars.put(USER_TOKEN, obj.getString(USER_TOKEN));
				notHistory.add((String) vars.get("login"));
			}
			if (!j.isNull(TEMPLATE)) {
				vars.put(TEMPLATE, j.getString(TEMPLATE));
			}
			vars.put(INTERNAL_TOKEN, j.getString(INTERNAL_TOKEN));
			vars.put(CHECK_JS_APPEAR, "false");
			vars.put(CHECK_JS_REMOVE, "false");
			vars.put(REMOVE_ITEM, "");
			vars.put(APPEAR_ITEM, "");
			vars.put(USER_ID, "");
			vars.put(URL_TO_INSERT, "");
			if (!j.isNull(MATCH_ORDER)) {
				setMatchOrder(j.getJSONArray(MATCH_ORDER));
			}
			obj = j.getJSONObject(URLS);
			addOnLoads(obj);
			if (!obj.isNull("IGNORE")) {
				addIgnores(obj.getJSONArray("IGNORE"));
			}
			parseUrls(obj);
		} catch (JSONException e) {
			Log.e(TAG, e.getMessage(), e);
			// FIXME En caso de error ????
		}
	}

	@Override
	public String checkConditions() {
		StringBuilder sb = new StringBuilder();
		ConnectivityManager connectivityManager = (ConnectivityManager) getActivity()
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager
				.getActiveNetworkInfo();
		if (activeNetworkInfo == null || !activeNetworkInfo.isConnected()) {
			sb.append("There is no internet connection. Please, enable it before continuing");
		}
		return sb.toString();
	}

	@Override
	public List<Action> getActions() {
		ArrayList<Action> a = new ArrayList<Action>();
		a.add(new AttrAction());
		a.add(new MenuAddAction());
		a.add(new RemoveAction());
		a.add(new RemoveAllButAction());
		a.add(new SetAction());
		a.add(new AppendAction());
		a.add(new ResetElementAction());
		return a;
	}

	@Override
	public void start() {
		fragment = (WebParserFragment) getActivity().getFragment();
		loadControllerUrl(getStartUrl(), true);
	}

	@Override
	public void menuClick(Object o) {
		String s = (String) o;
		loadControllerUrl(s, true);
	}

	@Override
	public void goBack() {
		loadControllerUrl(getNavigationStack().peek(), false);
	}

	private void setMatchOrder(JSONArray array) throws JSONException {
		matchOrder = new ArrayList<String>(array.length());
		for (int i = 0; i < array.length(); i++) {
			matchOrder.add(array.getString(i));
		}
	}

	private void addOnLoads(JSONObject urls) {
		@SuppressWarnings("rawtypes")
		Iterator keys = urls.keys();
		String key;
		while (keys.hasNext()) {
			key = (String) keys.next();
			if (key.equals("IGNORE")) {
				continue;
			}
			try {
				JSONArray array = urls.getJSONArray(key);
				for (int i = 0; i < array.length(); i++) {
					JSONObject obj = array.getJSONObject(i);
					if (obj.getString(NAME).equals(ONLOAD)) {
						onLoad.add(new URLEntry<JSONObject>(RegexUtils
								.replaceAllVars(this, key, true), obj));
						i = array.length();
					}
				}
			} catch (JSONException e) {
				Log.e(TAG, e.getMessage(), e);
			}
		}
	}

	private void addIgnores(JSONArray j) throws JSONException {
		for (int i = 0; i < j.length(); i++) {
			ignore.add(j.getString(i));
		}
	}

	private void parseUrls(JSONObject j) throws JSONException {
		@SuppressWarnings("rawtypes")
		Iterator keys = j.keys();
		String key;
		while (keys.hasNext()) {
			key = (String) keys.next();
			if (key.equals(DEFAULT)) {
				continue;
			}
			// String fielded = RegexUtils.extractFieldFromExpression(key);
			String fielded = RegexUtils.replaceAllVars(this, key, true);
			// TODO ???? Object o = vars.get(key);
			// if (o != null) {
			// urlMaps.put(o.toString(), key);
			// } else {
			// urlMaps.put(key, key);
			// }

			// urlMaps.put(fielded, key);
			urlList.add(new URLEntry<String>(fielded, key));

			if (key.equals("IGNORE")) {
				continue;
			}

			JSONArray a = j.getJSONArray(key);
			for (int i = 0; i < a.length(); i++) {
				try {
					if (!a.getJSONObject(i).optString(ELEMENT).equals("")
							&& a.getJSONObject(i).getString(ELEMENT)
									.equals(HISTORY_SAVE)
							&& !Boolean.parseBoolean(a.getJSONObject(i)
									.getString(VALUE))) {
						notHistory.add(fielded);
						break;
					}
				} catch (Exception ex) {
					Log.e(TAG, "Not well formed expression", ex);
				}
			}
		}
	}

	public String getStartUrl() {
		// String userId = PreferencesManager.getInstance().prefs.getString(
		// PreferencesManager.LOGIN_KEY, null);
		String userId = PreferencesManager.getInstance().prefs.getString(
				USER_ID + "_" + getConfName(), null);
		if (userId != null || vars.get(LOGIN) == null) {
			vars.put(USER_ID, userId);
			// this.userId = userId;
			return vars.get(HOME_TAG).toString();
		} else {
			return vars.get(LOGIN).toString();
		}
	}

	@Override
	public void loadFormData(String formIdentificationExpr,
			String serializedForm) {
		// loadControllerJSCommand("javascript: " + formIdentificationExpr
		// + ".deserialize('" + serializedForm + "');");
		// loadControllerJSCommand("javascript: $('" + formIdentificationExpr
		// + "').closest('form').deserialize('" + serializedForm + "');");
		loadControllerJSCommand("javascript: var form = $('"
				+ formIdentificationExpr
				+ "').closest('form'); "
				+ "form.find('input:text, input:password, input:file, select, textarea').val(''); "
				+ "form.find('input:radio, input:checkbox').removeAttr('checked').removeAttr('selected'); "
				+ "form.deserialize('" + serializedForm + "');");
		doClick(formIdentificationExpr);
		// if (formIdentificationExpr.startsWith("#")) {
		// clickId(formIdentificationExpr.substring(1));
		// } else {
		// clickClass(formIdentificationExpr.replace('.', ' ').trim());
		// }
	}

	@Override
	public void loadData(String id, String value) {
		// loadControllerJSCommand("javascript: $('input[name=" + id +
		// "]').val('"
		// + value + "');");
		loadControllerJSCommand("javascript: $('#" + id + "').val('" + value
				+ "');");
	}

	private void doClick(String jqSelector) {
		final int pages = fragment.getControllerHistory().getSize();
		loadControllerJSCommand("javascript: $('" + jqSelector
				+ "').attr('disabled', false);");
		loadController("javascript: $('" + jqSelector + "').click();", true);
		redirectFromClick(pages);
	}

	@Override
	public void clickClass(String clazz) {
		final int pages = fragment.getControllerHistory().getSize();
		loadController("javascript: $('." + clazz.replace(' ', '.')
				+ "').click();", true);
		redirectFromClick(pages);
	}

	@Override
	public void clickId(String id) {
		final int pages = fragment.getControllerHistory().getSize();
		loadController("javascript: $('#" + id + "').click();", true);
		redirectFromClick(pages);
	}

	private void redirectFromClick(final int nPages) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				Log.d(TAG, "Waiting for redirect");
				int timeout = 20;
				int i = 0;
				while (nPages == fragment.getControllerHistory().getSize()) {
					try {
						Thread.sleep(250);
					} catch (InterruptedException e) {
					}
					Thread.yield();
					i++;
					if (i == timeout) {
						break;
					}
				}
				if (i == timeout) {
					Log.d(TAG,
							"No redirection detected. Displaying refreshed page");
					getHtml();
				} else {
					Log.d(TAG, "Redirection detected. Displaying new content");
					if (!fragment.isLoading()) {
						WebBackForwardList w = fragment.getControllerHistory();
						pageLoaded(w.getItemAtIndex(w.getSize() - 1).getUrl());
					}
				}
			}
		}).start();
	}

	// private boolean isStopLoad() {
	// boolean s = stopLoad;
	// stopLoad = false;
	// return s;
	// }

	@Override
	public boolean getCheckJSRemoval() {
		return Boolean.parseBoolean((String) vars.get(CHECK_JS_REMOVE));
	}

	@Override
	public boolean getCheckJSAppear() {
		return Boolean.parseBoolean((String) vars.get(CHECK_JS_APPEAR));
	}

	@Override
	public String getRemoveItem() {
		return (String) vars.get(REMOVE_ITEM);
	}

	@Override
	public String getAppearItem() {
		return (String) vars.get(APPEAR_ITEM);
	}

	@Override
	public void getHtml() {
		// if (isStopLoad()) {
		// return;
		// }
		loadControllerJSCommand("javascript:window.HTMLView.getHTML(new XMLSerializer().serializeToString(document));");
	}

	@Override
	public void loadPreprocess(String html) {
		// if (isStopLoad()) {
		// Log.d(TAG, "Stop requested");
		// return;
		// }
		Log.d(TAG, "HTML load complete");
		String urlToInsert = (String) vars.get(URL_TO_INSERT);
		if (urlToInsert != null) {
			loadControllerUrl(urlToInsert, false);
			return;
		}
		Document d = Jsoup.parse(html);

		boolean show = transformHtml(d);
		// Log.d(TAG, "HTML parse complete");

		Element script = new Element(Tag.valueOf("script"), "");
		script.attr("type", "text/javascript");
		script.attr("src", "file:///android_asset/jquery-1.9.1.min.js");
		d.head().appendChild(script);

		script = new Element(Tag.valueOf("script"), "");
		script.attr("type", "text/javascript");
		script.append("function sendForm(buttonSelector) { window.DataAccess.sendFormParameters(buttonSelector, $(buttonSelector).closest('form').serialize()); }");
		d.body().appendChild(script);

		script = new Element(Tag.valueOf("script"), "");
		script.attr("type", "text/javascript");
		script.append("function sendFieldsById(buttonSelector) { var p = ''; "
				+ "$(buttonSelector).closest('form').find('input').each(function() {"
				+ "if ($(this).attr('type') == 'submit'){ return true;} "
				+ "p = p + $(this).attr('id') + '|val|' + $(this).val() + '|item|'; }); "
				+ "window.DataAccess.getParameters(buttonSelector, p);}");
		d.body().appendChild(script);

		getActivity().setSubtitle();

		if (vars.get(LOGOUT) != null) {
			getMenuActions()
					.add(new MenuItem<String>("Logout", vars.get(LOGOUT)
							.toString()));
		}

		if (show) {
			displayContent(d.html());
		} else {
			loadControllerUrl((String) vars.get(TEMPLATE), false);
		}
	}

	private boolean saveToHistory(String url) {
		for (String s : notHistory) {
			if (url.matches(s)) {
				return false;
			}
		}
		return true;
	}

	public void pageLoaded(String url) {
		if (setVariables(url, fragment.getHtmlInterface())) {
			return;
		}
		{
			String orientation = (String) vars.get(ORIENTATION);
			if (!"".equals(orientation)) {
				if (orientation.equals("landscape")) {
					fragment.getActivity()
							.setRequestedOrientation(
									android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				} else if (orientation.equals("portrait")) {
					fragment.getActivity()
							.setRequestedOrientation(
									android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				}
			} else {
				fragment.getActivity()
						.setRequestedOrientation(
								android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR);
			}
		}

		// {OK
		// String u = (String) vars.get(URL_TO_HISTORY);
		// // if (u != null
		// // && Boolean.parseBoolean((String) vars.get(HISTORY_SAVE))) {
		// // getNavigationStack().push(u);
		// // } else {
		// // getNavigationStack().push(null);
		// // }
		//
		// // if (!Boolean.parseBoolean((String) vars.get(HISTORY_SAVE))) {
		// // getNavigationStack().push(null);
		// // } else if (u != null) {
		// // getNavigationStack().push(u);
		// // }
		// if (!saveToHistory(url)) {
		// getNavigationStack().push(null);
		// } else if (u != null) {
		// getNavigationStack().push(u);
		// }
		// }
		if (fragment.getHtmlInterface().isExit()) {
			if (getActivity().getMenu().isMenuShowing()) {
				getActivity().onBackPressed();
			}
			getActivity().onBackPressed();
			return;
		}
		loadControllerJSCommand("javascript: var oHead = document.getElementsByTagName('head').item(0); var oScript= document.createElement('script'); oScript.type = \"text/javascript\"; oScript.src=\"http://code.jquery.com/jquery-1.9.1.min.js\"; oHead.appendChild(oScript);");
		{
			boolean isVideo = Boolean.parseBoolean((String) vars.get(VIDEO));
			if (isVideo) {
				fragment.showController();
				// loadControllerJSCommand("javascript: $('body').contents(':not(video)').remove(); $('*').css('min-width', '0%'); $('*').css('max-width', '100%');");
				// <meta name="viewport"
				// content="user-scalable=no, width=device-width, initial-scale=1">
				loadControllerJSCommand("javascript: $('*').css('min-width', '0%'); $('*').css('max-width', '100%');");
				getActivity().showContent();
				return;
			} else {
				fragment.checkView();
			}
		}
		loadControllerJSCommand("javascript: var oHead = document.getElementsByTagName('head').item(0); var oScript= document.createElement('script'); oScript.type = \"text/javascript\"; oScript.src=\"https://raw.github.com/kflorence/jquery-deserialize/master/src/jquery.deserialize.js\"; oHead.appendChild(oScript);");
		getHtml();
	}

	public void loadControllerUrl(final String url, boolean history) {
		// if (history && saveToHistory(url)) {

		// OK if (history) {
		// vars.put(URL_TO_HISTORY, url);
		// } else {
		// vars.put(URL_TO_HISTORY, null);
		// }

		if (history) {
			if (saveToHistory(url)) {
				getNavigationStack().push(url);
			} else {
				getNavigationStack().push(null);
			}
		}

		// if (history &&
		// Boolean.parseBoolean(vars.get(HISTORY_SAVE).toString())) {
		// getNavigationStack().push(url);
		// }
		loadController(url, true);
	}

	public void loadControllerJSCommand(final String command) {
		loadController(command, false);
	}

	private void loadController(final String url, final boolean hide) {
		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (hide) {
					getActivity().hideContent();
				}
				fragment.loadControllerUrl(url);
			}
		});
	}

	private void displayContent(final String html) {
		// fragment.displayContent(getCurrentUrl(), html);
		fragment.displayContent(vars.get(CURRENT_URL).toString(), html);
	}

	public void onLoad(String url) {
		boolean enters = false;
		try {
			for (URLEntry<JSONObject> s : onLoad) {
				if (url.matches(s.getKey())) {
					enters = true;
					Log.d(TAG,
							"URL '" + url + "' matches onLoad pattern '"
									+ s.getKey() + "'");
					JSONArray acts = s.getValue().getJSONArray(ELEMENT);
					for (int i = 0; i < acts.length(); i++) {
						JSONObject action = acts.getJSONObject(i);
						String name = action.getString(NAME);
						Action a = actions.get(name);
						if (a == null) {
							Log.e(TAG,
									"There is no action with name '"
											+ name
											+ "', the application may behave unexpectedly");
						} else {
							Log.v(TAG, "Executing '" + name + "' action");
							a.doAction(this, action, url);
						}
					}
				}
			}
		} catch (JSONException ex) {
			Log.e(TAG, "An unexpected error happened", ex);
		}
		if (enters) {
			Log.d(TAG, "On load finished");
		}
	}

	// public void onLoad(String url) {
	// boolean enters = false;
	// try {
	// JSONObject urls = json.getJSONObject(URLS);
	// for (String s : onLoad) {
	// String pattern = RegexUtils.replaceAllVars(this, s);
	// Log.v(TAG, "PATTERN: " + pattern);
	// if (url.matches(pattern)) {
	// enters = true;
	// Log.d(TAG, "URL '" + url + "' matches onLoad pattern '" + s
	// + "'");
	// JSONArray array = urls.getJSONArray(s);
	// for (int i = 0; i < array.length(); i++) {
	// JSONObject key = array.getJSONObject(i);
	// if (key.getString(NAME).equals(ONLOAD)) {
	// JSONArray olActions = key.getJSONArray(ELEMENT);
	// for (int j = 0; j < olActions.length(); j++) {
	// JSONObject action = array.getJSONObject(j);
	// String name = action.getString(NAME);
	// Action a = actions.get(name);
	// if (a == null) {
	// Log.e(TAG,
	// "There is no action with name '"
	// + name
	// + "', the application may behave unexpectedly");
	// } else {
	// Log.v(TAG, "Executing '" + name
	// + "' action");
	// a.doAction(this, action, url);
	// }
	// }
	// i = array.length();
	// }
	// }
	// }
	// }
	// } catch (JSONException ex) {
	// Log.e(TAG, "An unexpected error happened", ex);
	// }
	// if (enters) {
	// Log.d(TAG, "On load finished");
	// }
	// }

	// private List<String> urlMatching(JSONObject urls, String url) {
	// ArrayList<String> list = new ArrayList<String>();
	// String key = urlMaps.get(url);
	// if (key != null && !urls.isNull(key)) {
	// list.add(key);
	// }
	// // key = getMatchKeyFromUrl(urls, url);
	// list.addAll(getMatchKeysFromUrl(urls, url));
	// if (list.isEmpty()) {
	// list.add("DEFAULT");
	// }
	// return list;
	// }

	private List<String> getMatchKeysFromUrl(JSONObject root, String url) {
		ArrayList<String> list = new ArrayList<String>();
		// Iterator<?> keys = root.keys();

		// Set<String> keys = urlMaps.keySet();
		// for (String s : keys) {
		// if (url.matches(s)) {
		// list.add(urlMaps.get(s));
		// }
		// }
		for (URLEntry<String> u : urlList) {
			if (url.matches(u.getKey())) {
				list.add(u.getValue());
			}
		}
		if (matchOrder != null && !matchOrder.isEmpty()) {
			String[] array = new String[matchOrder.size()];
			ArrayList<String> orig = list;
			list = new ArrayList<String>();
			for (int i = 0; i < matchOrder.size(); i++) {
				String s = matchOrder.get(i);
				int index;
				if ((index = orig.indexOf(s)) >= 0) {
					array[i] = s;
					orig.remove(index);
				}
			}
			for (int i = 0; i < array.length; i++) {
				if (array[i] != null) {
					list.add(array[i]);
				}
			}
			for (String s : orig) {
				list.add(s);
			}
		}
		return list;
	}

	private boolean isIgnore(String url) {
		for (String s : ignore) {
			if (url.matches(s)) {
				Log.d(TAG, "Ignoring url '" + url + "' matching '" + s
						+ "' pattern");
				return true;
			}
		}
		return false;
	}

	/**
	 * 
	 * @param url
	 * @return True if the current URL should be ignored; false otherwise
	 */
	private boolean setVariables(String url, HTMLViewJSInterface jsi) {
		if (isIgnore(url)) {
			return true;
		}
		if (url.equals(vars.get(LOGOUT))) {
			jsi.exit();
		}
		if (url.equals(vars.get(LOGIN))) {
			vars.put(HISTORY_SAVE, "false");
		} else {
			vars.put(HISTORY_SAVE, "true");
		}
		vars.put(URL_TO_INSERT, null);
		vars.put(SUB_TXT, "");
		vars.put(SUB_IMG, "");
		vars.put(CURRENT_URL, url);
		vars.put(UPDATE_SUB, "false");
		vars.put(ORIENTATION, "");
		vars.put(VIDEO, "false");
		getMenuActions().clear();
		try {
			JSONObject urls = json.getJSONObject(URLS);

			// String key = urlMatching(urls, url);
			List<String> keys = getMatchKeysFromUrl(urls, url);

			// if (urls.isNull(key)) {
			if (keys.isEmpty()) {
				Log.w(TAG,
						"The url "
								+ url
								+ " has no rules to set variables. Default behavior is applied");
				if (!urls.isNull(DEFAULT)) {
					keys.add(DEFAULT);
				}
			}
			for (int j = 0; j < keys.size(); j++) {
				String key = keys.get(j);
				Log.d(TAG, "Setting variables for url '" + url + "' matching '"
						+ key + "' pattern");
				JSONArray array = urls.getJSONArray(key);
				for (int i = 0; i < array.length(); i++) {
					try {
						JSONObject action = array.getJSONObject(i);
						String name = action.getString(NAME);
						if (!name.equals(TRANSFORM) && !name.equals(ONLOAD)) {
							Action a = actions.get(name);
							if (a == null) {
								Log.e(TAG,
										"There is no action with name '"
												+ name
												+ "' when setting variables, the application may behave unexpectedly");
							} else {
								Log.v(TAG, "Executing '" + name + "' action");
								try {
									a.doAction(this, action, url);
								} catch (Exception ex) {
									Log.e(TAG, "An error occurred executing '"
											+ name + "' action", ex);
								}
							}
						}
					} catch (JSONException e) {
						Log.e(TAG,
								"Error parsing action. The process will continue",
								e);
					}
				}
			}
		} catch (JSONException e) {
			Log.e(TAG, "The configuration file is not correctly formatted", e);
		}
		Log.d(TAG, "Variable setting finished");
		return false;
	}

	/**
	 * 
	 * @param d
	 * @return True if the call to transformHtml should show the result; false
	 *         otherwise
	 */
	private boolean transformHtml(Document d) {
		vars.put(SHOW, "true");
		try {
			String url = (String) vars.get(CURRENT_URL);
			JSONObject urls = json.getJSONObject(URLS);

			// String key = urlMatching(urls, url);
			List<String> keys = getMatchKeysFromUrl(urls, url);

			// if (urls.isNull(key)) {
			if (keys.isEmpty()) {
				Log.w(TAG,
						"The url "
								+ url
								+ " has no rules to transform. Default behavior is applied");
				if (!urls.isNull(DEFAULT)) {
					keys.add(DEFAULT);
				}
			}
			for (int j = 0; j < keys.size(); j++) {
				String key = keys.get(j);
				if (key.contains("{{template}}")) {
					d.body().html((String) vars.get(CODE_TO_INSERT));
					vars.put(CODE_TO_INSERT, null);
				}
				JSONArray root = urls.getJSONArray(key);
				for (int i = 0; i < root.length(); i++) {
					try {
						JSONObject transform = root.getJSONObject(i);
						if (transform.getString(NAME).equals(TRANSFORM)) {
							Log.d(TAG, "Applying transformation for url '"
									+ url + "' matching '" + key + "' pattern");
							JSONArray array = transform.getJSONArray(VALUE);
							for (int k = 0; k < array.length(); k++) {
								JSONObject action = array.getJSONObject(k);
								String name = action.getString(NAME);
								Action a = actions.get(name);
								if (a == null) {
									Log.e(TAG,
											"There is no action with name '"
													+ name
													+ "' when transforming, the application may behave unexpectedly");
								} else {
									Log.v(TAG, "Executing '" + name
											+ "' action");
									try {
										a.doAction(this, action, url, d.body());
										// Log.v(TAG + "/toString",
										// d.toString().contains("MathJax") +
										// "");
										// {
										// String html = d.toString();
										// if (html.contains("MathJax")) {
										// Log.i(TAG, html);
										// int w = html.indexOf("MathJax");
										// Log.i(TAG, html.substring(w - 100, w
										// + 100));
										// }
										// }
									} catch (Exception ex) {
										Log.e(TAG,
												"An error occurred executing '"
														+ name + "' action", ex);
									}
								}
							}
							i = root.length();
						}
					} catch (JSONException e) {
						Log.e(TAG,
								"Error parsing action. The process will continue",
								e);
					}
				}
			}
		} catch (JSONException e) {
			Log.e(TAG, "The configuration file is not correctly formatted", e);
		}
		Log.d(TAG, "Transformation finished");
		return Boolean.parseBoolean((String) vars.get(SHOW));
	}

	private class URLEntry<T> implements Entry<String, T> {

		private String key;
		private T value;

		public URLEntry(String key, T value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public String getKey() {
			return key;
		}

		@Override
		public T getValue() {
			return value;
		}

		@Override
		public T setValue(T object) {
			T s = value;
			value = object;
			return s;
		}

	}

}
