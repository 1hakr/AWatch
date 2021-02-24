package dev.dworks.apps.awatch.config;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.ProviderInfoRetriever;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.widget.WearableRecyclerView;

import java.util.ArrayList;
import java.util.concurrent.Executors;

import dev.dworks.apps.awatch.R;
import dev.dworks.apps.awatch.common.FormClockView;
import dev.dworks.apps.awatch.common.config.ConfigHelper;
import dev.dworks.apps.awatch.common.config.Themes;
import dev.dworks.apps.awatch.config.ConfigData.BackgroundComplicationConfigItem;
import dev.dworks.apps.awatch.config.ConfigData.ColorConfigItem;
import dev.dworks.apps.awatch.config.ConfigData.ConfigItemType;
import dev.dworks.apps.awatch.config.ConfigData.MoreOptionsConfigItem;
import dev.dworks.apps.awatch.config.ConfigData.PreviewAndComplicationsConfigItem;
import dev.dworks.apps.awatch.config.ConfigData.SimpleConfigItem;
import dev.dworks.apps.awatch.helper.ColorActivity;
import dev.dworks.apps.awatch.watchface.FormWatchFace;

import static dev.dworks.apps.awatch.common.config.ConfigHelper.KEY_SHOW_NOTIFICATION_COUNT;

public class ConfigAdapter extends WearableRecyclerView.Adapter {

    private static final String TAG = "ConfigAdapter";

    public static final int TYPE_PREVIEW_AND_COMPLICATIONS_CONFIG = 0;
    public static final int TYPE_MORE_OPTIONS = 1;
    public static final int TYPE_COLOR_CONFIG = 2;
    public static final int TYPE_UNREAD_NOTIFICATION_CONFIG = 3;
    public static final int TYPE_BACKGROUND_COMPLICATION_IMAGE_CONFIG = 4;
    public static final int TYPE_SHOW_DATE_TIME_CONFIG = 5;
    public static final int TYPE_SHOW_SECONDS_CONFIG = 6;

    public enum ComplicationLocation {
        BACKGROUND,
        LEFT,
        RIGHT,
        TOP,
        BOTTOM
    }

    private PreviewAndComplicationsViewHolder mPreviewAndComplicationsViewHolder;
    private final Context mContext;
    private final ProviderInfoRetriever mProviderInfoRetriever;
    private final SharedPreferences mSharedPref;
    private int mSelectedComplicationId;
    private final int mBackgroundComplicationId;
    private final int mTopComplicationId;
    private final int mBottomComplicationId;
    private final ComponentName mWatchFaceComponentName;
    private ArrayList<ConfigItemType> mSettingsDataSet;

