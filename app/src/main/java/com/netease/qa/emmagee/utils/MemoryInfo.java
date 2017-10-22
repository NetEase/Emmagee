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
package com.netease.qa.emmagee.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.util.Log;

/**
 * operate memory information
 * 
 * @author andrewleo
 */
public class MemoryInfo {

	private static final String LOG_TAG = "Emmagee-" + MemoryInfo.class.getSimpleName();

	private static Process process;

	/**
	 * get total memory of certain device.
	 * 
	 * @return total memory of device
	 */
	public long getTotalMemory() {
		String memInfoPath = "/proc/meminfo";
		String readTemp = "";
		String memTotal = "";
		long memory = 0;
		try {
			FileReader fr = new FileReader(memInfoPath);
			BufferedReader localBufferedReader = new BufferedReader(fr, 8192);
			while ((readTemp = localBufferedReader.readLine()) != null) {
				if (readTemp.contains("MemTotal")) {
					String[] total = readTemp.split(":");
					memTotal = total[1].trim();
				}
			}
			localBufferedReader.close();
			String[] memKb = memTotal.split(" ");
			memTotal = memKb[0].trim();
			Log.d(LOG_TAG, "memTotal: " + memTotal);
			memory = Long.parseLong(memTotal);
		} catch (IOException e) {
			Log.e(LOG_TAG, "IOException: " + e.getMessage());
		}
		return memory;
	}

	/**
	 * get free memory.
	 * 
	 * @return free memory of device
	 * 
	 */
	public long getFreeMemorySize(Context context) {
		ActivityManager.MemoryInfo outInfo = new ActivityManager.MemoryInfo();
		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		am.getMemoryInfo(outInfo);
		long avaliMem = outInfo.availMem;
		return avaliMem / 1024;
	}

	/**
	 * get the memory of process with certain pid.
	 * 
	 * @param pid
	 *            pid of process
	 * @param context
	 *            context of certain activity
	 * @return memory usage of certain process
	 */
	public int getPidMemorySize(int pid, Context context) {
		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		int[] myMempid = new int[] { pid };
		Debug.MemoryInfo[] memoryInfo = am.getProcessMemoryInfo(myMempid);
		memoryInfo[0].getTotalSharedDirty();
		int memSize = memoryInfo[0].getTotalPss();
		return memSize;
	}

	/**
	 * get the sdk version of phone.
	 * 
	 * @return sdk version
	 */
	public String getSDKVersion() {
		return android.os.Build.VERSION.RELEASE;
	}

	/**
	 * get phone type.
	 * 
	 * @return phone type
	 */
	public String getPhoneType() {
		return android.os.Build.MODEL;
	}

	/**
	 * get app heap size, it is more importance than total memory
	 * 
	 * @return heap size
	 */
	public static String[][] getHeapSize(int pid, Context context) {
		String[][] heapData = parseMeminfo(pid);
		return heapData;
	}

	/**
	 * dumpsys meminfo, and parse the result to get native and heap data
	 * 
	 * @param pid
	 *            process id
	 * @return native and heap data
	 */
	public static String[][] parseMeminfo(int pid) {

		boolean infoStart = false;
		// [][],00:native heap size,01:native heap alloc;10: dalvik heap
		// size,11: dalvik heap alloc
		String[][] heapData = new String[2][2];

		try {
			Runtime runtime = Runtime.getRuntime();
			process = runtime.exec("su");
			DataOutputStream os = new DataOutputStream(process.getOutputStream());
			os.writeBytes("dumpsys meminfo " + pid + "\n");
			os.writeBytes("exit\n");
			os.flush();

			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = "";

			while ((line = bufferedReader.readLine()) != null) {
				line = line.trim();
				if (line.contains("Permission Denial")) {
					break;
				} else {
					// 当读取到MEMINFO in pid 这一行时，下一行就是需要获取的数据
					if (line.contains("MEMINFO in pid")) {
						infoStart = true;
					} else if (infoStart) {
						String[] lineItems = line.split("\\s+");
						int length = lineItems.length;
						if (line.startsWith("size")) {
							heapData[0][0] = lineItems[1];
							heapData[1][0] = lineItems[2];
						} else if (line.startsWith("allocated")) {
							heapData[0][1] = lineItems[1];
							heapData[1][1] = lineItems[2];
							break;
						} else if (line.startsWith("Native")) {
							Log.d(LOG_TAG, "Native");
							Log.d(LOG_TAG, "lineItems[4]=" + lineItems[4]);
							Log.d(LOG_TAG, "lineItems[5]=" + lineItems[5]);
							heapData[0][0] = lineItems[length-3];
							heapData[0][1] = lineItems[length-2];
						} else if (line.startsWith("Dalvik")) {
							Log.d(LOG_TAG, "Dalvik");
							Log.d(LOG_TAG, "lineItems[4]=" + lineItems[4]);
							Log.d(LOG_TAG, "lineItems[5]=" + lineItems[5]);
							heapData[1][0] = lineItems[length-3];
							heapData[1][1] = lineItems[length-2];
							break;
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return heapData;
	}
}
