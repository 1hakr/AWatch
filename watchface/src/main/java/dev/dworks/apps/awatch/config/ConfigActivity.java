/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.dworks.apps.awatch.config;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.ProviderChooserIntent;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;
import dev.dworks.apps.awatch.R;
import dev.dworks.apps.awatch.common.config.ConfigHelper;
import dev.dworks.apps.awatch.common.config.UpdateConfigIntentService;

public class ConfigActivity extends Activity {

    static final int COMPLICATION_CONFIG_REQUEST_CODE = 1001;
    static final int UPDATE_COLORS_CONFIG_REQUEST_CODE = 1002;

    private SharedPreferences mSharedPreferences;
    private WearableRecyclerView mWearableRecyclerView;
    private ConfigAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_DeviceDefault);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        registerSharedPrefsListener();

        mAdapter = new ConfigAdapter(this);
        mWearableRecyclerView = findViewById(R.id.wearable_recycler_view);
        mWearableRecyclerView.setEdgeItemsCenteringEnabled(true);
        mWearableRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mWearableRecyclerView.setHasFixedSize(true);
        mWearableRecyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterSharedPrefsListener();
    }

    private void registerSharedPrefsListener() {
        mSharedPreferences.registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
    }

    private void unregisterSharedPrefsListener() {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
    }

    private SharedPreferences.OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener
            = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (ConfigHelper.isConfigPrefKey(key)) {
                UpdateConfigIntentService.startConfigChangeService(ConfigActivity.this);
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            UpdateConfigIntentService.startConfigChangeService(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == COMPLICATION_CONFIG_REQUEST_CODE
                && resultCode == RESULT_OK) {

            // Retrieves information for selected Complication provider.
            ComplicationProviderInfo complicationProviderInfo =
                    data.getParcelableExtra(ProviderChooserIntent.EXTRA_PROVIDER_INFO);
            mAdapter.updateSelectedComplication(complicationProviderInfo);

        } else if (requestCode == UPDATE_COLORS_CONFIG_REQUEST_CODE
                && resultCode == RESULT_OK) {

            // Updates highlight and background colors based on the user preference.
            mAdapter.updatePreviewColors();
        }
    }
}
