package com.adapptit.android;

import java.util.List;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.slidingmenu.lib.SlidingMenu;
import com.slidingmenu.lib.SlidingMenu.OnOpenListener;

public class MainActivity extends FragmentActivity {

	private static final String TAG = "MainActivity";
	public static final String CONF_NAME = "confName";

	private ConfigurationManager confManager;
	private SlidingMenu menu;
	private ArrayAdapter<String> adapter;
	private CustomFragment fragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		hideActionBar();

		Bundle extras = getIntent().getExtras();
		String config = extras.getString(CONF_NAME);
		try {
			confManager = ConfigurationManager.getConfigManager(config, this);
			confManager.setConfigurationName(config);
		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
			new AlertDialog.Builder(this).setCancelable(false)
					.setTitle(R.string.error)
					.setMessage(R.string.config_load_error)
					.setIcon(R.drawable.cross)
					.setPositiveButton(R.string.accept, new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
							MainActivity.this.onBackPressed();
						}
					}).create().show();
			return;
		}

		String conditions = confManager.checkConditions();
		if (conditions != null && conditions.length() != 0) {
			new AlertDialog.Builder(this)
					.setCancelable(false)
					.setTitle(R.string.error)
					.setMessage(
							getString(R.string.conditions_error, conditions))
					.setIcon(R.drawable.cross)
					.setPositiveButton(R.string.accept, new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
							MainActivity.this.onBackPressed();
						}
					}).create().show();
			return;
		}

		try {
			insertFragment();
		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
			new AlertDialog.Builder(this).setCancelable(false)
					.setTitle(R.string.error)
					.setMessage(R.string.view_load_error)
					.setIcon(R.drawable.cross)
					.setPositiveButton(R.string.accept, new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
							MainActivity.this.onBackPressed();
						}
					}).create().show();
			return;
		}
		setupMenu();
		setLogo();
	}

	public void fragmentReady() {
		confManager.start();
	}

	private void insertFragment() throws Exception {
		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager
				.beginTransaction();
		Class<?> c = null;
		Exception last = null;
		try {
			c = Class.forName(confManager.FRAGMENT, true,
					ConfigurationManager.getCurrentClassLoader());
		} catch (Exception ex) {
			for (ClassLoader cl : ConfigurationManager.getClassLoaders()) {
				try {
					c = Class.forName(confManager.FRAGMENT, true, cl);
				} catch (Exception e) {
					last = e;
				}
			}
		}
		if (c == null) {
			throw last;
		}
		fragment = (CustomFragment) c.newInstance();
		fragmentTransaction.add(R.id.view, fragment, confManager.FRAGMENT);
		fragmentTransaction.commit();
	}

	public CustomFragment getFragment() {
		return fragment;
	}

	public ConfigurationManager getConfManager() {
		return confManager;
	}

	public SlidingMenu getMenu() {
		return menu;
	}

	public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
		if (keyCode == android.view.KeyEvent.KEYCODE_MENU) {
			if (menu.isMenuShowing()) {
				menu.showContent();
			} else {
				menu.showMenu();
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	// @Override
	// public boolean onMenuOpened(int featureId, Menu m) {
	// if (menu.isMenuShowing()) {
	// menu.showContent();
	// } else {
	// menu.showMenu();
	// }
	// return false;
	// }

	@Override
	public void onBackPressed() {
		if (fragment == null || fragment.onBackPressed()) {
			super.onBackPressed();
		}
		// if (menu.isMenuShowing()) {
		// menu.showContent();
		// } else {
		// try {
		// urlStack.pop();
		// } catch (EmptyStackException ex) {
		// }
		// if (urlStack.empty()) {
		// super.onBackPressed();
		// } else {
		// confManager.load(urlStack.peek());
		// loadControllerUrl(urlStack.peek(), false);
		// }
		// }
	}

	private void setupMenu() {
		menu = new SlidingMenu(this);
		menu.setMode(SlidingMenu.LEFT);
		menu.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
		menu.setFadeDegree(0.35f);
		menu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
		menu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);
		menu.setOnOpenListener(new OnOpenListener() {

			@Override
			public void onOpen() {
				if (confManager != null) {
					adapter = new ArrayAdapter<String>(MainActivity.this,
							R.layout.slidingmenu);
					ListView l = new ListView(MainActivity.this);
					final List<MenuItem<?>> m = confManager.getMenuActions();
					for (int i = 0; i < m.size(); i++) {
						adapter.add(m.get(i).getKey());
					}
					// for (MenuItem<?> s : m) {
					// adapter.add(s.getKey());
					// }
					l.setAdapter(adapter);
					l.setOnItemClickListener(new OnItemClickListener() {

						@Override
						public void onItemClick(AdapterView<?> parent,
								View view, int position, long id) {
							menu.showContent();
							confManager.menuClick(m.get(position).getValue());
							// loadControllerUrl(m.get(position).getValue(),
							// true);
						}
					});
					menu.setMenu(l);
				}
			}
		});
	}

	private void setLogo() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					final Bitmap logo = confManager.getLogo();
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							((ImageView) findViewById(R.id.logo))
									.setImageBitmap(logo);
						}
					});
				} catch (Exception e) {
					Log.e(TAG, e.getMessage(), e);
				}
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						((TextView) findViewById(R.id.logoText))
								.setText(confManager.getLogoText());
					}
				});
			}
		}).start();
	}

	public void setSubtitle() {
		if (!confManager.isDirtySubtitle()) {
			return;
		}
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					final Bitmap logo = confManager.getSubtitle();
					runOnUiThread(new Runnable() {

						@Override
						public void run() {
							((ImageView) findViewById(R.id.subtitle))
									.setImageBitmap(logo);
						}
					});
				} catch (Exception e) {
					Log.e(TAG, e.getMessage(), e);
				}
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						((TextView) findViewById(R.id.subtitleText))
								.setText(confManager.getSubtitleText());
					}
				});
			}
		}).start();
	}

	public void hideContent() {
		((View) findViewById(R.id.view)).setVisibility(View.GONE);
		((View) findViewById(R.id.waiting)).setVisibility(View.VISIBLE);
	}

	public void showContent() {
		((View) findViewById(R.id.view)).setVisibility(View.VISIBLE);
		((View) findViewById(R.id.waiting)).setVisibility(View.GONE);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void hideActionBar() {
		getActionBar().hide();
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void showActionBar() {
		getActionBar().show();
	}

	public void setFullscreen() {
		// getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		// WindowManager.LayoutParams.FLAG_FULLSCREEN);
		((View) findViewById(R.id.title)).setVisibility(View.GONE);
		hideActionBar();
	}

	public void undoFullscreen() {
		// getWindow().setFlags(
		// WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
		// WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		((View) findViewById(R.id.title)).setVisibility(View.VISIBLE);
		// showActionBar();
	}

}
