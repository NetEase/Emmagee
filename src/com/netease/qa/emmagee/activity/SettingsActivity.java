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

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.netease.qa.emmagee.R;

public class SettingsActivity extends Activity {

	private final String LOG_TAG = "Emmagee-"
			+ SettingsActivity.class.getSimpleName();
	
	private CheckBox chkFloat;
	private EditText edtTime;
	private String time;
	private String settingTempFile;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(LOG_TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);

		Intent intent = this.getIntent();
		settingTempFile = intent.getStringExtra("settingTempFile");
		
		chkFloat = (CheckBox) findViewById(R.id.floating);
		edtTime = (EditText) findViewById(R.id.time);
		Button btnSave = (Button) findViewById(R.id.save);
		boolean floatingTag = true;

		try {
			RandomAccessFile raf = new RandomAccessFile(settingTempFile, "r");
			String f = raf.readLine();
			if (f == null || (f != null && f.equals(""))) {
				time = "5";
			} else
				time = f;
			String tag = raf.readLine();
			if (tag != null && tag.equals("false"))
				floatingTag = false;
		} catch (FileNotFoundException e) {
			Log.e(LOG_TAG,
					"FileNotFoundException: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(LOG_TAG, "IOException: " + e.getMessage());
			e.printStackTrace();
		}

		edtTime.setText(time);
		chkFloat.setChecked(floatingTag);
		// edtTime.setInputType(InputType.TYPE_CLASS_NUMBER); 
		btnSave.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				time = edtTime.getText().toString().trim();
				if (!isNumeric(time)) {
					Toast.makeText(SettingsActivity.this, "输入数据无效，请重新输入",
							Toast.LENGTH_LONG).show();
					edtTime.setText("");
				} else if (time.equals("") || Long.parseLong(time) == 0) {
					Toast.makeText(SettingsActivity.this, "输入数据为空,请重新输入",
							Toast.LENGTH_LONG).show();
					edtTime.setText("");
				} else if (Integer.parseInt(time) > 600) {
					Toast.makeText(SettingsActivity.this, "数据超过最大值600，请重新输入",
							Toast.LENGTH_LONG).show();
				} else {
					try {
						BufferedWriter bw = new BufferedWriter(
								new OutputStreamWriter(new FileOutputStream(
										settingTempFile)));
						time = Integer.toString(Integer.parseInt(time));
						bw.write(time + "\r\n" + chkFloat.isChecked());
						bw.close();
						Toast.makeText(SettingsActivity.this, "保存成功",
								Toast.LENGTH_LONG).show();
						Intent intent = new Intent();
						setResult(Activity.RESULT_FIRST_USER, intent);
						SettingsActivity.this.finish();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	@Override
	public void finish() {
		super.finish();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	/**
	 * is input a number
	 * 
	 * @param inputStr
	 *            input string
	 * @return
	 */
	private boolean isNumeric(String inputStr) {
		for (int i = inputStr.length(); --i >= 0;) {
			if (!Character.isDigit(inputStr.charAt(i))) {
				return false;
			}
		}
		return true;
	}

}
