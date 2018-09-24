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

package dev.dworks.apps.awatch.watchface;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.WindowInsets;
import android.view.animation.DecelerateInterpolator;

import com.google.android.apps.muzei.api.MuzeiContract;

import java.util.Calendar;

import androidx.core.content.ContextCompat;
import dev.dworks.apps.awatch.R;
import dev.dworks.apps.awatch.common.FormClockRenderer;
import dev.dworks.apps.awatch.common.MathUtil;
import dev.dworks.apps.awatch.common.config.ConfigHelper;
import dev.dworks.apps.awatch.common.config.Themes;
import dev.dworks.apps.awatch.config.ConfigAdapter;
import dev.dworks.apps.awatch.helper.WatchfaceArtworkImageLoader;

import static android.support.wearable.watchface.WatchFaceStyle.PROTECT_HOTWORD_INDICATOR;
import static dev.dworks.apps.awatch.common.FormClockRenderer.ClockPaints;
import static dev.dworks.apps.awatch.common.MathUtil.constrain;
import static dev.dworks.apps.awatch.common.MathUtil.decelerate3;
import static dev.dworks.apps.awatch.common.MathUtil.interpolate;
import static dev.dworks.apps.awatch.common.MuzeiArtworkImageLoader.LoadedArtwork;
import static dev.dworks.apps.awatch.common.config.Themes.MUZEI_THEME;
import static dev.dworks.apps.awatch.common.config.Themes.Theme;
import static dev.dworks.apps.awatch.helper.LogUtil.LOGD;

public class FormWatchFace extends CanvasWatchFaceService {
    private static final String TAG = "FormWatchFace";

    private static final int UPDATE_THEME_ANIM_DURATION = 1000;

    // Unique IDs for each complication. The settings activity that supports allowing users
    // to select their complication data provider requires numbers to be >= 0.
    private static final int BACKGROUND_COMPLICATION_ID = 0;

    private static final int TOP_COMPLICATION_ID = 100;
    private static final int BOTTOM_COMPLICATION_ID = 101;

    // Background, Top and Bottom complication IDs as array for Complication API.
    private static final int[] COMPLICATION_IDS = {
            BACKGROUND_COMPLICATION_ID, TOP_COMPLICATION_ID, BOTTOM_COMPLICATION_ID
    };

    // Top and Bottom dial supported types.
    private static final int[][] COMPLICATION_SUPPORTED_TYPES = {
            {   ComplicationData.TYPE_LARGE_IMAGE  },
            {
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_SMALL_IMAGE,
                ComplicationData.TYPE_LONG_TEXT,
            },
            {
                ComplicationData.TYPE_RANGED_VALUE,
                ComplicationData.TYPE_ICON,
                ComplicationData.TYPE_SHORT_TEXT,
                ComplicationData.TYPE_SMALL_IMAGE,
                ComplicationData.TYPE_LONG_TEXT
            }
    };

    // Used by {@link ConfigAdapter} to check if complication location
    // is supported in settings config activity.
    public static int getComplicationId(
            ConfigAdapter.ComplicationLocation complicationLocation) {
        // Add any other supported locations here.
        switch (complicationLocation) {
            case BACKGROUND:
                return BACKGROUND_COMPLICATION_ID;
            case TOP:
                return TOP_COMPLICATION_ID;
            case BOTTOM:
                return BOTTOM_COMPLICATION_ID;
            default:
                return -1;
        }
    }

    // Used by {@link ConfigAdapter} to retrieve all complication ids.
    public static int[] getComplicationIds() {
        return COMPLICATION_IDS;
    }

    // Used by {@link ConfigAdapter} to see which complication types
    // are supported in the settings config activity.
    public static int[] getSupportedComplicationTypes(
            ConfigAdapter.ComplicationLocation complicationLocation) {
        // Add any other supported locations here.
        switch (complicationLocation) {
            case BACKGROUND:
                return COMPLICATION_SUPPORTED_TYPES[0];
            case TOP:
                return COMPLICATION_SUPPORTED_TYPES[1];
            case BOTTOM:
                return COMPLICATION_SUPPORTED_TYPES[2];
            default:
                return new int[] {};
        }
    }

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        private Paint mAmbientBackgroundPaint;
        private Paint mBackgroundPaint;

