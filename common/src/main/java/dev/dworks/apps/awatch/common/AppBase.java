package dev.dworks.apps.awatch.common;

import android.app.Application;

import static dev.dworks.apps.awatch.common.Utils.isTelevision;

public class AppBase extends Application {

    public boolean isTelevision;
    private static AppBase sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        isTelevision = isTelevision(getApplicationContext());
        if(!BuildConfig.DEBUG) {
            AnalyticsHelper.intialize(getApplicationContext());
            CrashHelper.enable(getApplicationContext(), true);
        }
    }

    public static synchronized AppBase getInstance() {
        return sInstance;
    }

    @Override
    public void onLowMemory() {
        Runtime.getRuntime().gc();
        super.onLowMemory();
    }
}
