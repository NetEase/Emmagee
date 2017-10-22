/*
 * Copyright (c) 2012-2013 NetEase, Inc. and other contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.netease.qa.emmagee.activity;

import java.io.IOException;
import java.util.List;

import com.netease.qa.emmagee.R;
import com.netease.qa.emmagee.service.EmmageeService;
import com.netease.qa.emmagee.utils.ProcessInfo;
import com.netease.qa.emmagee.utils.Programe;
import com.netease.qa.emmagee.utils.Settings;
import com.netease.qa.emmagee.utils.WakeLockHelper;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Main Page of Emmagee
 * 
 * @author andrewleo
 */
public class MainPageActivity extends Activity {

	private static final String LOG_TAG = "Emmagee-" + MainPageActivity.class.getSimpleName();

	private static final int TIMEOUT = 20000;

	private ProcessInfo processInfo;
	private Intent monitorService;
	private ListView lstViProgramme;
	private Button btnTest;
	private int pid, uid;
	private boolean isServiceStop = false;
	private UpdateReceiver receiver;

	private TextView nbTitle;
	private ImageView ivGoBack;
	private ImageView ivBtnSet;
	private LinearLayout layBtnSet;
	private Long mExitTime = (long) 0;
	private ListAdapter la;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(LOG_TAG, "MainActivity::onCreate");
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.mainpage);
		
		initTitleLayout();
		loadSettings();
		processInfo = new ProcessInfo();
		btnTest.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (Build.VERSION.SDK_INT < 24) {
					monitorService = new Intent();
					monitorService.setClass(MainPageActivity.this, EmmageeService.class);
					if (getString(R.string.start_test).equals(btnTest.getText().toString())) {
						ListAdapter adapter = (ListAdapter) lstViProgramme.getAdapter();
						if (adapter.checkedProg != null) {
							String packageName = adapter.checkedProg.getPackageName();
							String processName = adapter.checkedProg.getProcessName();
							Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
							String startActivity = "";
							Log.d(LOG_TAG, packageName);
							// clear logcat
							try {
								Runtime.getRuntime().exec("logcat -c");
							} catch (IOException e) {
								Log.d(LOG_TAG, e.getMessage());
							}
							try {
								startActivity = intent.resolveActivity(getPackageManager()).getShortClassName();
								startActivity(intent);
							} catch (Exception e) {
								Toast.makeText(MainPageActivity.this, getString(R.string.can_not_start_app_toast), Toast.LENGTH_LONG).show();
								return;
							}
							waitForAppStart(packageName);
							monitorService.putExtra("processName", processName);
							monitorService.putExtra("pid", pid);
							monitorService.putExtra("uid", uid);
							monitorService.putExtra("packageName", packageName);
							monitorService.putExtra("startActivity", startActivity);
							startService(monitorService);
							isServiceStop = false;
							btnTest.setText(getString(R.string.stop_test));
						} else {
							Toast.makeText(MainPageActivity.this, getString(R.string.choose_app_toast), Toast.LENGTH_LONG).show();
						}
					} else {
						btnTest.setText(getString(R.string.start_test));
						Toast.makeText(MainPageActivity.this, getString(R.string.test_result_file_toast) + EmmageeService.resultFilePath,
								Toast.LENGTH_LONG).show();
						stopService(monitorService);
					}
				} else {
					Toast.makeText(MainPageActivity.this, getString(R.string.nougat_warning),Toast.LENGTH_LONG).show();
				}
			}
		});

		la = new ListAdapter(processInfo.getAllPackages(getBaseContext()));
		lstViProgramme.setAdapter(la);
		lstViProgramme.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				RadioButton rdBtn = (RadioButton) ((LinearLayout) view).getChildAt(0);
				rdBtn.setChecked(true);
			}
		});

		nbTitle.setText(getString(R.string.app_name));
		ivGoBack.setImageResource(R.drawable.refresh);
		ivBtnSet.setImageResource(R.drawable.settings_button);
		layBtnSet.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				goToSettingsActivity();
			}
		});
		
		ivGoBack.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Toast.makeText(MainPageActivity.this, R.string.update_list, Toast.LENGTH_SHORT).show();
				la.swapItems(processInfo.getAllPackages(getBaseContext()));
			}
		});
		
		receiver = new UpdateReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(EmmageeService.SERVICE_ACTION);
		registerReceiver(receiver, filter);
	}

	private void initTitleLayout() {
		ivGoBack = (ImageView) findViewById(R.id.go_back);
		nbTitle = (TextView) findViewById(R.id.nb_title);
		ivBtnSet = (ImageView) findViewById(R.id.btn_set);
		lstViProgramme = (ListView) findViewById(R.id.processList);
		btnTest = (Button) findViewById(R.id.test);
		layBtnSet = (LinearLayout) findViewById(R.id.lay_btn_set);
	}
	
	private void loadSettings() {
		SharedPreferences preferences = Settings.getDefaultSharedPreferences(this);
		boolean wakeLock = preferences.getBoolean(Settings.KEY_WACK_LOCK, false);
		if (wakeLock) {
			Toast.makeText(this, R.string.wake_lock_on_toast, Toast.LENGTH_LONG).show();
			Settings.getDefaultWakeLock(this).acquireFullWakeLock();
		}
	}

	/**
	 * customized BroadcastReceiver
	 * 
	 * @author andrewleo
	 */
	public class UpdateReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			isServiceStop = intent.getExtras().getBoolean("isServiceStop");
			if (isServiceStop) {
				btnTest.setText(getString(R.string.start_test));
			}
		}
	}

	@Override
	protected void onStart() {
		Log.d(LOG_TAG, "onStart");
		super.onStart();
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(LOG_TAG, "onResume");
		if (isServiceStop) {
			btnTest.setText(getString(R.string.start_test));
		}
	}

	/**
	 * wait for test application started.
	 * 
	 * @param packageName
	 *            package name of test application
	 */
	private void waitForAppStart(String packageName) {
		Log.d(LOG_TAG, "wait for app start");
		boolean isProcessStarted = false;
		long startTime = System.currentTimeMillis();
		while (System.currentTimeMillis() < startTime + TIMEOUT) {
			pid = processInfo.getPidByPackageName(getBaseContext(), packageName);
			if (pid != 0) {
				isProcessStarted = true;
				break;
			}
			if (isProcessStarted) {
				break;
			}
		}
	}

	/**
	 * show a dialog when click return key.
	 * 
	 * @return Return true to prevent this event from being propagated further,
	 *         or false to indicate that you have not handled this event and it
	 *         should continue to be propagated.
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if ((System.currentTimeMillis() - mExitTime) > 2000) {
				Toast.makeText(this, R.string.quite_alert, Toast.LENGTH_SHORT).show();
				mExitTime = System.currentTimeMillis();
			} else {
				if (monitorService != null) {
					Log.d(LOG_TAG, "stop service");
					stopService(monitorService);
				}
				System.exit(0);
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void goToSettingsActivity() {
		Intent intent = new Intent();
		intent.setClass(MainPageActivity.this, SettingsActivity.class);
		startActivityForResult(intent, Activity.RESULT_FIRST_USER);
	}

	/**
	 * customizing adapter.
	 * 
	 * @author andrewleo
	 */
	private class ListAdapter extends BaseAdapter {
		List<Programe> programes;
		Programe checkedProg;
		int lastCheckedPosition = -1;

		public ListAdapter(List<Programe> programes) {
			this.programes = programes;
		}

		@Override
		public int getCount() {
			return programes.size();
		}

		@Override
		public Object getItem(int position) {
			return programes.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
		
		public void swapItems(List<Programe> programes) {
		    this.programes = programes;
		    notifyDataSetChanged();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Programe pr = (Programe) programes.get(position);
			if (convertView == null)
				convertView = getLayoutInflater().inflate(R.layout.list_item, parent, false);
			Viewholder holder = (Viewholder) convertView.getTag();
			if (holder == null) {
				holder = new Viewholder();
				convertView.setTag(holder);
				holder.imgViAppIcon = (ImageView) convertView.findViewById(R.id.image);
				holder.txtAppName = (TextView) convertView.findViewById(R.id.text);
				holder.rdoBtnApp = (RadioButton) convertView.findViewById(R.id.rb);
				holder.rdoBtnApp.setFocusable(false);
				holder.rdoBtnApp.setOnCheckedChangeListener(checkedChangeListener);
			}
			holder.imgViAppIcon.setImageDrawable(pr.getIcon());
			holder.txtAppName.setText(pr.getProcessName());
			holder.rdoBtnApp.setId(position);
			holder.rdoBtnApp.setChecked(checkedProg != null && getItem(position) == checkedProg);
			return convertView;
		}

		OnCheckedChangeListener checkedChangeListener = new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					final int checkedPosition = buttonView.getId();
					if (lastCheckedPosition != -1) {
						RadioButton tempButton = (RadioButton) findViewById(lastCheckedPosition);
						if ((tempButton != null) && (lastCheckedPosition != checkedPosition)) {
							tempButton.setChecked(false);
						}
					}
					checkedProg = programes.get(checkedPosition);
					lastCheckedPosition = checkedPosition;
				}
			}
		};
	}

	/**
	 * save status of all installed processes
	 * 
	 * @author andrewleo
	 */
	static class Viewholder {
		TextView txtAppName;
		ImageView imgViAppIcon;
		RadioButton rdoBtnApp;
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(receiver);  
		super.onDestroy();
	}
}
