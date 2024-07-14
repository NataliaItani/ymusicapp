package io.ymusic.app.updates;

import android.content.Context;
import android.content.SharedPreferences;

import io.ymusic.app.util.AppUtils;

public class UpdateSPManager {

    private static final String UPDATE_TIME_KEY = "update_time_key";
    private static final int DAYS_DELAY = (24 * 60 * 60 * 1000); // 1 days

    private static SharedPreferences getSP(Context c) {
        return c.getSharedPreferences("update_preferences", Context.MODE_PRIVATE);
    }

    public static void updateTime(Context c) {
        SharedPreferences sp = getSP(c);
        sp.edit().putLong(UPDATE_TIME_KEY, System.currentTimeMillis() + DAYS_DELAY).apply();
    }

    private static boolean isTimeValid(Context c) {
        SharedPreferences sp = getSP(c);
        long time = sp.getLong(UPDATE_TIME_KEY, 0);
        return time < System.currentTimeMillis();
    }

    public static boolean canShowDialog(Context c, Version version) {
        return AppUtils.isHasNewVersion(version) /*&& (isTimeValid(c))*/;
    }
}
