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
import java.io.FileReader;
import java.io.IOException;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.util.Log;

public class MemoryInfo {

	private static final String LOG_TAG = "Emmagee-"
			+ MemoryInfo.class.getSimpleName();

	/**
	 * read the total memory of certain device
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
	 * get free memory
	 * 
	 * @return free memory of device
	 * 
	 */
	public long getFreeMemorySize(Context context) {
		ActivityManager.MemoryInfo outInfo = new ActivityManager.MemoryInfo();
		ActivityManager am = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		am.getMemoryInfo(outInfo);
		long avaliMem = outInfo.availMem;
		return avaliMem / 1024;
	}

	/**
	 * get the memory of process with certain pid
	 * 
	 * @param pid
	 *            pid of process
	 * @param context
	 *            context of certain activity
	 * @return memory usage of certain process
	 */
	public int getPidMemorySize(int pid, Context context) {
		ActivityManager am = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		int[] myMempid = new int[] { pid };
		Debug.MemoryInfo[] memoryInfo = am.getProcessMemoryInfo(myMempid);
		memoryInfo[0].getTotalSharedDirty();
		
		// int memSize = memoryInfo[0].dalvikPrivateDirty;
		// TODO PSS
		int memSize = memoryInfo[0].getTotalPss();
		// int memSize = memoryInfo[0].getTotalPrivateDirty();
		return memSize;
	}

	/**
	 * get the sdk version of phone
	 * 
	 * @return sdk version
	 */
	public String getSDKVersion() {
		return android.os.Build.VERSION.RELEASE;
	}

	/**
	 * get phone type
	 * 
	 * @return phone type
	 */
	public String getPhoneType() {
		return android.os.Build.MODEL;
	}
}
