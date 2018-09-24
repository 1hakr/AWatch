package dev.dworks.apps.awatch.config;

import android.content.Context;
import android.graphics.Color;

import java.util.ArrayList;

import dev.dworks.apps.awatch.watchface.FormWatchFace;
import dev.dworks.apps.awatch.R;
import dev.dworks.apps.awatch.helper.ColorActivity;
import dev.dworks.apps.awatch.common.config.ConfigHelper;

/**
 * Data represents different views for configuring the
 * {@link FormWatchFace} watch face's appearance and complications
 * via {@link ConfigActivity}.
 */
public class ConfigData {

    public interface ConfigItemType {
        int getConfigType();
    }

    /**
     * Returns Watch Face Service class associated with configuration Activity.
     */
    public static Class getWatchFaceServiceClass() {
        return FormWatchFace.class;
    }

    /**
     * Returns Material Design color options.
     */
    public static ArrayList<Integer> getColorOptionsDataSet() {
        ArrayList<Integer> colorOptionsDataSet = new ArrayList<>();
        colorOptionsDataSet.add(Color.parseColor("#FFFFFF")); // White

        colorOptionsDataSet.add(Color.parseColor("#FFEB3B")); // Yellow
        colorOptionsDataSet.add(Color.parseColor("#FFC107")); // Amber
        colorOptionsDataSet.add(Color.parseColor("#FF9800")); // Orange
        colorOptionsDataSet.add(Color.parseColor("#FF5722")); // Deep Orange

        colorOptionsDataSet.add(Color.parseColor("#F44336")); // Red
        colorOptionsDataSet.add(Color.parseColor("#E91E63")); // Pink

        colorOptionsDataSet.add(Color.parseColor("#9C27B0")); // Purple
        colorOptionsDataSet.add(Color.parseColor("#673AB7")); // Deep Purple
        colorOptionsDataSet.add(Color.parseColor("#3F51B5")); // Indigo
        colorOptionsDataSet.add(Color.parseColor("#2196F3")); // Blue
        colorOptionsDataSet.add(Color.parseColor("#03A9F4")); // Light Blue

        colorOptionsDataSet.add(Color.parseColor("#00BCD4")); // Cyan
        colorOptionsDataSet.add(Color.parseColor("#009688")); // Teal
        colorOptionsDataSet.add(Color.parseColor("#4CAF50")); // Green
        colorOptionsDataSet.add(Color.parseColor("#8BC34A")); // Lime Green
        colorOptionsDataSet.add(Color.parseColor("#CDDC39")); // Lime

        colorOptionsDataSet.add(Color.parseColor("#607D8B")); // Blue Grey
        colorOptionsDataSet.add(Color.parseColor("#9E9E9E")); // Grey
        colorOptionsDataSet.add(Color.parseColor("#795548")); // Brown
        colorOptionsDataSet.add(Color.parseColor("#000000")); // Black

        return colorOptionsDataSet;
    }

    public static ArrayList<ConfigItemType> getDataToPopulateAdapter(Context context) {

        ArrayList<ConfigItemType> settingsConfigData = new ArrayList<>();

        // Data for watch face preview and complications UX in settings Activity.
        ConfigItemType complicationConfigItem =
                new PreviewAndComplicationsConfigItem(R.drawable.ic_add_complication);
        settingsConfigData.add(complicationConfigItem);

        // Data for "more options" UX in settings Activity.
        ConfigItemType moreOptionsConfigItem =
                new MoreOptionsConfigItem(R.drawable.ic_expand_more);
        settingsConfigData.add(moreOptionsConfigItem);

        // Data for Background color UX in settings Activity.
        ConfigItemType backgroundColorConfigItem =
                new ColorConfigItem(
                        context.getString(R.string.config_background_color_label),
                        R.drawable.ic_style,
                        ConfigHelper.KEY_THEME,
                        ColorActivity.class);
        settingsConfigData.add(backgroundColorConfigItem);

        // Data for 'Unread Notifications' UX (toggle) in settings Activity.
        ConfigItemType unreadNotificationsConfigItem =
                new SimpleConfigItem(
                        context.getString(R.string.config_unread_notifications_label),
                        R.drawable.ic_notifications,
                        R.drawable.ic_notifications_off,
                        ConfigHelper.KEY_SHOW_NOTIFICATION_COUNT,
                        ConfigAdapter.TYPE_UNREAD_NOTIFICATION_CONFIG);
        settingsConfigData.add(unreadNotificationsConfigItem);

        ConfigItemType showTimeConfigItem =
                new SimpleConfigItem(
                        context.getString(R.string.config_show_date_label),
                        R.drawable.ic_today,
                        ConfigHelper.KEY_SHOW_DATE,
                        ConfigAdapter.TYPE_SHOW_DATE_TIME_CONFIG);
        settingsConfigData.add(showTimeConfigItem);

        ConfigItemType showSecondsConfigItem =
                new SimpleConfigItem(
                        context.getString(R.string.config_show_seconds_label),
                        R.drawable.ic_seconds,
                        ConfigHelper.KEY_SHOW_SECONDS,
                        ConfigAdapter.TYPE_SHOW_SECONDS_CONFIG);
        settingsConfigData.add(showSecondsConfigItem);

        // Data for background complications UX in settings Activity.
        ConfigItemType backgroundImageComplicationConfigItem =
                // TODO (jewalker): Revised in another CL to support background complication.
                new BackgroundComplicationConfigItem(
                        context.getString(R.string.config_background_image_complication_label),
                        R.drawable.ic_landscape);
        //settingsConfigData.add(backgroundImageComplicationConfigItem);

        return settingsConfigData;
    }