        private boolean mMute;
        private Rect mCardBounds = new Rect();
        private ValueAnimator mBottomBoundAnimator = new ValueAnimator();
        private ValueAnimator mSecondsAlphaAnimator = new ValueAnimator();
        private int mWidth = 0;
        private int mHeight = 0;
        private int mDisplayMetricsWidth = 0;
        private int mDisplayMetricsHeight = 0;

        private Handler mMainThreadHandler = new Handler();

        // For Muzei
        private WatchfaceArtworkImageLoader mMuzeiLoader;
        private Paint mMuzeiArtworkPaint;
        private LoadedArtwork mMuzeiLoadedArtwork;

        // FORM clock renderer specific stuff
        private FormClockRenderer mHourMinRenderer;
        private FormClockRenderer mSecondsRenderer;
        private long mUpdateThemeStartAnimTimeMillis;
        private long mLastDrawTimeMin;
        private String mDateStr;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;

        private boolean mShowNotificationCount;
        private boolean mShowSeconds;
        private boolean mShowDate;

        private Typeface mDateTypeface;
        private ClockPaints mNormalPaints;
        private ClockPaints mAmbientPaints;
        private boolean mDrawMuzeiBitmap;
        private Theme mCurrentTheme;
        private Theme mAnimateFromTheme;
        private Path mUpdateThemeClipPath = new Path();
        private RectF mTempRectF = new RectF();

        private boolean mAmbient;
        private boolean mIsRound;
        private int mChinSize;
        private SparseArray<ComplicationData> mActiveComplicationDataSparseArray;
        private SparseArray<ComplicationDrawable> mComplicationDrawableSparseArray;
        private int mHighlightColor;

        @Override
        public void onCreate(SurfaceHolder holder) {
            LOGD(TAG, "onCreate");
            super.onCreate(holder);

            updateDateStr();

            mMute = getInterruptionFilter() == WatchFaceService.INTERRUPTION_FILTER_NONE;
            handleConfigUpdated();

            mDateTypeface = Typeface.createFromAsset(getAssets(), "VT323-Regular.ttf");
            initClockRenderers();

            registerSystemSettingsListener();
            registerSharedPrefsListener();
            registerTimeZoneReceiver();

            initMuzei();
            initComplications();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            unregisterSystemSettingsListener();
            unregisterSharedPrefsListener();
            unregisterTimeZoneReceiver();
            destroyMuzei();
        }

        private void initClockRenderers() {
            // Init paints
            mAmbientBackgroundPaint = new Paint();
            mAmbientBackgroundPaint.setColor(Color.BLACK);
            mBackgroundPaint = new Paint();

            Paint paint = new Paint();
            paint.setAntiAlias(true);
            mNormalPaints = new ClockPaints();
            mNormalPaints.fills[0] = paint;
            mNormalPaints.fills[1] = new Paint(paint);
            mNormalPaints.fills[2] = new Paint(paint);
            mNormalPaints.date = new Paint(paint);
            mNormalPaints.date.setTypeface(mDateTypeface);
            mNormalPaints.date.setTextSize(
                    getResources().getDimensionPixelSize(R.dimen.seconds_clock_height));

            rebuildAmbientPaints();

            // General config
            FormClockRenderer.Options options = new FormClockRenderer.Options();

            options.is24hour = DateFormat.is24HourFormat(FormWatchFace.this);
            options.textSize = getResources().getDimensionPixelSize(R.dimen.main_clock_height);
            options.charSpacing = getResources().getDimensionPixelSize(R.dimen.main_clock_spacing);
            options.glyphAnimAverageDelay = getResources().getInteger(R.integer.main_clock_glyph_anim_delay);
            options.glyphAnimDuration = getResources().getInteger(R.integer.main_clock_glyph_anim_duration);

            mHourMinRenderer = new FormClockRenderer(options, mNormalPaints);

            options = new FormClockRenderer.Options(options);
            options.textSize = getResources().getDimensionPixelSize(R.dimen.seconds_clock_height);
            options.onlySeconds = true;
            options.charSpacing = getResources().getDimensionPixelSize(R.dimen.seconds_clock_spacing);
            options.glyphAnimAverageDelay = getResources().getInteger(R.integer.seconds_clock_glyph_anim_delay);
            options.glyphAnimDuration = getResources().getInteger(R.integer.seconds_clock_glyph_anim_duration);

            mSecondsRenderer = new FormClockRenderer(options, mNormalPaints);
        }