    public ConfigAdapter(Context context){
        mContext = context;
        mWatchFaceComponentName = new ComponentName(mContext, ConfigData.getWatchFaceServiceClass());
        mSettingsDataSet = ConfigData.getDataToPopulateAdapter(mContext);

        // Default value is invalid (only changed when user taps to change complication).
        mSelectedComplicationId = -1;

        mBackgroundComplicationId =
                FormWatchFace.getComplicationId(
                        ComplicationLocation.BACKGROUND);

        mTopComplicationId =
                FormWatchFace.getComplicationId(ComplicationLocation.TOP);
        mBottomComplicationId =
                FormWatchFace.getComplicationId(ComplicationLocation.BOTTOM);

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        // Initialization of code to retrieve active complication data for the watch face.
        mProviderInfoRetriever =
                new ProviderInfoRetriever(mContext, Executors.newCachedThreadPool());
        mProviderInfoRetriever.init();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder = null;

        switch (viewType) {
            case TYPE_PREVIEW_AND_COMPLICATIONS_CONFIG:
                // Need direct reference to watch face preview view holder to update watch face
                // preview based on selections from the user.
                mPreviewAndComplicationsViewHolder =
                        new PreviewAndComplicationsViewHolder(
                                LayoutInflater.from(parent.getContext())
                                        .inflate(
                                                R.layout.config_list_preview_and_complications_item,
                                                parent,
                                                false));
                viewHolder = mPreviewAndComplicationsViewHolder;
                break;

            case TYPE_MORE_OPTIONS:
                viewHolder =
                        new MoreOptionsViewHolder(
                                LayoutInflater.from(parent.getContext())
                                        .inflate(
                                                R.layout.config_list_more_options_item,
                                                parent,
                                                false));
                break;

            case TYPE_COLOR_CONFIG:
                viewHolder =
                        new ColorPickerViewHolder(
                                LayoutInflater.from(parent.getContext())
                                        .inflate(R.layout.config_list_color_item, parent, false));
                break;

            case TYPE_UNREAD_NOTIFICATION_CONFIG:
            case TYPE_SHOW_DATE_TIME_CONFIG:
            case TYPE_SHOW_SECONDS_CONFIG:
                viewHolder =
                        new SimpleConfigViewHolder(
                                LayoutInflater.from(parent.getContext())
                                        .inflate(
                                                R.layout.config_list_unread_notif_item,
                                                parent,
                                                false));
                break;

            case TYPE_BACKGROUND_COMPLICATION_IMAGE_CONFIG:
                viewHolder =
                        new BackgroundComplicationViewHolder(
                                LayoutInflater.from(parent.getContext())
                                        .inflate(
                                                R.layout.config_list_background_complication_item,
                                                parent,
                                                false));
                break;
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {

        ConfigData.ConfigItemType configItemType = mSettingsDataSet.get(position);

        switch (viewHolder.getItemViewType()) {
            case TYPE_PREVIEW_AND_COMPLICATIONS_CONFIG:
                PreviewAndComplicationsViewHolder previewAndComplicationsViewHolder =
                        (PreviewAndComplicationsViewHolder) viewHolder;

                PreviewAndComplicationsConfigItem previewAndComplicationsConfigItem =
                        (PreviewAndComplicationsConfigItem) configItemType;

                int defaultComplicationResourceId =
                        previewAndComplicationsConfigItem.getDefaultComplicationResourceId();
                previewAndComplicationsViewHolder.setDefaultComplicationDrawable(
                        defaultComplicationResourceId);

                previewAndComplicationsViewHolder.initializesColorsAndComplications();
                break;

            case TYPE_MORE_OPTIONS:
                MoreOptionsViewHolder moreOptionsViewHolder = (MoreOptionsViewHolder) viewHolder;
                MoreOptionsConfigItem moreOptionsConfigItem =
                        (MoreOptionsConfigItem) configItemType;

                moreOptionsViewHolder.setIcon(moreOptionsConfigItem.getIconResourceId());
                break;

            case TYPE_COLOR_CONFIG:
                ColorPickerViewHolder colorPickerViewHolder = (ColorPickerViewHolder) viewHolder;
                ColorConfigItem colorConfigItem = (ColorConfigItem) configItemType;

                int iconResourceId = colorConfigItem.getIconResourceId();
                String name = colorConfigItem.getName();
                String sharedPrefString = colorConfigItem.getSharedPrefString();
                Class<ColorActivity> activity =
                        colorConfigItem.getActivityToChoosePreference();

                colorPickerViewHolder.setIcon(iconResourceId);
                colorPickerViewHolder.setName(name);
                colorPickerViewHolder.setSharedPrefString(sharedPrefString);
                colorPickerViewHolder.setLaunchActivityToSelectColor(activity);
                break;

            case TYPE_UNREAD_NOTIFICATION_CONFIG:
                SimpleConfigViewHolder unreadViewHolder =
                        (SimpleConfigViewHolder) viewHolder;

                SimpleConfigItem unreadConfigItem =
                        (SimpleConfigItem) configItemType;

                int unreadEnabledIconResourceId = unreadConfigItem.getIconEnabledResourceId();
                int unreadDisabledIconResourceId = unreadConfigItem.getIconDisabledResourceId();

                String unreadName = unreadConfigItem.getName();
                String unreadSharedPrefId = unreadConfigItem.getSharedPrefKey();

                unreadViewHolder.setIcons(
                        unreadEnabledIconResourceId, unreadDisabledIconResourceId);
                unreadViewHolder.setName(unreadName);
                unreadViewHolder.setSharedPrefId(unreadSharedPrefId);
                break;

            case TYPE_SHOW_DATE_TIME_CONFIG:
            case TYPE_SHOW_SECONDS_CONFIG:
                SimpleConfigViewHolder showTimeViewHolder =
                        (SimpleConfigViewHolder) viewHolder;

                SimpleConfigItem simpleConfigItem =
                        (SimpleConfigItem) configItemType;

                int configResourceId = simpleConfigItem.getIconEnabledResourceId();

                String configName = simpleConfigItem.getName();
                String sharedPrefId = simpleConfigItem.getSharedPrefKey();

                showTimeViewHolder.setIcon(configResourceId);
                showTimeViewHolder.setName(configName);
                showTimeViewHolder.setSharedPrefId(sharedPrefId);
                break;

            case TYPE_BACKGROUND_COMPLICATION_IMAGE_CONFIG:
                BackgroundComplicationViewHolder backgroundComplicationViewHolder =
                        (BackgroundComplicationViewHolder) viewHolder;

                BackgroundComplicationConfigItem backgroundComplicationConfigItem =
                        (BackgroundComplicationConfigItem) configItemType;

                int backgroundIconResourceId = backgroundComplicationConfigItem.getIconResourceId();
                String backgroundName = backgroundComplicationConfigItem.getName();

                backgroundComplicationViewHolder.setIcon(backgroundIconResourceId);
                backgroundComplicationViewHolder.setName(backgroundName);
                break;
        }
    }

    @Override
    public int getItemViewType(int position) {
        ConfigItemType configItemType = mSettingsDataSet.get(position);
        return configItemType.getConfigType();
    }

    @Override
    public int getItemCount() {
        return mSettingsDataSet.size();
    }

    /** Updates the selected complication id saved earlier with the new information. */
    public void updateSelectedComplication(ComplicationProviderInfo complicationProviderInfo) {

        Log.d(TAG, "updateSelectedComplication: " + mPreviewAndComplicationsViewHolder);

        // Checks if view is inflated and complication id is valid.
        if (mPreviewAndComplicationsViewHolder != null && mSelectedComplicationId >= 0) {
            mPreviewAndComplicationsViewHolder.updateComplicationViews(
                    mSelectedComplicationId, complicationProviderInfo);
        }
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        // Required to release retriever for active complication data on detach.
        mProviderInfoRetriever.release();
    }

    public void updatePreviewColors() {
        Log.d(TAG, "updatePreviewColors(): " + mPreviewAndComplicationsViewHolder);

        if (mPreviewAndComplicationsViewHolder != null) {
            mPreviewAndComplicationsViewHolder.updateWatchFaceColors();
        }
    }

    /**
     * Displays watch face preview along with complication locations. Allows user to tap on the
     * complication they want to change and preview updates dynamically.
     */
    public class PreviewAndComplicationsViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        private ImageView mTopComplicationBackground;
        private ImageView mBottomComplicationBackground;

        private ImageButton mTopComplication;
        private ImageButton mBottomComplication;

        private Drawable mDefaultComplicationDrawable;

        private final ImageView mMainImageView;
        private final FormClockView mMainClockView;
        private Themes.Theme theme;

        public PreviewAndComplicationsViewHolder(final View view) {
            super(view);

            // Sets up Top complication preview.
            mTopComplicationBackground =
                    (ImageView) view.findViewById(R.id.top_complication_background);
            mTopComplication = (ImageButton) view.findViewById(R.id.top_complication);
            mTopComplication.setOnClickListener(this);

            // Sets up Bottom complication preview.
            mBottomComplicationBackground =
                    (ImageView) view.findViewById(R.id.bottom_complication_background);
            mBottomComplication = (ImageButton) view.findViewById(R.id.bottom_complication);
            mBottomComplication.setOnClickListener(this);

            mMainImageView =  view.findViewById(R.id.watch_face_background);
            mMainClockView = (FormClockView) view.findViewById(R.id.clock);
        }

        @Override
        public void onClick(View view) {
            if (view.equals(mTopComplication)) {
                Activity currentActivity = (Activity) view.getContext();
                launchComplicationHelperActivity(currentActivity, ComplicationLocation.TOP);

            } else if (view.equals(mBottomComplication)) {
                Activity currentActivity = (Activity) view.getContext();
                launchComplicationHelperActivity(currentActivity, ComplicationLocation.BOTTOM);
            }
        }

        public void updateWatchFaceColors() {
            updateWatchFace();
        }

        // Verifies the watch face supports the complication location, then launches the helper
        // class, so user can choose their complication data provider.
        private void launchComplicationHelperActivity(
                Activity currentActivity, ComplicationLocation complicationLocation) {

            mSelectedComplicationId =
                    FormWatchFace.getComplicationId(complicationLocation);

            if (mSelectedComplicationId >= 0) {

                int[] supportedTypes =
                        FormWatchFace.getSupportedComplicationTypes(
                                complicationLocation);

                ComponentName watchFace =
                        new ComponentName(
                                currentActivity, FormWatchFace.class);

                currentActivity.startActivityForResult(
                        ComplicationHelperActivity.createProviderChooserHelperIntent(
                                currentActivity,
                                watchFace,
                                mSelectedComplicationId,
                                supportedTypes),
                        ConfigActivity.COMPLICATION_CONFIG_REQUEST_CODE);

            } else {
                Log.d(TAG, "Complication not supported by watch face.");
            }
        }

        public void setDefaultComplicationDrawable(int resourceId) {
            Context context = mMainImageView.getContext();
            mDefaultComplicationDrawable = ContextCompat.getDrawable(context, resourceId);

            mTopComplication.setImageDrawable(mDefaultComplicationDrawable);
            mTopComplicationBackground.setVisibility(View.INVISIBLE);

            mBottomComplication.setImageDrawable(mDefaultComplicationDrawable);
            mBottomComplicationBackground.setVisibility(View.INVISIBLE);
        }

        public void updateComplicationViews(
                int watchFaceComplicationId, ComplicationProviderInfo complicationProviderInfo) {
            Log.d(TAG, "updateComplicationViews(): id: " + watchFaceComplicationId);
            Log.d(TAG, "\tinfo: " + complicationProviderInfo);

            if (watchFaceComplicationId == mTopComplicationId) {
                updateComplicationView(complicationProviderInfo, mTopComplication,
                        mTopComplicationBackground);

            } else if (watchFaceComplicationId == mBottomComplicationId) {
                updateComplicationView(complicationProviderInfo, mBottomComplication,
                        mBottomComplicationBackground);
            }
        }

        private void updateComplicationView(ComplicationProviderInfo complicationProviderInfo,
                                            ImageButton button, ImageView background) {
            if (complicationProviderInfo != null) {
                button.setImageIcon(complicationProviderInfo.providerIcon);
                button.setContentDescription(
                        mContext.getString(R.string.edit_complication,
                                complicationProviderInfo.appName + " " +
                                        complicationProviderInfo.providerName));
                button.setVisibility(View.VISIBLE);
                background.setVisibility(View.INVISIBLE);
            } else {
                button.setImageDrawable(mDefaultComplicationDrawable);
                button.setContentDescription(mContext.getString(R.string.add_complication));
                background.setVisibility(View.INVISIBLE);
            }
        }

        public void initializesColorsAndComplications() {
            updateWatchFace();

            final int[] complicationIds = FormWatchFace.getComplicationIds();

            mProviderInfoRetriever.retrieveProviderInfo(
                    new ProviderInfoRetriever.OnProviderInfoReceivedCallback() {
                        @Override
                        public void onProviderInfoReceived(
                                int watchFaceComplicationId,
                                @Nullable ComplicationProviderInfo complicationProviderInfo) {

                            Log.d(TAG, "onProviderInfoReceived: " + complicationProviderInfo);

                            updateComplicationViews(
                                    watchFaceComplicationId, complicationProviderInfo);
                        }
                    },
                    mWatchFaceComponentName,
                    complicationIds);
        }

        private void updateWatchFace(){
            String themeId = mSharedPref.getString(ConfigHelper.KEY_THEME, Themes.DEFAULT_THEME.id);
            theme = Themes.getThemeById(themeId);

            mMainClockView.setColors(
                    ContextCompat.getColor(mContext, theme.lightRes),
                    ContextCompat.getColor(mContext,theme.midRes),
                    Color.WHITE);

            int currentBackgroundColor = ContextCompat.getColor(mContext, theme.defaultRes);

            PorterDuffColorFilter backgroundColorFilter =
                    new PorterDuffColorFilter(currentBackgroundColor, PorterDuff.Mode.SRC_ATOP);
            mMainImageView
                    .getBackground()
                    .setColorFilter(backgroundColorFilter);
        }
    }

    /** Displays icon to indicate there are more options below the fold. */
    public class MoreOptionsViewHolder extends RecyclerView.ViewHolder {

        private ImageView mMoreOptionsImageView;

        public MoreOptionsViewHolder(View view) {
            super(view);
            mMoreOptionsImageView = (ImageView) view.findViewById(R.id.more_options_image_view);
        }

        public void setIcon(int resourceId) {
            Context context = mMoreOptionsImageView.getContext();
            mMoreOptionsImageView.setImageDrawable(ContextCompat.getDrawable(context, resourceId));
        }
    }

    /**
     * Displays color options for the an item on the watch face. These could include marker color,
     * background color, etc.
     */
    public class ColorPickerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private Button mAppearanceButton;

        private String mSharedPrefResourceString;

        private Class<ColorActivity> mLaunchActivityToSelectColor;

        public ColorPickerViewHolder(View view) {
            super(view);

            mAppearanceButton = (Button) view.findViewById(R.id.color_picker_button);
            view.setOnClickListener(this);
        }

        public void setName(String name) {
            mAppearanceButton.setText(name);
        }

        public void setIcon(int resourceId) {
            Context context = mAppearanceButton.getContext();
            mAppearanceButton.setCompoundDrawablesWithIntrinsicBounds(
                    ContextCompat.getDrawable(context, resourceId), null, null, null);
        }

        public void setSharedPrefString(String sharedPrefString) {
            mSharedPrefResourceString = sharedPrefString;
        }

        public void setLaunchActivityToSelectColor(Class<ColorActivity> activity) {
            mLaunchActivityToSelectColor = activity;
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();
            Log.d(TAG, "Complication onClick() position: " + position);

            if (mLaunchActivityToSelectColor != null) {
                Intent launchIntent = new Intent(view.getContext(), mLaunchActivityToSelectColor);

                Activity activity = (Activity) view.getContext();
                activity.startActivityForResult(
                        launchIntent,
                        ConfigActivity.UPDATE_COLORS_CONFIG_REQUEST_CODE);
            }
        }
    }

