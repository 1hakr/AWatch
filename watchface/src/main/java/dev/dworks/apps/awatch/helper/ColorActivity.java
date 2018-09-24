/*
 * Copyright (C) 2017 The Android Open Source Project
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
package dev.dworks.apps.awatch.helper;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;
import dev.dworks.apps.awatch.R;
import dev.dworks.apps.awatch.common.MuzeiArtworkImageLoader;
import dev.dworks.apps.awatch.common.config.ConfigHelper;
import dev.dworks.apps.awatch.common.config.Themes;


public class ColorActivity extends Activity {

    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_color);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        WearableRecyclerView recyclerView = findViewById(R.id.wearable_list);
        recyclerView.setLayoutManager(new WearableLinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setEdgeItemsCenteringEnabled(true);
        recyclerView.setCircularScrollingGestureEnabled(true);
        recyclerView.setBezelFraction(0.5f);
        recyclerView.setScrollDegreesPerScreen(90);

        final boolean hasMuzeiArtwork = MuzeiArtworkImageLoader.hasMuzeiArtwork(this);

        ColorAdapter adapter = new ColorAdapter(this, hasMuzeiArtwork);
        recyclerView.setAdapter(adapter);

        int startingIndex = 0;
        String theme = mSharedPreferences.getString(ConfigHelper.KEY_THEME, null);
        if (theme != null) {
            for (int i = 0; i < Themes.THEMES.length; i++) {
                if (Themes.THEMES[i].id.equals(theme)) {
                    startingIndex = i;
                    break;
                }
            }
        }

        recyclerView.scrollToPosition(startingIndex);
    }
}