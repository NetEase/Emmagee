package com.netease.qa.emmagee.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class FpsInfo {

	private static Process process;
	private static BufferedReader ir;
	private static DataOutputStream os = null;
	private static long startTime = 0L;
	private static int lastFrameNum = 0;
	private static boolean ok = true;

	/**
	 * get frame per second
	 * 
	 * @return frame per second
	 */
	public static float fps() {
		if (ok) {
			long nowTime = System.nanoTime();
			float f = (float) (nowTime - startTime) / 1000000.0F;
			startTime = nowTime;
			int nowFrameNum = getFrameNum();
			final float fps = Math.round((nowFrameNum - lastFrameNum) * 1000
					/ f);
			lastFrameNum = nowFrameNum;
			return fps;
		} else {
			return -1;
		}

	}

	/**
	 * get frame value
	 * 
	 * @return frame value
	 */
	public static final int getFrameNum() {
		try {
			if (process == null) {
				process = Runtime.getRuntime().exec("su");
				os = new DataOutputStream(process.getOutputStream());
				ir = new BufferedReader(new InputStreamReader(
						process.getInputStream()));
			}
			os.writeBytes("service call SurfaceFlinger 1013" + "\n");
			os.flush();
			String str1 = ir.readLine();
			if (str1 != null) {
				int start = str1.indexOf("(");
				int end = str1.indexOf("  ");
				if ((start != -1) & (end > start)) {
					String str2 = str1.substring(start + 1, end);
					return Integer.parseInt((String) str2, 16);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		ok = false;
		return -1;
	}
}