    /** Displays button to trigger background image complication selector. */
    public class BackgroundComplicationViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        private Button mBackgroundComplicationButton;

        public BackgroundComplicationViewHolder(View view) {
            super(view);

            mBackgroundComplicationButton =
                    (Button) view.findViewById(R.id.background_complication_button);
            view.setOnClickListener(this);
        }

        public void setName(String name) {
            mBackgroundComplicationButton.setText(name);
        }

        public void setIcon(int resourceId) {
            Context context = mBackgroundComplicationButton.getContext();
            mBackgroundComplicationButton.setCompoundDrawablesWithIntrinsicBounds(
                    ContextCompat.getDrawable(context, resourceId), null, null, null);
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();
            Log.d(TAG, "Background Complication onClick() position: " + position);

            Activity currentActivity = (Activity) view.getContext();

            mSelectedComplicationId =
                    FormWatchFace.getComplicationId(
                            ComplicationLocation.BACKGROUND);

            if (mSelectedComplicationId >= 0) {

                int[] supportedTypes =
                        FormWatchFace.getSupportedComplicationTypes(
                                ComplicationLocation.BACKGROUND);

                ComponentName watchFace =
                        new ComponentName(
                                currentActivity, FormWatchFace.class);

                currentActivity.startActivityForResult(
                        ComplicationHelperActivity.createProviderChooserHelperIntent(
                                currentActivity,
                                watchFace,
                                mSelectedComplicationId,
                                supportedTypes),
                        ConfigActivity.COMPLICATION_CONFIG_REQUEST_CODE);

            } else {
                Log.d(TAG, "Complication not supported by watch face.");
            }
        }
    }

    /**
     * Displays switch to indicate whether or not icon appears for unread notifications. User can
     * toggle on/off.
     */
    public class SimpleConfigViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        private SwitchCompat mSimpleSwitch;
        private int mEnabledIconResourceId;
        private int mDisabledIconResourceId;
        private String mSharedPrefKey;

        public SimpleConfigViewHolder(View view) {
            super(view);
            mSimpleSwitch = view.findViewById(R.id.unread_notification_switch);
            view.setOnClickListener(this);
        }

        public void setName(String name) {
            mSimpleSwitch.setText(name);
        }

        public void setIcon(int enabledIconResourceId) {

            mEnabledIconResourceId = enabledIconResourceId;
            Context context = mSimpleSwitch.getContext();

            // Set default to enabled.
            mSimpleSwitch.setCompoundDrawablesWithIntrinsicBounds(
                    ContextCompat.getDrawable(context, mEnabledIconResourceId), null, null, null);
        }

        public void setIcons(int enabledIconResourceId, int disabledIconResourceId) {

            mEnabledIconResourceId = enabledIconResourceId;
            mDisabledIconResourceId = disabledIconResourceId;

            Context context = mSimpleSwitch.getContext();

            // Set default to enabled.
            mSimpleSwitch.setCompoundDrawablesWithIntrinsicBounds(
                    ContextCompat.getDrawable(context, mEnabledIconResourceId), null, null, null);
        }

        public void setSharedPrefId(String sharedPreKey) {
            mSharedPrefKey = sharedPreKey;

            if (mSimpleSwitch != null) {
                boolean defaultValue = mSharedPrefKey.equals(KEY_SHOW_NOTIFICATION_COUNT);
                Context context = mSimpleSwitch.getContext();
                Boolean currentState = mSharedPref.getBoolean(mSharedPrefKey, defaultValue);

                updateIcon(context, currentState);
            }
        }

        private void updateIcon(Context context, Boolean currentState) {
            mSimpleSwitch.setChecked(currentState);
            if(0 == mDisabledIconResourceId){
                return;
            }

            int currentIconResourceId;
            if (currentState) {
                currentIconResourceId = mEnabledIconResourceId;
            } else {
                currentIconResourceId = mDisabledIconResourceId;
            }
            mSimpleSwitch.setCompoundDrawablesWithIntrinsicBounds(
                    ContextCompat.getDrawable(context, currentIconResourceId), null, null, null);
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();
            Log.d(TAG, "Complication onClick() position: " + position);

            Context context = view.getContext();

            // Since user clicked on a switch, new state should be opposite of current state.
            Boolean newState = !mSharedPref.getBoolean(mSharedPrefKey, true);

            SharedPreferences.Editor editor = mSharedPref.edit();
            editor.putBoolean(mSharedPrefKey, newState);
            editor.apply();

            updateIcon(context, newState);
        }
    }
}
