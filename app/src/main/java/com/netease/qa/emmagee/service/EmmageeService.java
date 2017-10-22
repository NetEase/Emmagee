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
package com.netease.qa.emmagee.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import com.netease.qa.emmagee.R;
import com.netease.qa.emmagee.activity.MainPageActivity;
import com.netease.qa.emmagee.utils.Constants;
import com.netease.qa.emmagee.utils.CpuInfo;
import com.netease.qa.emmagee.utils.CurrentInfo;
import com.netease.qa.emmagee.utils.EncryptData;
import com.netease.qa.emmagee.utils.FpsInfo;
import com.netease.qa.emmagee.utils.MailSender;
import com.netease.qa.emmagee.utils.MemoryInfo;
import com.netease.qa.emmagee.utils.MyApplication;
import com.netease.qa.emmagee.utils.ProcessInfo;
import com.netease.qa.emmagee.utils.Programe;
import com.netease.qa.emmagee.utils.Settings;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Service running in background
 * 
 * @author andrewleo
 */
public class EmmageeService extends Service {

	private final static String LOG_TAG = "Emmagee-"
			+ EmmageeService.class.getSimpleName();

	private static final String BLANK_STRING = "";

	private WindowManager windowManager = null;
	private WindowManager.LayoutParams wmParams = null;
	private View viFloatingWindow;
	private float mTouchStartX;
	private float mTouchStartY;
	private float x;
	private float y;
	private TextView txtTotalMem;
	private TextView txtUnusedMem;
	private TextView txtTraffic;
	private Button btnStop;
	private Button btnWifi;
	private int delaytime;
	private DecimalFormat fomart;
	private MemoryInfo memoryInfo;
	private WifiManager wifiManager;
	private Handler handler = new Handler();
	private CpuInfo cpuInfo;
	private boolean isFloating;
	private boolean isRoot;
	private boolean isAutoStop = false;
	private String processName, packageName, startActivity;
	private int pid, uid;
	private boolean isServiceStop = false;
	private String sender, password, recipients, smtp;
	private String[] receivers;
	private EncryptData des;
	private ProcessInfo procInfo;
	private int statusBarHeight;

	public static BufferedWriter bw;
	public static FileOutputStream out;
	public static OutputStreamWriter osw;
	public static String resultFilePath;
	public static boolean isStop = false;

	private String totalBatt;
	private String temperature;
	private String voltage;
	private CurrentInfo currentInfo;
	private FpsInfo fpsInfo;
	private BatteryInfoBroadcastReceiver batteryBroadcast = null;

	// get start time
	private static final int MAX_START_TIME_COUNT = 5;
	private static final String START_TIME = "#startTime";
	private int getStartTimeCount = 0;
	private boolean isGetStartTime = true;
	private String startTime = "";
	public static final String SERVICE_ACTION = "com.netease.action.emmageeService";
	private static final String BATTERY_CHANGED = "android.intent.action.BATTERY_CHANGED";

	@Override
	public void onCreate() {
		Log.i(LOG_TAG, "service onCreate");
		super.onCreate();
		isServiceStop = false;
		isStop = false;
		fpsInfo = new FpsInfo();
		memoryInfo = new MemoryInfo();
		procInfo = new ProcessInfo();
		fomart = new DecimalFormat();
		fomart.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		fomart.setGroupingUsed(false);
		fomart.setMaximumFractionDigits(2);
		fomart.setMinimumFractionDigits(0);
		des = new EncryptData("emmagee");
		currentInfo = new CurrentInfo();
		statusBarHeight = getStatusBarHeight();
		batteryBroadcast = new BatteryInfoBroadcastReceiver();
		registerReceiver(batteryBroadcast, new IntentFilter(BATTERY_CHANGED));
	}

