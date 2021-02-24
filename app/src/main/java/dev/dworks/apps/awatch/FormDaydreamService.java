package dev.dworks.apps.awatch;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.service.dreams.DreamService;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import dev.dworks.apps.awatch.common.FormClockView;
import dev.dworks.apps.awatch.common.config.ConfigHelper;
import dev.dworks.apps.awatch.common.config.Themes;

public class FormDaydreamService extends DreamService {

    private ViewGroup mMainClockContainerView;
    private FormClockView mMainClockView;
    private SharedPreferences mSharedPreferences;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        setInteractive(true);
        setFullscreen(true);
        setScreenBright(false);
        setContentView(R.layout.form_daydream);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mMainClockContainerView = (ViewGroup) ((ViewGroup) findViewById(R.id.clock_container)).getChildAt(0);
        mMainClockView = (FormClockView) mMainClockContainerView.findViewById(R.id.clock);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        showClock();
    }

    private void showClock() {
        String themeId = mSharedPreferences.getString(ConfigHelper.KEY_THEME, Themes.DEFAULT_THEME.id);
        Themes.Theme theme = Themes.getThemeById(themeId);

        ((ImageView) mMainClockContainerView.findViewById(R.id.background_image))
                .setImageDrawable(null);
        mMainClockView.setColors(
                ContextCompat.getColor(this, theme.lightRes),
                ContextCompat.getColor(this, theme.midRes),
                Color.WHITE);
        mMainClockContainerView.setBackgroundColor(
                ContextCompat.getColor(this, theme.defaultRes));
    }
}
