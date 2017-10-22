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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.netease.qa.emmagee.R;
import com.netease.qa.emmagee.utils.Settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Test Report
 * 
 * @author andrewleo
 */
public class TestListActivity extends Activity {

	private static final String LOG_TAG = "Emmagee-"
			+ TestListActivity.class.getSimpleName();
	static final String CSV_PATH_KEY = "csvPath";

	private ListAdapter la;
	private ListView lstViReport;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(LOG_TAG, "onCreate");
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.test_list);
		
		TextView title = (TextView)findViewById(R.id.nb_title);
		lstViReport = (ListView)findViewById(R.id.test_list);
		ImageView btnSave = (ImageView) findViewById(R.id.btn_set);
		
		btnSave.setVisibility(ImageView.INVISIBLE);
		title.setText(R.string.test_report);
		
		LinearLayout layGoBack = (LinearLayout) findViewById(R.id.lay_go_back);
		
		layGoBack.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				TestListActivity.this.finish();
			}
		});
		la = new ListAdapter(listReports());
		lstViReport.setAdapter(la);
		lstViReport.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				Intent intent = new Intent();
				intent.setClass(TestListActivity.this, TestReportActivity.class);
				intent.putExtra(CSV_PATH_KEY, la.getCSVPath(i));
				startActivity(intent);
			}
		});
	}
	
	/**
	 * customizing adapter.
	 * 
	 * @author andrewleo
	 */
	private class ListAdapter extends BaseAdapter {
		List<String> reports;

		public ListAdapter(List<String> reports) {
			this.reports = reports;
		}

		@Override
		public int getCount() {
			return reports.size();
		}

		@Override
		public Object getItem(int position) {
			return reports.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
		
		public String getCSVPath(int position) {
			return Settings.EMMAGEE_RESULT_DIR + getItem(position) + ".csv";
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			String pr = (String) reports.get(position);
			if (convertView == null)
				convertView = getLayoutInflater().inflate(R.layout.test_list_item, parent, false);
			Viewholder holder = (Viewholder) convertView.getTag();
			if (holder == null) {
				holder = new Viewholder();
				convertView.setTag(holder);
				holder.name = (TextView) convertView.findViewById(R.id.package_name);
			}
			holder.name.setText(pr);
			return convertView;
		}

	}
	
	private static class Viewholder {
		TextView name;
	}
	
	/**
	 * list all test report
	 */
	private ArrayList<String> listReports() {
		ArrayList<String> reportList = new ArrayList<String>();
		File reportDir = new File(Settings.EMMAGEE_RESULT_DIR);
		if (reportDir.isDirectory()) {
			File files[] = reportDir.listFiles();
			Arrays.sort(files, Collections.reverseOrder());
			for (File file: files) {
				if (isLegalReport(file)) {
					String baseName = file.getName().substring(0, file.getName().lastIndexOf("."));
					reportList.add(baseName);
				}
			}
		}
		return reportList;
	}
	
	private boolean isLegalReport(File file) {
		return !file.isDirectory() && file.getName().endsWith(".csv");
	}
}