	/**
	 * 电池信息监控监听器
	 * 
	 * @author andrewleo
	 * 
	 */
	public class BatteryInfoBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
				int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
				int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
				totalBatt = String.valueOf(level * 100 / scale);
				voltage = String.valueOf(intent.getIntExtra(
						BatteryManager.EXTRA_VOLTAGE, -1) * 1.0 / 1000);
				temperature = String.valueOf(intent.getIntExtra(
						BatteryManager.EXTRA_TEMPERATURE, -1) * 1.0 / 10);
			}

		}

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(LOG_TAG, "service onStart");
		PendingIntent contentIntent = PendingIntent.getActivity(
				getBaseContext(), 0, new Intent(this, MainPageActivity.class),
				0);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(
				this);
		builder.setContentIntent(contentIntent).setSmallIcon(R.drawable.icon)
				.setWhen(System.currentTimeMillis()).setAutoCancel(true)
				.setContentTitle("Emmagee");
		startForeground(startId, builder.build());

		pid = intent.getExtras().getInt("pid");
		//uid = intent.getExtras().getInt("uid");
		processName = intent.getExtras().getString("processName");
		packageName = intent.getExtras().getString("packageName");
		startActivity = intent.getExtras().getString("startActivity");

		try {
			PackageManager pm = getPackageManager();
			ApplicationInfo ainfo = pm.getApplicationInfo(packageName, PackageManager.GET_ACTIVITIES);
			uid = ainfo.uid;
		}catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		cpuInfo = new CpuInfo(getBaseContext(), pid, Integer.toString(uid));
		readSettingInfo();
		if (isFloating) {
			viFloatingWindow = LayoutInflater.from(this).inflate(
					R.layout.floating, null);
			txtUnusedMem = (TextView) viFloatingWindow
					.findViewById(R.id.memunused);
			txtTotalMem = (TextView) viFloatingWindow
					.findViewById(R.id.memtotal);
			txtTraffic = (TextView) viFloatingWindow.findViewById(R.id.traffic);
			btnWifi = (Button) viFloatingWindow.findViewById(R.id.wifi);

			wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			if (wifiManager.isWifiEnabled()) {
				btnWifi.setText(R.string.close_wifi);
			} else {
				btnWifi.setText(R.string.open_wifi);
			}
			txtUnusedMem.setText(getString(R.string.calculating));
			txtUnusedMem.setTextColor(android.graphics.Color.RED);
			txtTotalMem.setTextColor(android.graphics.Color.RED);
			txtTraffic.setTextColor(android.graphics.Color.RED);
			btnStop = (Button) viFloatingWindow.findViewById(R.id.stop);
			btnStop.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent();
					intent.putExtra("isServiceStop", true);
					intent.setAction(SERVICE_ACTION);
					sendBroadcast(intent);
					stopSelf();
				}
			});
			createFloatingWindow();
		}
		createResultCsv();
		handler.postDelayed(task, 1000);
		return START_NOT_STICKY;
	}

	/**
	 * read configuration file.
	 * 
	 * @throws IOException
	 */
	private void readSettingInfo() {
		SharedPreferences preferences = Settings
				.getDefaultSharedPreferences(getApplicationContext());
		int interval = preferences.getInt(Settings.KEY_INTERVAL, 5);
		delaytime = interval * 1000;
		isFloating = preferences.getBoolean(Settings.KEY_ISFLOAT, true);
		sender = preferences.getString(Settings.KEY_SENDER, BLANK_STRING);
		password = preferences.getString(Settings.KEY_PASSWORD, BLANK_STRING);
		recipients = preferences.getString(Settings.KEY_RECIPIENTS,
				BLANK_STRING);
		receivers = recipients.split("\\s+");
		smtp = preferences.getString(Settings.KEY_SMTP, BLANK_STRING);
		isRoot = preferences.getBoolean(Settings.KEY_ROOT, false);
		isAutoStop = preferences.getBoolean(Settings.KEY_AUTO_STOP, true);
	}

	/**
	 * write the test result to csv format report.
	 */
	private void createResultCsv() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
		String heapData = "";
		String mDateTime = formatter.format(cal.getTime().getTime());
		resultFilePath = Settings.EMMAGEE_RESULT_DIR + mDateTime + "_" + packageName
				+ ".csv";
		try {
			File resultFile = new File(resultFilePath);
			resultFile.getParentFile().mkdirs();
			resultFile.createNewFile();
			out = new FileOutputStream(resultFile);
			osw = new OutputStreamWriter(out);
			bw = new BufferedWriter(osw);
			long totalMemorySize = memoryInfo.getTotalMemory();
			String totalMemory = fomart.format((double) totalMemorySize / 1024);
			String multiCpuTitle = BLANK_STRING;
			// titles of multiple cpu cores
			ArrayList<String> cpuList = cpuInfo.getCpuList();
			for (int i = 0; i < cpuList.size(); i++) {
				multiCpuTitle += Constants.COMMA + cpuList.get(i)
						+ getString(R.string.total_usage);
			}
			bw.write(getString(R.string.process_package) + Constants.COMMA
					+ packageName + Constants.LINE_END
					+ getString(R.string.process_name) + Constants.COMMA
					+ processName + Constants.LINE_END
					+ getString(R.string.process_pid) + Constants.COMMA + pid
					+ Constants.LINE_END + getString(R.string.mem_size)
					+ Constants.COMMA + totalMemory + "MB" + Constants.LINE_END
					+ getString(R.string.cpu_type) + Constants.COMMA
					+ cpuInfo.getCpuName() + Constants.LINE_END
					+ getString(R.string.android_system_version)
					+ Constants.COMMA + memoryInfo.getSDKVersion()
					+ Constants.LINE_END + getString(R.string.mobile_type)
					+ Constants.COMMA + memoryInfo.getPhoneType()
					+ Constants.LINE_END + "UID" + Constants.COMMA + uid
					+ Constants.LINE_END);

			if (isGrantedReadLogsPermission()) {
				bw.write(START_TIME);
			}
			if (isRoot) {
				heapData = getString(R.string.native_heap) + Constants.COMMA
						+ getString(R.string.dalvik_heap) + Constants.COMMA;
			}
			bw.write(getString(R.string.timestamp) + Constants.COMMA
					+ getString(R.string.top_activity) + Constants.COMMA
					+ heapData + getString(R.string.used_mem_PSS)
					+ Constants.COMMA + getString(R.string.used_mem_ratio)
					+ Constants.COMMA + getString(R.string.mobile_free_mem)
					+ Constants.COMMA + getString(R.string.app_used_cpu_ratio)
					+ Constants.COMMA
					+ getString(R.string.total_used_cpu_ratio) + multiCpuTitle
					+ Constants.COMMA + getString(R.string.traffic)
					+ Constants.COMMA + getString(R.string.battery)
					+ Constants.COMMA + getString(R.string.current)
					+ Constants.COMMA + getString(R.string.temperature)
					+ Constants.COMMA + getString(R.string.voltage)
					+ Constants.COMMA + getString(R.string.fps)
					+ Constants.LINE_END);
		} catch (IOException e) {
			Log.e(LOG_TAG, e.getMessage());
		}
	}

	/**
	 * create a floating window to show real-time data.
	 */
	private void createFloatingWindow() {
		SharedPreferences shared = getSharedPreferences("float_flag",
				Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = shared.edit();
		editor.putInt("float", 1);
		editor.commit();
		windowManager = (WindowManager) getApplicationContext()
				.getSystemService("window");
		wmParams = ((MyApplication) getApplication()).getMywmParams();
		wmParams.type = 2002;
		wmParams.flags |= 8;
		wmParams.gravity = Gravity.LEFT | Gravity.TOP;
		wmParams.x = 0;
		wmParams.y = 0;
		wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
		wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
		wmParams.format = 1;
		windowManager.addView(viFloatingWindow, wmParams);
		viFloatingWindow.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				x = event.getRawX();
				y = event.getRawY() - statusBarHeight;
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					mTouchStartX = event.getX();
					mTouchStartY = event.getY();
					break;
				case MotionEvent.ACTION_MOVE:
					updateViewPosition();
					break;
				case MotionEvent.ACTION_UP:
					updateViewPosition();
					mTouchStartX = mTouchStartY = 0;
					break;
				}
				return true;
			}
		});

		btnWifi.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					btnWifi = (Button) viFloatingWindow.findViewById(R.id.wifi);
					String buttonText = (String) btnWifi.getText();
					String wifiText = getResources().getString(
							R.string.open_wifi);
					if (buttonText.equals(wifiText)) {
						wifiManager.setWifiEnabled(true);
						btnWifi.setText(R.string.close_wifi);
					} else {
						wifiManager.setWifiEnabled(false);
						btnWifi.setText(R.string.open_wifi);
					}
				} catch (Exception e) {
					Toast.makeText(viFloatingWindow.getContext(),
							getString(R.string.wifi_fail_toast),
							Toast.LENGTH_LONG).show();
					Log.e(LOG_TAG, e.toString());
				}
			}
		});
	}

	private Runnable task = new Runnable() {

		public void run() {
			if (!isServiceStop) {
				dataRefresh();
				handler.postDelayed(this, delaytime);
				if (isFloating && viFloatingWindow != null) {
					windowManager.updateViewLayout(viFloatingWindow, wmParams);
				}
				// get app start time from logcat on every task running
				getStartTimeFromLogcat();
			} else {
				Intent intent = new Intent();
				intent.putExtra("isServiceStop", true);
				intent.setAction(SERVICE_ACTION);
				sendBroadcast(intent);
				stopSelf();
			}
		}
	};

	/**
	 * Try to get start time from logcat.
	 */
	private void getStartTimeFromLogcat() {
		if (!isGetStartTime || getStartTimeCount >= MAX_START_TIME_COUNT) {
			return;
		}
		try {
			// filter logcat by Tag:ActivityManager and Level:Info
			String logcatCommand = "logcat -v time -d ActivityManager:I *:S";
			Process process = Runtime.getRuntime().exec(logcatCommand);
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(process.getInputStream()));
			StringBuilder strBuilder = new StringBuilder();
			String line = BLANK_STRING;

			while ((line = bufferedReader.readLine()) != null) {
				strBuilder.append(line);
				strBuilder.append(Constants.LINE_END);
				String regex = ".*Displayed.*" + startActivity
						+ ".*\\+(.*)ms.*";
				if (line.matches(regex)) {
					Log.w("my logs", line);
					if (line.contains("total")) {
						line = line.substring(0, line.indexOf("total"));
					}
					startTime = line.substring(line.lastIndexOf("+") + 1,
							line.lastIndexOf("ms") + 2);
					Toast.makeText(EmmageeService.this,
							getString(R.string.start_time) + startTime,
							Toast.LENGTH_LONG).show();
					isGetStartTime = false;
					break;
				}
			}
			getStartTimeCount++;
		} catch (IOException e) {
			Log.d(LOG_TAG, e.getMessage());
		}
	}

	/**
	 * Above JellyBean, we cannot grant READ_LOGS permission...
	 * 
	 * @return
	 */
	private boolean isGrantedReadLogsPermission() {
		int permissionState = getPackageManager().checkPermission(
				android.Manifest.permission.READ_LOGS, getPackageName());
		return permissionState == PackageManager.PERMISSION_GRANTED;
	}

	/**
	 * refresh the performance data showing in floating window.
	 * 
	 * @throws FileNotFoundException
	 * 
	 * @throws IOException
	 */
	private void dataRefresh() {
		int pidMemory = memoryInfo.getPidMemorySize(pid, getBaseContext());
		long freeMemory = memoryInfo.getFreeMemorySize(getBaseContext());
		String freeMemoryKb = fomart.format((double) freeMemory / 1024);
		String processMemory = fomart.format((double) pidMemory / 1024);
		String currentBatt = String.valueOf(currentInfo.getCurrentValue());
		// 异常数据过滤
		try {
			if (Math.abs(Double.parseDouble(currentBatt)) >= 500) {
				currentBatt = Constants.NA;
			}
		} catch (Exception e) {
			currentBatt = Constants.NA;
		}
		ArrayList<String> processInfo = cpuInfo.getCpuRatioInfo(totalBatt,
				currentBatt, temperature, voltage,
				String.valueOf(fpsInfo.fps()), isRoot);
		if (isFloating) {
			String processCpuRatio = "0.00";
			String totalCpuRatio = "0.00";
			String trafficSize = "0";
			long tempTraffic = 0L;
			double trafficMb = 0;
			boolean isMb = false;
			if (!processInfo.isEmpty()) {
				processCpuRatio = processInfo.get(0);
				totalCpuRatio = processInfo.get(1);
				trafficSize = processInfo.get(2);
				if (!(BLANK_STRING.equals(trafficSize))
						&& !("-1".equals(trafficSize))) {
					tempTraffic = Long.parseLong(trafficSize);
					if (tempTraffic > 1024) {
						isMb = true;
						trafficMb = (double) tempTraffic / 1024;
					}
				}
				// 如果cpu使用率存在且都不小于0，则输出
				if (processCpuRatio != null && totalCpuRatio != null) {
					txtUnusedMem.setText(getString(R.string.process_free_mem)
							+ processMemory + "/" + freeMemoryKb + "MB");
					txtTotalMem.setText(getString(R.string.process_overall_cpu)
							+ processCpuRatio + "%/" + totalCpuRatio + "%");
					String batt = getString(R.string.current) + currentBatt;
					if ("-1".equals(trafficSize)) {
						txtTraffic.setText(batt + Constants.COMMA
								+ getString(R.string.traffic) + Constants.NA);
					} else if (isMb)
						txtTraffic.setText(batt + Constants.COMMA
								+ getString(R.string.traffic)
								+ fomart.format(trafficMb) + "MB");
					else
						txtTraffic.setText(batt + Constants.COMMA
								+ getString(R.string.traffic) + trafficSize
								+ "KB");
				}
				// 当内存为0切cpu使用率为0时则是被测应用退出
				if ("0".equals(processMemory)) {
					if (isAutoStop) {
						closeOpenedStream();
						isServiceStop = true;
						return;
					} else {
						Log.i(LOG_TAG, "未设置自动停止测试，继续监听");
						// 如果设置应用退出后不自动停止，则需要每次监听时重新获取pid
						Programe programe = procInfo.getProgrameByPackageName(
								this, packageName);
						if (programe != null && programe.getPid() > 0) {
							pid = programe.getPid();
							uid = programe.getUid();
							cpuInfo = new CpuInfo(getBaseContext(), pid,
									Integer.toString(uid));
						}
					}
				}
			}

		}
	}

	/**
	 * update the position of floating window.
	 */
	private void updateViewPosition() {
		wmParams.x = (int) (x - mTouchStartX);
		wmParams.y = (int) (y - mTouchStartY);
		if (viFloatingWindow != null) {
			windowManager.updateViewLayout(viFloatingWindow, wmParams);
		}
	}

	/**
	 * close all opened stream.
	 */
	public void closeOpenedStream() {
		try {
			if (bw != null) {
				bw.close();
			}
			if (osw != null)
				osw.close();
			if (out != null)
				out.close();
		} catch (Exception e) {
			Log.d(LOG_TAG, e.getMessage());
		}
	}

	@Override
	public void onDestroy() {
		Log.i(LOG_TAG, "service onDestroy");
		if (windowManager != null) {
			windowManager.removeView(viFloatingWindow);
			viFloatingWindow = null;
		}
		handler.removeCallbacks(task);
		closeOpenedStream();
		// replace the start time in file
		if (!BLANK_STRING.equals(startTime)) {
			replaceFileString(resultFilePath, START_TIME,
					getString(R.string.start_time) + startTime
							+ Constants.LINE_END);
		} else {
			replaceFileString(resultFilePath, START_TIME, BLANK_STRING);
		}
		isStop = true;
		unregisterReceiver(batteryBroadcast);
		boolean isSendSuccessfully = false;
		try {
			isSendSuccessfully = MailSender.sendTextMail(sender,
					des.decrypt(password), smtp,
					"Emmagee Performance Test Report", "see attachment",
					resultFilePath, receivers);
		} catch (Exception e) {
			isSendSuccessfully = false;
		}
		if (isSendSuccessfully) {
			Toast.makeText(this,
					getString(R.string.send_success_toast) + recipients,
					Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(
					this,
					getString(R.string.send_fail_toast)
							+ EmmageeService.resultFilePath, Toast.LENGTH_LONG)
					.show();
		}
		super.onDestroy();
		stopForeground(true);
	}

	/**
	 * Replaces all matches for replaceType within this replaceString in file on
	 * the filePath
	 * 
	 * @param filePath
	 * @param replaceType
	 * @param replaceString
	 */
	private void replaceFileString(String filePath, String replaceType,
			String replaceString) {
		try {
			File file = new File(filePath);
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = BLANK_STRING;
			String oldtext = BLANK_STRING;
			while ((line = reader.readLine()) != null) {
				oldtext += line + Constants.LINE_END;
			}
			reader.close();
			// replace a word in a file
			String newtext = oldtext.replaceAll(replaceType, replaceString);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(filePath),
					getString(R.string.csv_encoding)));
			writer.write(newtext);
			writer.close();
		} catch (IOException e) {
			Log.d(LOG_TAG, e.getMessage());
		}
	}

	/**
	 * get height of status bar
	 * 
	 * @return height of status bar, if default method does not work, return 25
	 */
	public int getStatusBarHeight() {
		// set status bar height to 25
		int barHeight = 25;
		int resourceId = getResources().getIdentifier("status_bar_height",
				"dimen", "android");
		if (resourceId > 0) {
			barHeight = getResources().getDimensionPixelSize(resourceId);
		}
		return barHeight;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}