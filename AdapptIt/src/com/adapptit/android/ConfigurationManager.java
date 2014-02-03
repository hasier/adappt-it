package com.adapptit.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import com.adapptit.android.actions.CustomClassAction;
import com.adapptit.android.actions.DeleteAction;
import com.adapptit.android.actions.MenuAddAction;
import com.adapptit.android.actions.SaveAction;
import com.adapptit.android.actions.SetAction;

import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;

public abstract class ConfigurationManager {

	private static final String TAG = "ConfigurationManager";
	private static HashMap<String, Class<?>> modules = new HashMap<String, Class<?>>();
	private static HashMap<String, ClassLoader> loaders = new HashMap<String, ClassLoader>();
	private static ClassLoader currentLoader = null;

	public static final String MODULE = "module";
	public static final String LOGO_IMG = "logoImg";
	public static final String LOGO_TXT = "logoTxt";
	public static final String HOME_TAG = "home";
	public static final String UPDATE_SUB = "updateSub";
	public static final String SUB_TXT = "subTxt";
	public static final String SUB_IMG = "subImg";
	public static final String HISTORY_SAVE = "historySave";

	public static final String TRANSFORM = "TRANSFORM";
	public static final String NAME = "name";
	public static final String VALUE = "value";
	public static final String ELEMENT = "element";
	public static final String SOURCE = "source";
	public static final String KEY = "key";
	public static final String ORIENTATION = "orientation";

	private String currentConf = null;
	public final String FRAGMENT;
	private final MainActivity activity;
	private final Stack<String> navigationStack = new Stack<String>();
	protected final JSONObject json;
	protected final HashMap<String, Object> vars = new HashMap<String, Object>();
	private ArrayList<MenuItem<?>> menuActions = new ArrayList<MenuItem<?>>();
	protected final HashMap<String, Action> actions = new HashMap<String, Action>();
	private final HashMap<String, Class<?>> customClasses = new HashMap<String, Class<?>>();

	public abstract void menuClick(Object o);

	public abstract void start();

	public abstract void goBack();

	public abstract List<Action> getActions();

	public abstract String checkConditions();

