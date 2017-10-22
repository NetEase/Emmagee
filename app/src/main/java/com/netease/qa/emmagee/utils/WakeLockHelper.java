package com.netease.qa.emmagee.utils;

import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

public class WakeLockHelper {
	private WakeLock wakeLock = null;
	private Context context;

	public WakeLockHelper(Context context) {
		this.context = context;
	}
	
	public void acquireFullWakeLock() {
		if (null == wakeLock) {
			PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "WakeLock");
		} 
		wakeLock.acquire();
	}

	public void releaseWakeLock() {
		if (null != wakeLock) {
			wakeLock.release();
			wakeLock = null;
		}
	}
}
