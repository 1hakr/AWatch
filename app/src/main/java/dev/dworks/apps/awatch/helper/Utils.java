package dev.dworks.apps.awatch.helper;

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;

public class Utils {

    public static boolean isTelevision(Context context) {
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        return uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    }
}