	public ConfigurationManager(JSONObject j, final MainActivity a,
			String fragment) {
		json = j;
		activity = a;
		FRAGMENT = fragment;
		addActions();
		String s;
		try {
			setClassLoader(j.getString("module"));
			s = j.getString(LOGO_IMG);
			vars.put(LOGO_IMG, s);
			s = j.getString(LOGO_TXT);
			vars.put(LOGO_TXT, s);
			s = j.getString(HOME_TAG);
			vars.put(HOME_TAG, s);
			vars.put(UPDATE_SUB, "true");
			vars.put(SUB_TXT, "");
			vars.put(SUB_IMG, "");
			vars.put(HISTORY_SAVE, "true");
		} catch (JSONException e) {
			Log.e(TAG, e.getMessage(), e);
			new AlertDialog.Builder(a).setCancelable(false)
					.setTitle(R.string.error)
					.setMessage(R.string.view_load_error)
					.setIcon(R.drawable.cross)
					.setPositiveButton(R.string.accept, new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
							a.onBackPressed();
						}
					}).create().show();
			return;
		}
	}
	
	void setConfigurationName(String name) {
		if (currentConf == null) {
			currentConf = name;
		}
	}
	
	public String getConfName() {
		return currentConf;
	}

	private void addActions() {
		addOwnActions();
		List<Action> l = this.getActions();
		if (l != null) {
			for (Action a : l) {
				String name = a.getActionName();
				if (actions.containsKey(name)) {
					Log.w(TAG, "Tag '" + name + "' is being overriden");
				}
				actions.put(name, a);
			}
		}
	}

	private void addOwnActions() {
		Action a = new DeleteAction();
		actions.put(a.getActionName(), a);
		a = new MenuAddAction();
		actions.put(a.getActionName(), a);
		a = new SaveAction();
		actions.put(a.getActionName(), a);
		a = new SetAction();
		actions.put(a.getActionName(), a);
		a = new CustomClassAction();
		actions.put(a.getActionName(), a);
	}

	public MainActivity getActivity() {
		return activity;
	}

	public Stack<String> getNavigationStack() {
		return navigationStack;
	}

	public Bitmap getLogo() throws IOException {
		String l = (String) vars.get(LOGO_IMG);
		return getImage(l);
	}

	public String getLogoText() {
		return (String) vars.get(LOGO_TXT);
	}

	public Bitmap getSubtitle() throws IOException {
		String s = (String) vars.get(SUB_IMG);
		// dirtySubtitle = false;
		vars.put(UPDATE_SUB, "false");
		return getImage(s);
	}

	private Bitmap getImage(String url) throws IOException {
		if (url == null || url.equals("")) {
			return null;
		}
		URL u = new URL(url);
		HttpURLConnection connection = (HttpURLConnection) u.openConnection();
		InputStream is = connection.getInputStream();
		return BitmapFactory.decodeStream(is);
	}

	public String getSubtitleText() {
		// dirtySubtitle = false;
		vars.put(UPDATE_SUB, "false");
		return (String) vars.get(SUB_TXT);
	}

	// public void setDirtySubtitle() {
	// dirtySubtitle = true;
	// }

	public boolean isDirtySubtitle() {
		// return dirtySubtitle;
		return Boolean.parseBoolean((String) vars.get(UPDATE_SUB));
	}

	public Object getVar(String var) {
		return vars.get(var);
	}

	public void setVar(String var, Object value) {
		vars.put(var, value);
	}

	public Class<?> getCustomClass(String name) {
		return customClasses.get(name);
	}

	public void setCustomClass(String name, Class<?> clazz) {
		customClasses.put(name, clazz);
	}

	public static void addModule(String name, Class<?> clazz) {
		modules.put(name, clazz);
	}

	public static boolean isModuleLoaded(String name) {
		return modules.containsKey(name);
	}

	public static void loadModules(Context ctx) {
		File[] files = new File(Environment.getExternalStorageDirectory()
				+ "/AdapptIt/modules").listFiles();
		for (File jarFile : files) {
			try {
				ClassLoader classLoader = new DexClassLoader(
						jarFile.getAbsolutePath(), ctx.getCacheDir()
								.getAbsolutePath(), null,
						ConfigurationManager.class.getClassLoader());
//				JarFile jar = new JarFile(jarFile);
//				Manifest mf = jar.getManifest();
//				Attributes attr = mf.getMainAttributes();
//				String clazz = attr.getValue("Main-Class");
//				classLoader.loadClass(clazz);
//				Class.forName(clazz, true, classLoader);
				DexFile dex = DexFile.loadDex(
						jarFile.getAbsolutePath(),
						new File(ctx.getCacheDir().getAbsolutePath(), jarFile
								.getName() + ".dex").getAbsolutePath(), 0);
				for (Enumeration<String> classNames = dex.entries(); classNames
						.hasMoreElements();) {
					String className = classNames.nextElement();
					classLoader.loadClass(className);
					Class.forName(className, true, classLoader);
				}
				loaders.put(
						jarFile.getName().substring(0,
								jarFile.getName().length() - 4), classLoader);
			} catch (Exception ex) {
				Log.e(TAG, ex.getMessage(), ex);
				new AlertDialog.Builder(ctx)
						.setCancelable(false)
						.setTitle(R.string.error)
						.setMessage(
								ctx.getString(R.string.module_load_error,
										jarFile.getName()))
						.setIcon(R.drawable.cross)
						.setPositiveButton(R.string.accept,
								new OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										dialog.cancel();
									}
								}).create().show();
				return;
			}
		}
		// Class.forName("com.example.pfg.webparser.WebParserConfigurationManager");
	}

	private static void setClassLoader(String name) {
		currentLoader = loaders.get(name);
	}

	public static ClassLoader getCurrentClassLoader() {
		return currentLoader;
	}
	
	static Collection<ClassLoader> getClassLoaders() {
		return loaders.values();
	}

	public static ConfigurationManager getConfigManager(String file,
			MainActivity a) throws Exception {
		return importConfig(file, a);
	}

	private static ConfigurationManager importConfig(String file, MainActivity a)
			throws Exception {
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new FileReader(new File(
					Environment.getExternalStorageDirectory()
							+ "/AdapptIt/configs/" + file)));
			String s;
			StringBuilder sb = new StringBuilder();
			while ((s = bufferedReader.readLine()) != null) {
				sb.append(s).append('\n');
			}
			try {
				bufferedReader.close();
			} catch (IOException e1) {
				Log.e(TAG, e1.getMessage(), e1);
			}

			JSONObject main = new JSONObject(sb.toString());
			String module = main.getString(MODULE);
			Class<?> clazz = modules.get(module);
			if (clazz != null) {
				return (ConfigurationManager) clazz.getConstructor(
						JSONObject.class, MainActivity.class).newInstance(main,
						a);
			} else {
				throw new IllegalStateException("The module '" + module
						+ "' is not loaded");
			}
		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException e1) {
					Log.e(TAG, e1.getMessage(), e1);
				}
			}
			throw e;
		}
	}

	// public final String INTERNAL_URL_TOKEN = "coursera.org";
	// public final String LOGIN = "https://www.coursera.org/account/signin";
	// public final String LOGOUT =
	// "https://www.coursera.org/maestro/api/user/logout";
	// public final String HOME = "https://www.coursera.org/";
	// public final String LOGO_URL =
	// "https://dt5zaw6a98blc.cloudfront.net/site-static/pages/home/template/coursera_logo_small.png";
	// public final String LOGO_TEXT = "";
	// public final String TEMPLATE_URL =
	// "https://www.coursera.org/universities";
	// public final String USER_TOKEN =
	// "https://www.coursera.org/maestro/api/topic/list_my?user_id=";
	//
	// private String currentUrl;
	// private String userId;
	// private String codeToInsert;
	// private String urlToInsert;
	// private String intermediateUrlTokenIgnore;
	//
	// private String subtitleUrl;
	// private String subtitleText;
	// private boolean dirtySubtitle = true;

	//
	// public boolean saveToHistory(String url) {
	// if (url == null || url.equals("") || url.equals(LOGIN)
	// || url.contains("quiz/attempt?quiz_id=")) {
	// return false;
	// }
	// return true;
	// }
	//
	// // public void onLoad(String url) {
	// // if (url.startsWith(USER_TOKEN)) {
	// // setUserId(url.substring(USER_TOKEN.length()));
	// // }
	// // }
	//
	// /**
	// *
	// * @param url
	// * @param i
	// * @return True if the current URL should be ignored; false otherwise
	// */
	// public boolean setVariables(String url, HTMLViewJSInterface i) {
	// urlToInsert = null;
	// subtitleText = "";
	// subtitleUrl = "";
	// menuActions.clear();
	// if (LOGOUT != null) {
	// menuActions.add(new MenuItem("Logout", LOGOUT));
	// }
	// if (url.equals(TEMPLATE_URL)) {
	// i.checkJSRemoval(false);
	// i.checkJSAppear(false);
	// } else if (url.equals(LOGIN)) {
	// dirtySubtitle = true;
	// i.checkJSRemoval(true);
	// i.checkJSAppear(false);
	// i.setRemoveItem("id=\"coursera-loading-js\"");
	// menuActions.clear();
	// } else if (url.equals(HOME)) {
	// // i.checkJSRemoval(false);
	// // i.checkJSAppear(true);
	// // i.setAppearItem("Your courses");
	// dirtySubtitle = true;
	// urlToInsert = USER_TOKEN + userId;
	// } else if (url.startsWith(USER_TOKEN)) {
	// i.checkJSRemoval(false);
	// i.checkJSAppear(false);
	// PreferencesManager.getInstance().prefs.edit()
	// .putString(PreferencesManager.LOGIN_KEY, userId).commit();
	// } else if (url.startsWith("https://class.coursera.org/")) {
	// if (url.contains("/auth/login_receiver")) {
	// return true;
	// }
	// i.checkJSRemoval(false);
	// i.checkJSAppear(false);
	// menuActions.add(0, new MenuItem("Home", HOME));
	// } else if (url.equals(LOGOUT)) {
	// i.checkJSRemoval(false);
	// i.checkJSAppear(false);
	// PreferencesManager.getInstance().prefs.edit()
	// .remove(PreferencesManager.LOGIN_KEY).commit();
	// i.exit();
	// } else {
	// throw new IllegalArgumentException("The url " + url
	// + " is not valid");
	// }
	// currentUrl = url;
	// return false;
	// }
	//
	// /**
	// *
	// * @param d
	// * @return True if the call to transformHtml should show the result; false
	// * otherwise
	// */
	// public boolean transformHtml(Document d) {
	// boolean show = true;
	// if (currentUrl.equals(TEMPLATE_URL)) {
	// d.body().html(codeToInsert);
	// d.body().attr("style",
	// "padding: 30px; padding-top: 10px; padding-right: 5px;");
	// d.body().attr("onload", "$('*').css('max-width','100%');");
	// } else if (currentUrl.equals(LOGIN)) {
	// Element target = d.body()
	// .getElementsByClass("coursera-signin-form").first();
	// Elements all = d.body().getAllElements();
	//
	// Elements not = new Elements();
	// not.add(target);
	// not.addAll(target.getAllElements());
	// not.removeAll(d.body().getElementsByClass("internal-home"));
	// while (target.parent() != null) {
	// target = target.parent();
	// not.add(target);
	// }
	//
	// for (Element e : all) {
	// if (!not.contains(e)) {
	// e.remove();
	// }
	// }
	// String clazz = "btn btn-success coursera-signin-button";
	// Element e = d.body().getElementsByAttributeValue("class", clazz)
	// .get(0);
	//
	// e.attr("onclick", "sendParameters();");
	//
	// Element script = new Element(Tag.valueOf("script"), "");
	// script.attr("type", "text/javascript");
	// script.append("function sendParameters() { var p = ''; "
	// + "$('."
	// + clazz.replace(' ', '.')
	// + "').closest('form').find('input').each(function() {"
	// + "if ($(this).attr('type') == 'submit'){ return true;} "
	// // +
	// //
	// "p = p + $(this).attr('id') + '|val|' + $(this).val() + '|item|'; }); "
	// +
	// "p = p + $(this).attr('name') + '|val|' + $(this).val() + '|item|'; }); "
	// + "window.DataAccess.getParameters('" + clazz + "', p);}");
	// d.body().appendChild(script);
	// d.body()
	// .attr("onload",
	// "$('.span6 div').attr('style', ''); $('.span6').attr('style', ''); $('*').css('max-width', '100%');");
	// } else if (currentUrl.startsWith(USER_TOKEN)) {
	// String s = d.body().child(0).text();
	// try {
	// JSONArray j = new JSONArray(s);
	// Element external = new Element(Tag.valueOf("div"), "");
	// external.attr("class", "coursera-full-canvas");
	// for (int i = 0; i < j.length(); i++) {
	// JSONObject o = (JSONObject) j.get(i);
	// if (!o.getBoolean("display")) {
	// continue;
	// }
	// Element element = new Element(Tag.valueOf("div"), "");
	// element.attr("class", "row");
	// element.attr("style",
	// "border-bottom:solid #d9d9d9 1px; padding: 5px;");
	//
	// Element e = new Element(Tag.valueOf("h3"), "");
	// e.text(o.getString("name"));
	// element.appendChild(e);
	// element.appendElement("br");
	//
	// JSONObject inner = o.getJSONArray("courses").getJSONObject(
	// 0);
	// try {
	// e = new Element(Tag.valueOf("span"), "");
	// StringBuilder sb = new StringBuilder("Start: ");
	// String temp = inner.getString("start_year");
	// if (temp == null || temp.equals("")
	// && !temp.equals("null")) {
	// sb.append("To be announced");
	// } else {
	// String temp2 = inner.getString("start_day");
	// if (temp2 != null && !temp2.equals("")
	// && !temp2.equals("null")) {
	// sb.append(temp2).append("/");
	// }
	// temp2 = inner.getString("start_month");
	// if (temp2 != null && !temp2.equals("")
	// && !temp2.equals("null")) {
	// sb.append(temp2).append("/");
	// }
	// sb.append(temp);
	// }
	// temp = inner.getString("duration_string");
	// if (temp != null && !temp.equals("")
	// && !temp.equals("null")) {
	// sb.append(" - Duration: " + temp);
	// }
	// e.text(sb.toString());
	// element.appendChild(e);
	// element.appendElement("br");
	// } catch (Exception ex) {
	// Log.e(TAG, ex.getMessage(), ex);
	// }
	//
	// e = new Element(Tag.valueOf("a"), "");
	// e.attr("href", inner.getString("home_link")
	// + "auth/auth_redirector?type=login&subtype=normal");
	// e.attr("class", "btn btn-success coursera-course-button");
	// e.text("Go to class");
	// element.appendChild(e);
	//
	// external.appendChild(element);
	// }
	// codeToInsert = external.html();
	// } catch (JSONException e) {
	// codeToInsert = "An error happened. Try again later";
	// Log.e(TAG, e.getMessage(), e);
	// }
	// //
	// show = false;
	// } else if (currentUrl.startsWith("https://class.coursera.org/")) {
	// String html = d.html();
	// d.body()
	// .getElementsByAttributeValue("class",
	// "coursera-browser-banner").remove();
	// d.body().getElementsByTag("script").remove();
	// {
	// Element el = d.getElementById("MathJax_Message");
	// if (el != null) {
	// el.remove();
	// }
	// }
	// d.getElementsByAttributeValueContaining("src", "MathJax").remove();
	// if (d.getElementById("course-page-sidebar") != null) {
	// int i = 1;
	// Element sidebar = d.getElementById("course-page-sidebar");
	//
	// Elements es = sidebar.getElementsByAttributeValueContaining(
	// "href", "/lecture/index");
	// for (Element e : es) {
	// menuActions.add(i, new MenuItem(e.text(), e.attr("href")));
	// i++;
	// }
	//
	// es = sidebar.getElementsByAttributeValueContaining("href",
	// "/quiz/index");
	// for (Element e : es) {
	// menuActions.add(i, new MenuItem(e.text(), e.attr("href")));
	// i++;
	// }
	// }
	//
	// if (currentUrl.endsWith("class/index")) {
	// dirtySubtitle = true;
	//
	// boolean sub = setSubtitle(d, "course-topbanner-logo-name");
	// if (!sub) {
	// sub = setSubtitle(d,
	// "course-topbanner-name coursera-university-color");
	// }
	//
	// try {
	// d.body()
	// .getElementsByAttributeValue("class",
	// "course-overview-upcoming-column").remove();
	// } catch (NullPointerException ex) {
	// }
	//
	// Element e = d.body().getElementById("course-page-content");
	// try {
	// e.getElementsByAttributeValue("class",
	// "course-page-sidebar").remove();
	// } catch (NullPointerException ex) {
	// }
	//
	// d.body().html(e.html());
	// } else if (currentUrl.contains("/lecture/index")) {
	// Element e = d.body().getElementById("course-page-content");
	// try {
	// e.getElementsByAttributeValue("class",
	// "course-page-sidebar").remove();
	// } catch (NullPointerException ex) {
	// }
	//
	// e.getElementsByAttributeValue("class", "course-lectures-list")
	// .first().getElementsByTag("p").first().remove();
	//
	// e.getElementsByAttributeValue("class",
	// "course-lecture-item-resource").remove();
	//
	// for (Element el : e.getElementsByAttributeValue("class",
	// "lecture-link")) {
	// // el.attr("href",
	// // MainActivity.VIDEO_SCHEME
	// // + el.attr("data-modal-iframe")
	// // .replace("view?lecture",
	// // "download.mp4?lecture")
	// // .replace("https://", "http://"));
	// el.attr("href",
	// MainActivity.VIDEO_SCHEME
	// + el.attr("data-modal-iframe"));
	// // Probar con url a home de curso, a ver si es por
	// // autenticacion
	// }
	//
	// d.body().html(e.html());
	// } else if (currentUrl.contains("/quiz/index")) {
	// Element e = d.body().getElementById("course-page-content");
	// try {
	// e.getElementsByAttributeValue("class",
	// "course-page-sidebar").remove();
	// } catch (NullPointerException ex) {
	// }
	//
	// try {
	// e.getElementsByAttributeValueStarting("id", "btn_gensym")
	// .remove();
	// } catch (NullPointerException ex) {
	// }
	// try {
	// e.getElementsByAttributeValueStarting("id",
	// "element_gensym").remove();
	// } catch (NullPointerException ex) {
	// }
	//
	// d.body().html(e.html());
	// } else if (currentUrl.contains("quiz/start?quiz_id=")) {
	// Element e = d.body().getElementById("course-page-content");
	// d.body().html(e.html());
	// } else if (currentUrl.contains("quiz/attempt?quiz_id=")) {
	// Element e = d.body().getElementById("course-page-content");
	//
	// Element script = new Element(Tag.valueOf("script"), "");
	// script.attr("type", "text/javascript");
	// script.append("$('input:checkbox:last').attr('disabled', true); $('input:checkbox:last').attr('checked', true); $('.btn.btn-success').attr('disabled', false); //$('body :not(input:checkbox:last)').attr('disabled', false);");
	// d.body()
	// .getElementsByAttributeValue("class",
	// "course-quiz-submit-button-container").get(0)
	// .appendChild(script);
	//
	// String form = "$('.btn.btn-success').closest('form')";
	// d.body()
	// .getElementsByAttributeValue("class", "btn btn-success")
	// .attr("onclick",
	// "window.DataAccess.sendFormParameters('btn btn-success', \""
	// + form + "\", " + form
	// + ".serialize());");
	//
	// form = "$('.btn.btn-primary').closest('form')";
	// d.body()
	// .getElementsByAttributeValue("class", "btn btn-primary")
	// .attr("onclick",
	// "window.DataAccess.sendFormParameters('btn btn-primary', \""
	// + form + "\", " + form
	// + ".serialize());");
	//
	// d.body().html(e.html());
	// } else if (currentUrl.contains("quiz/feedback?")) {
	// Element e = d.body().getElementById("course-page-content");
	// d.body().html(e.html());
	// } else if (html.contains("has not officially started")) {
	// d.body().getElementsByAttributeValue("class", "btn").remove();
	// } else {
	// Log.d(TAG, "URL not recognized");
	// Element e = d.body().getElementById("course-page-content");
	// d.body().html(e.html());
	// }
	//
	// d.getAllElements()
	// .attr("style",
	// "padding-left:2px; padding-right:2px; margin-left:5px; margin-right:5px;");
	//
	// // d.getAllElements().attr("style", "max-width: 100%;");
	// d.body().attr("onload", "$('*').css('max-width', '100%');");
	// }
	// return show;
	// }
	//
	// private boolean setSubtitle(Document d, String clazz) {
	// try {
	// subtitleText = d.body().getElementsByAttributeValue("class", clazz)
	// .first().text();
	// return true;
	// } catch (NullPointerException ex) {
	// return false;
	// }
	// }
	//
	// public String getCurrentUrl() {
	// return currentUrl;
	// }
	//
	// public String getUrlToInsert() {
	// return urlToInsert;
	// }
	//
	// public void setCodeToInsert(String codeToInsert) {
	// this.codeToInsert = codeToInsert;
	// }
	//
	public List<MenuItem<?>> getMenuActions() {
		return menuActions;
	}
	//
	// public void setUserId(String userId) {
	// // urlToInsert = USER_TOKEN + userId;
	// this.userId = userId;
	// }
	//
	// // public boolean isUserIdLoaded() {
	// // return userId != null;
	// // }
	//
	// public String getIntermediateUrlTokenIgnore() {
	// return intermediateUrlTokenIgnore;
	// }

}