        private void handleConfigUpdated() {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(FormWatchFace.this);
            String themeId = sp.getString(ConfigHelper.KEY_THEME, Themes.DEFAULT_THEME.id);
            Theme newCurrentTheme = Themes.getThemeById(themeId);
            if (newCurrentTheme != mCurrentTheme) {
                mAnimateFromTheme = mCurrentTheme;
                mCurrentTheme = newCurrentTheme;
                mUpdateThemeStartAnimTimeMillis = System.currentTimeMillis() + 200;
            }

            mShowNotificationCount = !sp.getBoolean(ConfigHelper.KEY_SHOW_NOTIFICATION_COUNT, true);
            mShowSeconds = sp.getBoolean(ConfigHelper.KEY_SHOW_SECONDS, false);
            mShowDate = sp.getBoolean(ConfigHelper.KEY_SHOW_DATE, false);
            mHighlightColor = ContextCompat.getColor(FormWatchFace.this, mCurrentTheme.lightRes);

            updateWatchFaceStyle();
            postInvalidate();
        }

        private void updateWatchFaceStyle() {
            setWatchFaceStyle(new WatchFaceStyle.Builder(FormWatchFace.this)
                    .setAccentColor(ContextCompat.getColor(getBaseContext(), mCurrentTheme.midRes))
                    .setAcceptsTapEvents(true)
                    .setStatusBarGravity(Gravity.TOP | Gravity.CENTER)
                    .setViewProtectionMode(PROTECT_HOTWORD_INDICATOR)
                    .setShowUnreadCountIndicator(mShowNotificationCount && !mMute)
                    .build());
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            mWidth = width;
            mHeight = height;

            DisplayMetrics dm = getResources().getDisplayMetrics();
            mDisplayMetricsWidth = dm.widthPixels;
            mDisplayMetricsHeight = dm.heightPixels;

            mBottomBoundAnimator.cancel();
            mBottomBoundAnimator.setFloatValues(mHeight, mHeight);
            mBottomBoundAnimator.setInterpolator(new DecelerateInterpolator(3));
            mBottomBoundAnimator.setDuration(0);
            mBottomBoundAnimator.start();

            mSecondsAlphaAnimator.cancel();
            mSecondsAlphaAnimator.setFloatValues(1f, 1f);
            mSecondsAlphaAnimator.setDuration(0);
            mSecondsAlphaAnimator.start();

            updateComplicationBound(height, width);
        }

