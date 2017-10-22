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

import com.netease.qa.emmagee.R;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * About Page of Emmagee
 * 
 * @author andrewleo
 */
public class AboutActivity extends Activity {

	private static final String LOG_TAG = "Emmagee-"
			+ AboutActivity.class.getSimpleName();

	private TextView appVersion;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(LOG_TAG, "onCreate");
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.about);
		
		appVersion = (TextView)findViewById(R.id.app_version);
		appVersion.setText(getVersion());
		
		TextView title = (TextView)findViewById(R.id.nb_title);
		title.setText(R.string.about);
		
		ImageView btnSave = (ImageView) findViewById(R.id.btn_set);
		btnSave.setVisibility(ImageView.INVISIBLE);
		
		LinearLayout layGoBack = (LinearLayout) findViewById(R.id.lay_go_back);
		
		layGoBack.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				AboutActivity.this.finish();
			}
		});
	}
	
	/**
	 * get app version
	 * @return app version
	 */
	public String getVersion() {
	    try {
	        PackageManager manager = this.getPackageManager();
	        PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
	        String version = info.versionName;
	        return  version;
	    } catch (Exception e) {
	        e.printStackTrace();
	        return "-";
	    }
	}

	@Override
	public void finish() {
		super.finish();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

}