    /**
     * Data for Watch Face Preview with Complications Preview item in RecyclerView.
     */
    public static class PreviewAndComplicationsConfigItem implements ConfigItemType {

        private int defaultComplicationResourceId;

        PreviewAndComplicationsConfigItem(int defaultComplicationResourceId) {
            this.defaultComplicationResourceId = defaultComplicationResourceId;
        }

        public int getDefaultComplicationResourceId() {
            return defaultComplicationResourceId;
        }

        @Override
        public int getConfigType() {
            return ConfigAdapter.TYPE_PREVIEW_AND_COMPLICATIONS_CONFIG;
        }
    }

    /**
     * Data for "more options" item in RecyclerView.
     */
    public static class MoreOptionsConfigItem implements ConfigItemType {

        private int iconResourceId;

        MoreOptionsConfigItem(int iconResourceId) {
            this.iconResourceId = iconResourceId;
        }

        public int getIconResourceId() {
            return iconResourceId;
        }

        @Override
        public int getConfigType() {
            return ConfigAdapter.TYPE_MORE_OPTIONS;
        }
    }

    /**
     * Data for color picker item in RecyclerView.
     */
    public static class ColorConfigItem  implements ConfigItemType {

        private String name;
        private int iconResourceId;
        private String sharedPrefString;
        private Class<ColorActivity> activityToChoosePreference;

        ColorConfigItem(
                String name,
                int iconResourceId,
                String sharedPrefString,
                Class<ColorActivity> activity) {
            this.name = name;
            this.iconResourceId = iconResourceId;
            this.sharedPrefString = sharedPrefString;
            this.activityToChoosePreference = activity;
        }

        public String getName() {
            return name;
        }

        public int getIconResourceId() {
            return iconResourceId;
        }

        public String getSharedPrefString() {
            return sharedPrefString;
        }

        public Class<ColorActivity> getActivityToChoosePreference() {
            return activityToChoosePreference;
        }

        @Override
        public int getConfigType() {
            return ConfigAdapter.TYPE_COLOR_CONFIG;
        }
    }

    /**
     * Data for background image complication picker item in RecyclerView.
     */
    public static class BackgroundComplicationConfigItem  implements ConfigItemType {

        private String name;
        private int iconResourceId;

        BackgroundComplicationConfigItem(
                String name,
                int iconResourceId) {

            this.name = name;
            this.iconResourceId = iconResourceId;
        }

        public String getName() {
            return name;
        }

        public int getIconResourceId() {
            return iconResourceId;
        }

        @Override
        public int getConfigType() {
            return ConfigAdapter.TYPE_BACKGROUND_COMPLICATION_IMAGE_CONFIG;
        }
    }

    /**
     * Data for Unread Notification preference picker item in RecyclerView.
     */
    public static class SimpleConfigItem  implements ConfigItemType {

        private String name;
        private int iconEnabledResourceId;
        private int iconDisabledResourceId;
        private int configType;
        private String sharedPrefKey;

        SimpleConfigItem(
                String name,
                int iconEnabledResourceId,
                String sharedPrefKey,
                int configType) {
            this.name = name;
            this.iconEnabledResourceId = iconEnabledResourceId;
            this.sharedPrefKey = sharedPrefKey;
            this.configType = configType;
        }

        SimpleConfigItem(
                String name,
                int iconEnabledResourceId,
                int iconDisabledResourceId,
                String sharedPrefKey,
                int configType) {
            this.name = name;
            this.iconEnabledResourceId = iconEnabledResourceId;
            this.iconDisabledResourceId = iconDisabledResourceId;
            this.sharedPrefKey = sharedPrefKey;
            this.configType = configType;
        }

        public String getName() {
            return name;
        }

        public int getIconEnabledResourceId() {
            return iconEnabledResourceId;
        }

        public int getIconDisabledResourceId() {
            return iconDisabledResourceId;
        }

        public String getSharedPrefKey() {
            return sharedPrefKey;
        }

        @Override
        public int getConfigType() {
            return configType;
        }
    }
}