        @Override
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
            mIsRound = insets.isRound();
            mChinSize = insets.getSystemWindowInsetBottom();
            updateWatchFaceStyle();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            setComplicationsActiveAndAmbientColors();
            if (visible) {
                postInvalidate();
            }
        }

        private void initMuzei() {
            mMuzeiArtworkPaint = new Paint();
            mMuzeiArtworkPaint.setAlpha(102);
            mMuzeiLoader = new WatchfaceArtworkImageLoader(FormWatchFace.this);
            mMuzeiLoader.registerListener(0, mMuzeiLoadCompleteListener);
            mMuzeiLoader.startLoading();

            // Watch for artwork changes
            IntentFilter artworkChangedIntent = new IntentFilter();
            artworkChangedIntent.addAction(MuzeiContract.Artwork.ACTION_ARTWORK_CHANGED);
            registerReceiver(mMuzeiArtworkChangedReceiver, artworkChangedIntent);
        }

        private BroadcastReceiver mMuzeiArtworkChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mMuzeiLoader.startLoading();
            }
        };

        private void destroyMuzei() {
            unregisterReceiver(mMuzeiArtworkChangedReceiver);
            if (mMuzeiLoader != null) {
                mMuzeiLoader.unregisterListener(mMuzeiLoadCompleteListener);
                mMuzeiLoader.reset();
                mMuzeiLoader = null;
            }
        }

        private Loader.OnLoadCompleteListener<LoadedArtwork> mMuzeiLoadCompleteListener
                = new Loader.OnLoadCompleteListener<LoadedArtwork>() {
            public void onLoadComplete(Loader<LoadedArtwork> loader, LoadedArtwork data) {
                if (data != null) {
                    mMuzeiLoadedArtwork = data;
                } else {
                    mMuzeiLoadedArtwork = null;
                }
                postInvalidate();
            }
        };

        private void registerSystemSettingsListener() {
            getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(Settings.System.TIME_12_24),
                    false, mSystemSettingsObserver);
        }

        private void unregisterSystemSettingsListener() {
            getContentResolver().unregisterContentObserver(mSystemSettingsObserver);
        }

        private ContentObserver mSystemSettingsObserver = new ContentObserver(mMainThreadHandler) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                initClockRenderers();
                postInvalidate();
            }
        };

        private void registerSharedPrefsListener() {
            PreferenceManager.getDefaultSharedPreferences(FormWatchFace.this)
                    .registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
        }

        private void unregisterSharedPrefsListener() {
            PreferenceManager.getDefaultSharedPreferences(FormWatchFace.this)
                    .unregisterOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
        }

        private SharedPreferences.OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener
                = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (ConfigHelper.isConfigPrefKey(key)) {
                    handleConfigUpdated();
                }
            }
        };

        private void registerTimeZoneReceiver() {
            IntentFilter timeZoneIntentFilter = new IntentFilter();
            timeZoneIntentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            registerReceiver(mTimeZoneReceiver, timeZoneIntentFilter);
        }

        private void unregisterTimeZoneReceiver() {
            unregisterReceiver(mTimeZoneReceiver);
        }

        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
                    initClockRenderers();
                    postInvalidate();
                }
            }
        };

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);

            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);

            rebuildAmbientPaints();
            // Updates complications to properly render in ambient mode based on the
            // screen's capabilities.
            ComplicationDrawable complicationDrawable;

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationDrawable = mComplicationDrawableSparseArray.get(COMPLICATION_IDS[i]);

                complicationDrawable.setLowBitAmbient(mLowBitAmbient);
                complicationDrawable.setBurnInProtection(mBurnInProtection);
            }


            LOGD(TAG, "onPropertiesChanged: burn-in protection = " + mBurnInProtection
                    + ", low-bit ambient = " + mLowBitAmbient);
        }

        private void rebuildAmbientPaints() {
            Paint paint = new Paint();
            mAmbientPaints = new ClockPaints();
            if (mBurnInProtection || mLowBitAmbient) {
                paint.setAntiAlias(false);
                paint.setColor(Color.BLACK);
                mAmbientPaints.fills[0] = mAmbientPaints.fills[1] = mAmbientPaints.fills[2] = paint;

                paint = new Paint();
                paint.setAntiAlias(!mLowBitAmbient);

                mAmbientPaints.date = new Paint(paint);
                mAmbientPaints.date.setColor(Color.WHITE);

                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5,
                        getResources().getDisplayMetrics()));
                paint.setStrokeJoin(Paint.Join.BEVEL);
                paint.setColor(Color.WHITE);
                mAmbientPaints.strokes[0] = mAmbientPaints.strokes[1] = mAmbientPaints.strokes[2]
                        = paint;
                mAmbientPaints.hasStroke = true;

            } else {
                paint.setAntiAlias(true);
                mAmbientPaints.fills[0] = paint;
                mAmbientPaints.fills[0].setColor(0xFFCCCCCC);

                mAmbientPaints.fills[1] = new Paint(paint);
                mAmbientPaints.fills[1].setColor(0xFFAAAAAA);

                mAmbientPaints.fills[2] = new Paint(paint);
                mAmbientPaints.fills[2].setColor(Color.WHITE);

                mAmbientPaints.date = new Paint(paint);
                mAmbientPaints.date.setColor(0xFFCCCCCC);
            }

            mAmbientPaints.date.setTypeface(mDateTypeface);
            mAmbientPaints.date.setTextSize(
                    getResources().getDimensionPixelSize(R.dimen.seconds_clock_height));
        }

        @Override
        public void onPeekCardPositionUpdate(Rect bounds) {
            super.onPeekCardPositionUpdate(bounds);
            LOGD(TAG, "onPeekCardPositionUpdate: " + bounds);
            if (!bounds.equals(mCardBounds)) {
                mCardBounds.set(bounds);

                mBottomBoundAnimator.cancel();
                mBottomBoundAnimator.setFloatValues(
                        (Float) mBottomBoundAnimator.getAnimatedValue(),
                        mCardBounds.top > 0 ? mCardBounds.top : mHeight);
                mBottomBoundAnimator.setDuration(200);
                mBottomBoundAnimator.start();

                mSecondsAlphaAnimator.cancel();
                mSecondsAlphaAnimator.setFloatValues(
                        (Float) mSecondsAlphaAnimator.getAnimatedValue(),
                        mCardBounds.top > 0 ? 0f : 1f);
                mSecondsAlphaAnimator.setDuration(200);
                mSecondsAlphaAnimator.start();

                LOGD(TAG, "onPeekCardPositionUpdate: " + mCardBounds);
                postInvalidate();
            }
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            LOGD(TAG, "onTimeTick: ambient = " + isInAmbientMode());
            postInvalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            LOGD(TAG, "onAmbientModeChanged: " + inAmbientMode);
            super.onAmbientModeChanged(inAmbientMode);
            mAmbient = inAmbientMode;

            // Update drawable complications' ambient state.
            // Note: ComplicationDrawable handles switching between active/ambient colors, we just
            // have to inform it to enter ambient mode.
            ComplicationDrawable complicationDrawable;

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationDrawable = mComplicationDrawableSparseArray.get(COMPLICATION_IDS[i]);
                complicationDrawable.setInAmbientMode(mAmbient);
            }

            postInvalidate();
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            LOGD(TAG, "onInterruptionFilterChanged: " + interruptionFilter);
            super.onInterruptionFilterChanged(interruptionFilter);

            boolean inMuteMode = interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE;

            if (mMute != inMuteMode) {
                mMute = inMuteMode;
                updateWatchFaceStyle();
                postInvalidate();
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            drawWatchFace(canvas);
            drawComplications(canvas);
        }

        private void drawWatchFace(Canvas canvas) {
            updatePaintsForTheme(mCurrentTheme);

            // Figure out what to animate
            long currentTimeMillis = System.currentTimeMillis();
            long currentTimeMin = currentTimeMillis / 60000;
            if (currentTimeMin != mLastDrawTimeMin) {
                mLastDrawTimeMin = currentTimeMin;
                updateDateStr();
            }

            mHourMinRenderer.setPaints(mAmbient ? mAmbientPaints : mNormalPaints);
            mSecondsRenderer.setPaints(mAmbient ? mAmbientPaints : mNormalPaints);

            mHourMinRenderer.updateTime();

            if (mShowSeconds) {
                mSecondsRenderer.updateTime();
            }

            if (mAmbient) {
                drawClock(canvas);
            } else {
                int sc = -1;
                if (isAnimatingThemeChange()) {
                    // show a reveal animation
                    updatePaintsForTheme(mAnimateFromTheme);
                    drawClock(canvas);

                    sc = canvas.save();

                    mUpdateThemeClipPath.reset();
                    float cx = mWidth / 2;
                    float bottom = (Float) mBottomBoundAnimator.getAnimatedValue();
                    float cy = bottom / 2;
                    float maxRadius = MathUtil.maxDistanceToCorner(0, 0, mWidth, mHeight, cx, cy);
                    float radius = interpolate(
                            decelerate3(constrain(
                                    (currentTimeMillis - mUpdateThemeStartAnimTimeMillis)
                                            * 1f / UPDATE_THEME_ANIM_DURATION,
                                    0 , 1)),
                            0 , maxRadius);

                    mTempRectF.set(cx - radius, cy - radius, cx + radius, cy + radius);
                    mUpdateThemeClipPath.addOval(mTempRectF, Path.Direction.CW);
                    canvas.clipPath(mUpdateThemeClipPath);
                }

                updatePaintsForTheme(mCurrentTheme);
                drawClock(canvas);

                if (sc >= 0) {
                    canvas.restoreToCount(sc);
                }
            }

            if (mBottomBoundAnimator.isRunning() || isAnimatingThemeChange()) {
                postInvalidate();
            } else if (isVisible() && !mAmbient) {
                float secondsOpacity = (Float) mSecondsAlphaAnimator.getAnimatedValue();
                boolean showingSeconds = mShowSeconds && secondsOpacity > 0;
                long timeToNextSecondsAnimation = showingSeconds
                        ? mSecondsRenderer.timeToNextAnimation()
                        : 10000;
                long timeToNextHourMinAnimation = mHourMinRenderer.timeToNextAnimation();
                if (timeToNextHourMinAnimation < 0 || timeToNextSecondsAnimation < 0) {
                    postInvalidate();
                } else {
                    mInvalidateHandler.sendEmptyMessageDelayed(0,
                            Math.min(timeToNextHourMinAnimation, timeToNextSecondsAnimation));
                }
            }
        }

        private boolean isAnimatingThemeChange() {
            return mAnimateFromTheme != null
                    && System.currentTimeMillis() - mUpdateThemeStartAnimTimeMillis
                    < UPDATE_THEME_ANIM_DURATION;
        }

        private void updateDateStr() {
            mDateStr = DateFormat.format("EEE d", Calendar.getInstance()).toString().toUpperCase();
        }

        private void updatePaintsForTheme(Theme theme) {
            if (theme == MUZEI_THEME) {
                mBackgroundPaint.setColor(Color.BLACK);
                if (mMuzeiLoadedArtwork != null) {
                    mNormalPaints.fills[0].setColor(mMuzeiLoadedArtwork.color1);
                    mNormalPaints.fills[1].setColor(mMuzeiLoadedArtwork.color2);
                    mNormalPaints.fills[2].setColor(Color.WHITE);
                    mNormalPaints.date.setColor(mMuzeiLoadedArtwork.color1);
                }
                mDrawMuzeiBitmap = true;
            } else {
                Context context = getBaseContext();
                mBackgroundPaint.setColor(ContextCompat.getColor(context, theme.darkRes));
                mNormalPaints.fills[0].setColor(ContextCompat.getColor(context, theme.lightRes));
                mNormalPaints.fills[1].setColor(ContextCompat.getColor(context, theme.midRes));
                mNormalPaints.fills[2].setColor(Color.WHITE);
                mNormalPaints.date.setColor(ContextCompat.getColor(context, theme.lightRes));
                mDrawMuzeiBitmap = false;
            }
        }

        private void drawClock(Canvas canvas) {
            boolean offscreenGlyphs = !mAmbient;

            boolean allowAnimate = !mAmbient;

            if (mAmbient) {
                canvas.drawRect(0, 0, mWidth, mHeight, mAmbientBackgroundPaint);
            } else if (mDrawMuzeiBitmap && mMuzeiLoadedArtwork != null) {
                canvas.drawRect(0, 0, mWidth, mHeight, mAmbientBackgroundPaint);
                canvas.drawBitmap(mMuzeiLoadedArtwork.bitmap,
                        (mDisplayMetricsWidth - mMuzeiLoadedArtwork.bitmap.getWidth()) / 2,
                        (mDisplayMetricsHeight - mMuzeiLoadedArtwork.bitmap.getHeight()) / 2,
                        mMuzeiArtworkPaint);
            } else {
                canvas.drawRect(0, 0, mWidth, mHeight, mBackgroundPaint);
            }

            float bottom = (Float) mBottomBoundAnimator.getAnimatedValue();

            PointF hourMinSize = mHourMinRenderer.measure(allowAnimate);
            mHourMinRenderer.draw(canvas,
                    (mWidth - hourMinSize.x) / 2, (bottom - hourMinSize.y) / 2,
                    allowAnimate,
                    offscreenGlyphs);

            float clockSecondsSpacing = getResources().getDimension(R.dimen.clock_seconds_spacing);
            float secondsOpacity = (Float) mSecondsAlphaAnimator.getAnimatedValue();
            if (mShowSeconds && !mAmbient && secondsOpacity > 0) {
                PointF secondsSize = mSecondsRenderer.measure(allowAnimate);
                int sc = -1;
                if (secondsOpacity != 1) {
                    sc = canvas.saveLayerAlpha(0, 0, canvas.getWidth(), canvas.getHeight(),
                            (int) (secondsOpacity * 255));
                }
                mSecondsRenderer.draw(canvas,
                        (mWidth + hourMinSize.x) / 2 - secondsSize.x,
                        (bottom + hourMinSize.y) / 2 + clockSecondsSpacing,
                        allowAnimate,
                        offscreenGlyphs);
                if (sc >= 0) {
                    canvas.restoreToCount(sc);
                }
            }

            if (mShowDate) {
                Paint paint = mAmbient ? mAmbientPaints.date : mNormalPaints.date;
                float x = (mWidth - hourMinSize.x) / 2;
                if (!mShowSeconds) {
                    x = (mWidth - paint.measureText(mDateStr)) / 2;
                }
                canvas.drawText(
                        mDateStr,
                        x,
                        (bottom + hourMinSize.y) / 2 + clockSecondsSpacing - paint.ascent(),
                                paint);
            }
        }

        private void initComplications() {
            mActiveComplicationDataSparseArray = new SparseArray<>(COMPLICATION_IDS.length);

            // Creates a ComplicationDrawable for each location where the user can render a
            // complication on the watch face. In this watch face, we create one for left, right,
            // and background, but you could add many more.
            ComplicationDrawable topComplicationDrawable =
                    new ComplicationDrawable(getApplicationContext());

            ComplicationDrawable bottomComplicationDrawable =
                    new ComplicationDrawable(getApplicationContext());

            ComplicationDrawable backgroundComplicationDrawable =
                    new ComplicationDrawable(getApplicationContext());

            // Adds new complications to a SparseArray to simplify setting styles and ambient
            // properties for all complications, i.e., iterate over them all.
            mComplicationDrawableSparseArray = new SparseArray<>(COMPLICATION_IDS.length);

            mComplicationDrawableSparseArray.put(TOP_COMPLICATION_ID, topComplicationDrawable);
            mComplicationDrawableSparseArray.put(BOTTOM_COMPLICATION_ID, bottomComplicationDrawable);
            mComplicationDrawableSparseArray.put(
                    BACKGROUND_COMPLICATION_ID, backgroundComplicationDrawable);

            setComplicationsActiveAndAmbientColors();
            setActiveComplications(COMPLICATION_IDS);
        }

        /* Sets active/ambient mode colors for all complications.
         *
         * Note: With the rest of the watch face, we update the paint colors based on
         * ambient/active mode callbacks, but because the ComplicationDrawable handles
         * the active/ambient colors, we only set the colors twice. Once at initialization and
         * again if the user changes the highlight color via AnalogComplicationConfigActivity.
         */
        private void setComplicationsActiveAndAmbientColors() {
            int complicationId;
            ComplicationDrawable complicationDrawable;

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationId = COMPLICATION_IDS[i];
                complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);

                if (complicationId == BACKGROUND_COMPLICATION_ID) {
                    // It helps for the background color to be black in case the image used for the
                    // watch face's background takes some time to load.
                    complicationDrawable.setBackgroundColorActive(Color.BLACK);
                } else {
                    // Active mode colors.
                    //complicationDrawable.setBorderStyleActive(ComplicationDrawable.BORDER_STYLE_NONE);
                    complicationDrawable.setIconColorActive(Color.WHITE);
                    complicationDrawable.setTextColorActive(Color.WHITE);
                    complicationDrawable.setTextTypefaceActive(mDateTypeface);
                    complicationDrawable.setBorderColorActive(mHighlightColor);
                    complicationDrawable.setRangedValuePrimaryColorActive(mHighlightColor);

                    // Ambient mode colors.
                    complicationDrawable.setBorderStyleAmbient(ComplicationDrawable.BORDER_STYLE_NONE);
                    complicationDrawable.setIconColorAmbient(Color.WHITE);
                    complicationDrawable.setTextColorAmbient(Color.WHITE);
                    complicationDrawable.setTextTypefaceAmbient(mDateTypeface);
                    complicationDrawable.setBorderColorAmbient(Color.WHITE);
                    complicationDrawable.setRangedValuePrimaryColorAmbient(Color.WHITE);
                }
            }
        }

        private void updateComplicationBound(int height, int width){
            /*
             * Calculates location bounds for right and left circular complications. Please note,
             * we are not demonstrating a long text complication in this watch face.
             *
             * We suggest using at least 1/4 of the screen width for circular (or squared)
             * complications and 2/3 of the screen width for wide rectangular complications for
             * better readability.
             */

            // For most Wear devices, width and height are the same, so we just chose one (width).
            int sizeOfComplication = width / 4;
            int midpointOfScreen = width / 2;

            int horizontalOffset = (midpointOfScreen - sizeOfComplication) / 2;
            int verticalOffset = midpointOfScreen - (sizeOfComplication / 2);
            int extra = horizontalOffset/2;

            Rect topBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            verticalOffset,
                            horizontalOffset,
                            (verticalOffset + sizeOfComplication),
                            (horizontalOffset + sizeOfComplication));

            ComplicationDrawable topComplicationDrawable =
                    mComplicationDrawableSparseArray.get(TOP_COMPLICATION_ID);
            topComplicationDrawable.setBounds(topBounds);

            Rect bottomBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            verticalOffset - extra,
                            (midpointOfScreen + horizontalOffset + horizontalOffset),
                            (verticalOffset + sizeOfComplication + extra),
                            (midpointOfScreen + horizontalOffset + sizeOfComplication + extra));

            ComplicationDrawable bottomComplicationDrawable =
                    mComplicationDrawableSparseArray.get(BOTTOM_COMPLICATION_ID);
            bottomComplicationDrawable.setBounds(bottomBounds);

            Rect screenForBackgroundBound =
                    // Left, Top, Right, Bottom
                    new Rect(0, 0, width, height);

            ComplicationDrawable backgroundComplicationDrawable =
                    mComplicationDrawableSparseArray.get(BACKGROUND_COMPLICATION_ID);
            backgroundComplicationDrawable.setBounds(screenForBackgroundBound);
        }

        private void drawComplications(Canvas canvas) {
            if(mAmbient){
                return;
            }
            long currentTimeMillis = System.currentTimeMillis();
            int complicationId;
            ComplicationDrawable complicationDrawable;

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationId = COMPLICATION_IDS[i];
                complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);
                complicationDrawable.setBorderColorActive(mHighlightColor);
                complicationDrawable.setRangedValuePrimaryColorActive(mHighlightColor);
                complicationDrawable.draw(canvas, currentTimeMillis);
            }
        }

        @Override
        public void onComplicationDataUpdate(int complicationId, ComplicationData complicationData) {
            super.onComplicationDataUpdate(complicationId, complicationData);
            Log.d(TAG, "onComplicationDataUpdate() id: " + complicationId);

            // Adds/updates active complication data in the array.
            mActiveComplicationDataSparseArray.put(complicationId, complicationData);

            // Updates correct ComplicationDrawable with updated data.
            ComplicationDrawable complicationDrawable =
                    mComplicationDrawableSparseArray.get(complicationId);
            complicationDrawable.setComplicationData(complicationData);

            invalidate();
        }

        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Log.d(TAG, "OnTapCommand()");
            switch (tapType) {
                case TAP_TYPE_TAP:

                    // If your background complication is the first item in your array, you need
                    // to walk backward through the array to make sure the tap isn't for a
                    // complication above the background complication.
                    for (int i = COMPLICATION_IDS.length - 1; i >= 0; i--) {
                        int complicationId = COMPLICATION_IDS[i];
                        ComplicationDrawable complicationDrawable =
                                mComplicationDrawableSparseArray.get(complicationId);

                        boolean successfulTap = complicationDrawable.onTap(x, y);

                        if (successfulTap) {
                            return;
                        }
                    }
                    break;
            }
        }

        private Handler mInvalidateHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                postInvalidate();
            }
        };
    }
}
