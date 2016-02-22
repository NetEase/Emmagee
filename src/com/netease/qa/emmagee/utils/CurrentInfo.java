package com.netease.qa.emmagee.utils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.Locale;

import android.os.Build;
import android.util.Log;

/**
 * Current info
 * 
 * @author andrewleo
 * 
 */
public class CurrentInfo {
	private static final String LOG_TAG = "Emmagee-CurrentInfo";
	private static final String BUILD_MODEL = Build.MODEL.toLowerCase(Locale.ENGLISH);
	private static final String I_MBAT = "I_MBAT: ";
	private static final String CURRENT_NOW = "/sys/class/power_supply/battery/current_now";
	private static final String BATT_CURRENT = "/sys/class/power_supply/battery/batt_current";
	private static final String SMEM_TEXT = "/sys/class/power_supply/battery/smem_text";
	private static final String BATT_CURRENT_ADC = "/sys/class/power_supply/battery/batt_current_adc";
	private static final String CURRENT_AVG = "/sys/class/power_supply/battery/current_avg";

	/**
	 * read system file to get current value
	 * 
	 * @return current value
	 */
	public Long getCurrentValue() {
		File f = null;
		Log.d(LOG_TAG, BUILD_MODEL);
		// galaxy s4,oppo find,samgsung note2
		if (BUILD_MODEL.contains("sgh-i337") || BUILD_MODEL.contains("gt-i9505") || BUILD_MODEL.contains("sch-i545")
				|| BUILD_MODEL.contains("find 5") || BUILD_MODEL.contains("sgh-m919") || BUILD_MODEL.contains("sgh-i537")
				|| BUILD_MODEL.contains("x907") || BUILD_MODEL.contains("gt-n7100")) {
			f = new File(CURRENT_NOW);
			if (f.exists()) {
				return getCurrentValue(f, false);
			}
		}

		// samsung galaxy
		if (BUILD_MODEL.contains("gt-p31") || BUILD_MODEL.contains("gt-p51")) {
			f = new File(CURRENT_AVG);
			if (f.exists()) {
				return getCurrentValue(f, false);
			}
		}

		// htc desire hd ,desire z
		if (BUILD_MODEL.contains("desire hd") || BUILD_MODEL.contains("desire z")) {
			f = new File(BATT_CURRENT);
			if (f.exists())
				return getCurrentValue(f, false);
		}

		// htc sensation z710e
		f = new File(BATT_CURRENT);
		if (f.exists())
			return getCurrentValue(f, false);

		// htc one V
		f = new File(SMEM_TEXT);
		if (f.exists())
			return getSMemValue();

		// nexus one,meizu
		f = new File(CURRENT_NOW);
		if (f.exists())
			return getCurrentValue(f, true);

		// meizu pro 5
		f = new File("/sys/class/power_supply/bq2753x-0/current_now");
		if (f.exists())
			return getCurrentValue(f, true);

		// galaxy note, galaxy s2
		f = new File(BATT_CURRENT_ADC);
		if (f.exists())
			return getCurrentValue(f, false);

		// acer V360
		f = new File("/sys/class/power_supply/battery/BatteryAverageCurrent");
		if (f.exists())
			return getCurrentValue(f, false);

		// moto milestone,moto mb526
		f = new File("/sys/devices/platform/cpcap_battery/power_supply/usb/current_now");
		if (f.exists())
			return getCurrentValue(f, false);

		return null;
	}

	/**
	 * get current value from smem_text
	 * 
	 * @return current value
	 */
	public Long getSMemValue() {
		boolean success = false;
		String text = null;
		Long value = null;
		try {
			FileReader fr = new FileReader(SMEM_TEXT);
			BufferedReader br = new BufferedReader(fr);
			String line = br.readLine();
			while (line != null) {
				if (line.contains(I_MBAT)) {
					text = line.substring(line.indexOf(I_MBAT) + 8);
					success = true;
					break;
				}
				line = br.readLine();
			}
			fr.close();
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (success) {
			try {
				value = Long.parseLong(text);
			} catch (NumberFormatException nfe) {
				nfe.printStackTrace();
				value = null;
			}
		}
		return value;
	}

	/**
	 * read system file to get current value
	 * 
	 * @param file
	 * @param convertToMillis 
	 * @return current value
	 */
	public Long getCurrentValue(File file, boolean convertToMillis) {
		Log.d(LOG_TAG, "*** getCurrentValue ***");
		Log.d(LOG_TAG, "*** " + convertToMillis + " ***");
		String line = null;
		Long value = null;
		FileInputStream fs = null;
		DataInputStream ds = null;
		try {
			fs = new FileInputStream(file);
			ds = new DataInputStream(fs);
			line = ds.readLine();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				fs.close();
				ds.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		if (line != null) {
			try {
				value = Long.parseLong(line);
			} catch (NumberFormatException nfe) {
				value = null;
			}
			if (convertToMillis)
				value = value / 1000;
		}
		return value;
	}
}
