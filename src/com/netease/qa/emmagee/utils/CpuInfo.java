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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import com.netease.qa.emmagee.service.EmmageeService;

import android.content.Context;
import android.util.Log;

public class CpuInfo {

	private static final String LOG_TAG = "Emmagee-"
			+ CpuInfo.class.getSimpleName();

	private Context context;
	private long processCpu;
	private long idleCpu;
	private long totalCpu;
	private boolean isInitialStatics = true;
	private SimpleDateFormat formatterFile;
	private MemoryInfo mi;
	private long totalMemorySize;
	private long initialTraffic;
	private long lastestTraffic;
	private long traffic;
	private TrafficInfo trafficInfo;
	private ArrayList<String> CpuUsedRatio;
	private long totalCpu2;
	private long processCpu2;
	private long idleCpu2;
	private String processCpuRatio = "";
	private String totalCpuRatio = "";
	private int pid;

	public CpuInfo(Context context, int pid, String uid) {
		this.pid = pid;
		this.context = context;
		trafficInfo = new TrafficInfo(uid);
		formatterFile = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		mi = new MemoryInfo();
		totalMemorySize = mi.getTotalMemory();
		CpuUsedRatio = new ArrayList<String>();
	}

	/**
	 * read the status of CPU
	 * 
	 * @throws FileNotFoundException
	 */
	public void readCpuStat() {
		String processPid = Integer.toString(pid);
		String cpuStatPath = "/proc/" + processPid + "/stat";
		try {
			// monitor cpu stat of certain process
			RandomAccessFile processCpuInfo = new RandomAccessFile(cpuStatPath,
					"r");
			String line = "";
			StringBuffer stringBuffer = new StringBuffer();
			stringBuffer.setLength(0);
			while ((line = processCpuInfo.readLine()) != null) {
				stringBuffer.append(line + "\n");
			}
			String[] tok = stringBuffer.toString().split(" ");
			processCpu = Long.parseLong(tok[13]) + Long.parseLong(tok[14]);
			processCpuInfo.close();
		} catch (FileNotFoundException e) {
			Log.e(LOG_TAG, "FileNotFoundException: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			// monitor total and idle cpu stat of certain process
			RandomAccessFile cpuInfo = new RandomAccessFile("/proc/stat", "r");
			String[] toks = cpuInfo.readLine().split(" ");
			idleCpu = Long.parseLong(toks[5]);
			totalCpu = Long.parseLong(toks[2]) + Long.parseLong(toks[3])
					+ Long.parseLong(toks[4]) + Long.parseLong(toks[6])
					+ Long.parseLong(toks[5]) + Long.parseLong(toks[7])
					+ Long.parseLong(toks[8]);
			cpuInfo.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * get CPU name
	 * 
	 * @return CPU name
	 */
	public String getCpuName() {
		try {
			RandomAccessFile cpu_stat = new RandomAccessFile("/proc/cpuinfo",
					"r");
			String[] cpu = cpu_stat.readLine().split(":"); // cpu信息的前一段是含有processor字符串，此处替换为不显示
			return cpu[1];
		} catch (IOException e) {
			Log.e(LOG_TAG, "IOException: " + e.getMessage());
		}
		return "";
	}

	/**
	 * reserve used ratio of process CPU and total CPU, meanwhile collect
	 * network traffic
	 * 
	 * @return network traffic ,used ratio of process CPU and total CPU in
	 *         certain interval
	 */
	public ArrayList<String> getCpuRatioInfo() {

		DecimalFormat fomart = new DecimalFormat();
		fomart.setMaximumFractionDigits(2);
		fomart.setMinimumFractionDigits(2);

		readCpuStat();
		CpuUsedRatio.clear();

		try {
			Calendar cal = Calendar.getInstance();
			String mDateTime2 = formatterFile.format(cal.getTime().getTime()
					+ 8 * 60 * 60 * 1000);

			if (isInitialStatics == true) {
				initialTraffic = trafficInfo.getTrafficInfo();
				isInitialStatics = false;
			} else {
				lastestTraffic = trafficInfo.getTrafficInfo();
				if (initialTraffic == -1)
					traffic = -1;
				else
					traffic = (lastestTraffic - initialTraffic + 1023) / 1024;
				processCpuRatio = fomart
						.format(100 * ((double) (processCpu - processCpu2) / (double) (totalCpu - totalCpu2)));
				totalCpuRatio = fomart
						.format(100 * ((double) ((totalCpu - idleCpu) - (totalCpu2 - idleCpu2)) / (double) (totalCpu - totalCpu2)));
				long pidMemory = mi.getPidMemorySize(pid, context);
				String pMemory = fomart.format((double) pidMemory / 1024);
				long freeMemory = mi.getFreeMemorySize(context);
				String fMemory = fomart.format((double) freeMemory / 1024);
				String percent = "统计出错";
				if (totalMemorySize != 0) {
					percent = fomart
							.format(((double) pidMemory / (double) totalMemorySize) * 100);
				}

				// whether certain device supports traffic statics
				if (traffic == -1) {
					EmmageeService.bw.write(mDateTime2 + "," + pMemory + ","
							+ percent + "," + fMemory + "," + processCpuRatio
							+ "," + totalCpuRatio + "," + "本程序或本设备不支持流量统计"
							+ "\r\n");
				} else {
					EmmageeService.bw.write(mDateTime2 + "," + pMemory + ","
							+ percent + "," + fMemory + "," + processCpuRatio
							+ "," + totalCpuRatio + "," + traffic + "\r\n");
				}
			}
			totalCpu2 = totalCpu;
			processCpu2 = processCpu;
			idleCpu2 = idleCpu;
			CpuUsedRatio.add(processCpuRatio);
			CpuUsedRatio.add(totalCpuRatio);
			CpuUsedRatio.add(String.valueOf(traffic));
		} catch (IOException e) {
			e.printStackTrace();
			// PttService.closeOpenedStream()
		}
		return CpuUsedRatio;

	}

	// TODO coming soon
	// public String cpuinfo() {
	// String sys_info = "";
	// String s;
	// try {
	// RandomAccessFile reader_stat = new RandomAccessFile("/proc/stat",
	// "r");
	// RandomAccessFile reader_info = new RandomAccessFile(
	// "/proc/cpuinfo", "r");
	// sys_info = reader_info.readLine(); // CPU型号
	// String load_info;
	// String cpu_stat = reader_stat.readLine(); // cpu行信息
	// String cpu0_stat = reader_stat.readLine(); // cpu0
	// String cpu1_stat = reader_stat.readLine(); // cpu1
	//
	// String[] tok = cpu_stat.split(" ");
	// String[] tok1 = cpu0_stat.split(" ");
	// String[] tok2 = cpu1_stat.split(" ");
	//
	// // 判断单核
	// if (tok[2].equals(tok1[1])) {
	// long idle_s1 = Long.parseLong(tok[5]);
	// long cpu_s1 = Long.parseLong(tok[2]) + Long.parseLong(tok[3])
	// + Long.parseLong(tok[4]) + Long.parseLong(tok[6])
	// + Long.parseLong(tok[5]) + Long.parseLong(tok[7])
	// + Long.parseLong(tok[8]);
	//
	// try {
	// Thread.sleep(1000);
	//
	// } catch (Exception e) {
	// }
	//
	// reader_stat.seek(0);
	//
	// load_info = reader_stat.readLine();
	//
	// reader_stat.close();
	//
	// tok = load_info.split(" ");
	// long idle_s2 = Long.parseLong(tok[5]);
	//
	// long cpu_s2 = Long.parseLong(tok[2]) + Long.parseLong(tok[3])
	// + Long.parseLong(tok[4]) + Long.parseLong(tok[6])
	// + Long.parseLong(tok[5]) + Long.parseLong(tok[7])
	// + Long.parseLong(tok[8]);
	//
	// return "CPU使用率为："
	// + (100 * ((cpu_s2 - idle_s2) - (cpu_s1 - idle_s1)) / (cpu_s2 - cpu_s1))
	// + "%";
	//
	// }
	//
	// // 双核情况
	// else if (tok2[0].equals("cpu1")) {
	// // 双核
	// reader_stat = new RandomAccessFile("/proc/stat", "r");
	// long[] idle_d1 = null;
	// long[] cpu_d1 = null;
	// long[] idle_d2 = null;
	// long[] cpu_d2 = null;
	// idle_d1[0] = Long.parseLong(tok1[4]); // cpu0空闲时间
	// cpu_d1[0] = Long.parseLong(tok1[2]) + Long.parseLong(tok1[3])
	// + Long.parseLong(tok1[4]) + Long.parseLong(tok1[6])
	// + Long.parseLong(tok1[5]) + Long.parseLong(tok1[7])
	// + Long.parseLong(tok1[1]); // cpu0非空闲时间
	// idle_d1[1] = Long.parseLong(tok2[4]);
	// cpu_d1[1] = Long.parseLong(tok2[2]) + Long.parseLong(tok2[3])
	// + Long.parseLong(tok2[4]) + Long.parseLong(tok2[6])
	// + Long.parseLong(tok2[5]) + Long.parseLong(tok2[7])
	// + Long.parseLong(tok2[1]);
	//
	// try {
	// Thread.sleep(1000);
	//
	// } catch (Exception e) {
	// }
	//
	// reader_stat.seek(0);
	//
	// cpu_stat = reader_stat.readLine(); // cpu行信息
	// cpu0_stat = reader_stat.readLine(); // cpu0
	// cpu1_stat = reader_stat.readLine();
	//
	// tok1 = cpu0_stat.split(" ");
	// tok2 = cpu1_stat.split(" ");
	//
	// idle_d2[0] = Long.parseLong(tok1[4]); // cpu0空闲时间
	// cpu_d2[0] = Long.parseLong(tok1[2]) + Long.parseLong(tok1[3])
	// + Long.parseLong(tok1[4]) + Long.parseLong(tok1[6])
	// + Long.parseLong(tok1[5]) + Long.parseLong(tok1[7])
	// + Long.parseLong(tok1[1]); // cpu0非空闲时间
	// idle_d2[1] = Long.parseLong(tok2[4]);
	// cpu_d2[1] = Long.parseLong(tok2[2]) + Long.parseLong(tok2[3])
	// + Long.parseLong(tok2[4]) + Long.parseLong(tok2[6])
	// + Long.parseLong(tok2[5]) + Long.parseLong(tok2[7])
	// + Long.parseLong(tok2[1]);
	//
	// reader_stat.close();
	// return "CPU1使用率为："
	// + (100 * ((cpu_d2[0] - idle_d2[0]) - (cpu_d1[0] - idle_d1[0])) /
	// (cpu_d2[0] - cpu_d1[0]))
	// + "%"
	// + "\n"
	// + "CPU2使用率为："
	// + (100 * ((cpu_d2[1] - idle_d2[1]) - (cpu_d1[1] - idle_d1[1])) /
	// (cpu_d2[1] - cpu_d1[1]))
	// + "%";
	// }
	// } catch (IOException ex) {
	// Log.e(LOG_TAG, ex.getMessage());
	//
	// }
	// return "0";
	// }
}
