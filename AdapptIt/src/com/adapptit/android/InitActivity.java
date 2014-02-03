package com.adapptit.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class InitActivity extends ListActivity {

	private static final String TAG = "InitActivity";
	private ConfAdapter adapter;
	private Dialog splash;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PreferencesManager.createInstance(this);
		if (adapter == null) {
			showSplash();

			// try {
			ConfigurationManager.loadModules(this);
			// } catch (Exception e) {
			// Log.e(TAG, e.getMessage(), e);
			// new AlertDialog.Builder(this).setCancelable(false)
			// .setTitle(R.string.error)
			// .setMessage(R.string.module_load_error)
			// .setIcon(R.drawable.cross)
			// .setPositiveButton(R.string.accept, new OnClickListener() {
			//
			// @Override
			// public void onClick(DialogInterface dialog, int which) {
			// dialog.cancel();
			// InitActivity.this.onBackPressed();
			// }
			// }).create().show();
			// return;
			// }
		}
		setContentView(R.layout.activity_init);
		setList();
	}

	private void showSplash() {
		splash = new Dialog(this);
		splash.setContentView(R.layout.splashscreen);
		splash.setCancelable(false);
		splash.show();
	}

	private void removeSplash() {
		if (splash != null) {
			splash.dismiss();
			splash = null;
		}
	}

	private void setList() {
		if (adapter == null) {
			File configs = new File(Environment.getExternalStorageDirectory()
					+ "/AdapptIt/configs");
			File[] listFiles = configs.listFiles(new FilenameFilter() {

				@Override
				public boolean accept(File dir, String filename) {
					if (filename.endsWith(".ait")) {
						return true;
					} else {
						return false;
					}
				}
			});
			ArrayList<String> list = new ArrayList<String>(listFiles.length);
			HashMap<String, String> map = new HashMap<String, String>(
					listFiles.length);
			for (File f : listFiles) {
				String name = f.getName()
						.substring(0, f.getName().length() - 4);
				String mod = "false|" + getString(R.string.unknown);

				BufferedReader bufferedReader = null;
				try {
					bufferedReader = new BufferedReader(new FileReader(f));
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

					JSONObject main = null;
					boolean modLoaded = false;
					try {
						main = new JSONObject(sb.toString());
						modLoaded = ConfigurationManager.isModuleLoaded(main
								.getString("module"));
					} catch (JSONException e) {
						Log.e(TAG, e.getMessage(), e);
						name += "\n("
								+ getString(R.string.invalid_config_message)
								+ ")";
					}
					if (main != null) {
						if (modLoaded) {
							mod = "true|" + main.getString("module");
						} else {
							mod = "false|" + main.getString("module");
						}
					}
				} catch (Exception ex) {
					if (bufferedReader != null) {
						try {
							bufferedReader.close();
						} catch (IOException e) {
							Log.e(TAG, e.getMessage(), e);
						}
					}
					Log.e(TAG, ex.getMessage(), ex);
					name += "\n(" + getString(R.string.file_access_error) + ")";
				}

				// list.add(f.getName().substring(0, f.getName().length() - 4));
				map.put(name, mod);
				list.add(name);

			}
			adapter = new ConfAdapter(this, list, map);
		}
		setListAdapter(adapter);
		removeSplash();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		String item = (String) this.getListAdapter().getItem(position);
		Intent i = new Intent(this, MainActivity.class);
		i.putExtra(MainActivity.CONF_NAME, item + ".ait");
		startActivity(i);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.init, menu);
		return true;
	}

	private class ConfAdapter extends ArrayAdapter<String> {

		private List<String> values;
		private HashMap<String, String> map;
		private HashMap<String, Boolean> enabled;

		public ConfAdapter(Context ctx, List<String> list,
				HashMap<String, String> map) {
			super(ctx, R.layout.rowlayout, list);
			values = list;
			this.map = map;
			enabled = new HashMap<String, Boolean>();
		}

		// @Override
		// public View getView(int position, View convertView, ViewGroup parent)
		// {
		// LayoutInflater inflater = (LayoutInflater) getContext()
		// .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		// View rowView = inflater.inflate(R.layout.rowlayout, parent, false);
		// TextView textView = (TextView) rowView.findViewById(R.id.label);
		// ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
		// String name = values.get(position);
		// textView.setText(name);
		// BufferedReader bufferedReader = null;
		// try {
		// bufferedReader = new BufferedReader(new FileReader(new File(
		// Environment.getExternalStorageDirectory()
		// + "/AdapptIt/configs/" + name + ".ait")));
		// String s;
		// StringBuilder sb = new StringBuilder();
		// while ((s = bufferedReader.readLine()) != null) {
		// sb.append(s).append('\n');
		// }
		// try {
		// bufferedReader.close();
		// } catch (IOException e1) {
		// Log.e(TAG, e1.getMessage(), e1);
		// }
		//
		// JSONObject main = null;
		// try {
		// main = new JSONObject(sb.toString());
		// } catch (JSONException e) {
		// Log.e(TAG, e.getMessage(), e);
		// textView.setText(name + "("
		// + getString(R.string.invalid_config_message) + ")");
		// }
		// if (main != null
		// && ConfigurationManager.isConfigLoaded(main
		// .getString("module"))) {
		// imageView.setImageResource(R.drawable.check);
		// imageView
		// .setContentDescription(getString(R.string.available));
		// } else {
		// imageView.setImageResource(R.drawable.cross);
		// imageView
		// .setContentDescription(getString(R.string.not_available));
		// rowView.setEnabled(false);
		// }
		// } catch (Exception ex) {
		// if (bufferedReader != null) {
		// try {
		// bufferedReader.close();
		// } catch (IOException e) {
		// Log.e(TAG, e.getMessage(), e);
		// }
		// }
		// Log.e(TAG, ex.getMessage(), ex);
		// }
		//
		// return rowView;
		// }
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View rowView = inflater.inflate(R.layout.rowlayout, parent, false);
			TextView label = (TextView) rowView.findViewById(R.id.label);
			label.setSingleLine(false);
			// ImageView icon = (ImageView) rowView.findViewById(R.id.icon);
			TextView module = (TextView) rowView.findViewById(R.id.module);
			String name = values.get(position);
			label.setText(name);

			String mod = map.get(name);
			if (Boolean.parseBoolean(mod.split("\\|")[0])) {
				// icon.setImageResource(R.drawable.check);
				module.setCompoundDrawablesWithIntrinsicBounds(0,
						R.drawable.check, 0, 0);
				module.setContentDescription(getString(R.string.available));
				enabled.put(name, true);
			} else {
				// icon.setImageResource(R.drawable.cross);
				module.setCompoundDrawablesWithIntrinsicBounds(0,
						R.drawable.cross, 0, 0);
				module.setContentDescription(getString(R.string.not_available));
				enabled.put(name, false);
			}

			module.setText(mod.substring(mod.indexOf('|') + 1));

			return rowView;
		}

		@Override
		public boolean isEnabled(int position) {
			return enabled.get(values.get(position));
		}
	}

}
