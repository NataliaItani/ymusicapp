package io.ymusic.app.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.annimon.stream.Optional;
import com.annimon.stream.function.Consumer;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import org.schabi.newpipe.extractor.ServiceList;

import java.util.Locale;

import io.ymusic.app.BuildConfig;
import io.ymusic.app.R;
import io.ymusic.app.updates.Version;

public class AppUtils {

    public static boolean isOnline(@NonNull Context context) {
        // true if online
        return Optional.ofNullable(((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)))
                .map(ConnectivityManager::getActiveNetworkInfo)
                .map(NetworkInfo::isConnected)
                .orElse(false);
    }

    public static String getDeviceCountryIso(Context context) {
        // get device country by sim card (most accurate)
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String deviceCountry = null;
        if (tm != null) {
            deviceCountry = tm.getSimCountryIso().toUpperCase();
        }

        // if no deviceCountry by sim card, try locale
        if (TextUtils.isEmpty(deviceCountry)) {
            deviceCountry = Locale.getDefault().getCountry();
        }

        return deviceCountry;
    }

    public static int dpToPx(Context context, float dip) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, context.getResources().getDisplayMetrics());
    }

    public static int getStatusBarHeight(Context context) {
        int statusBarHeight = dpToPx(context, 24);
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
        }
        return statusBarHeight;
    }

    public static void setStatusBarHeight(Context context, View statusBarView) {
        ViewGroup.LayoutParams layoutParams = statusBarView.getLayoutParams();
        layoutParams.height = AppUtils.getStatusBarHeight(context);
        statusBarView.setLayoutParams(layoutParams);
    }

    public static int getActionBarSize(@NonNull Context context) {
        TypedValue typedValue = new TypedValue();
        int[] textSizeAttr = new int[]{R.attr.actionBarSize};
        int indexOfAttrTextSize = 0;
        TypedArray a = context.obtainStyledAttributes(typedValue.data, textSizeAttr);
        int actionBarSize = a.getDimensionPixelSize(indexOfAttrTextSize, -1);
        a.recycle();
        return actionBarSize;
    }

    public static Point getScreenSize(@NonNull Context c) {
        Display display = ((WindowManager) c.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    public static void hideSoftKeyboard(@Nullable Activity activity) {
        if (activity != null) {
            View currentFocus = activity.getCurrentFocus();
            if (currentFocus != null) {
                InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
            }
        }
    }

    public static void fetchShowAdsFromRemoteConfig(Activity activity) {
        // create FirebaseRemoteConfig
        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();

        // init firebase remote config
        remoteConfig.setConfigSettingsAsync(new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0)
                .build());
        // fetch data from FirebaseRemoteConfig
        remoteConfig.fetchAndActivate().addOnSuccessListener(activity, success -> {
            boolean showAd = remoteConfig.getBoolean("show_ads");
            SharedPrefsHelper.setBooleanPrefs(activity, SharedPrefsHelper.Key.DISPLAY_ADS.name(), showAd);
        });
    }

    public static void fetchDownloadVisibility(Activity activity) {
        // create FirebaseRemoteConfig
        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();

        // init firebase remote config
        remoteConfig.setConfigSettingsAsync(new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0)
                .build());
        // fetch data from FirebaseRemoteConfig
        remoteConfig.fetchAndActivate().addOnSuccessListener(activity, success -> {
            boolean showDownload = remoteConfig.getBoolean("show_download");
            SharedPrefsHelper.setBooleanPrefs(activity, SharedPrefsHelper.Key.SHOW_DOWNLOAD.name(), showDownload);
        });
    }

    public static boolean isShowDownload(Context context) {
        return SharedPrefsHelper.getBooleanPrefs(context, SharedPrefsHelper.Key.SHOW_DOWNLOAD.name());
    }

    public static boolean isShowAds(Context context) {
        return SharedPrefsHelper.getBooleanPrefs(context, SharedPrefsHelper.Key.DISPLAY_ADS.name());
    }

    public static int getServiceId(String url) {
        return url.contains("youtube.com") ? ServiceList.YouTube.getServiceId() : ServiceList.SoundCloud.getServiceId();
    }

    public static void checkAppUpdates(Version version, Consumer<Version> updateCallback, Runnable cancelCallback) {
        // get version android from server
        String latestVersion = version.getVersion();

        // get current installed version
        String installVersion = BuildConfig.VERSION_NAME;

        // split version
        String[] splitLatestVersion = latestVersion.split("\\.");
        String[] splitInstallVersion = installVersion.split("\\.");

        // true if not same major.minor.patch
        boolean needUpdate = false;
        for (int index = 0; index < 3; index++) {

            int latest = Integer.parseInt(splitLatestVersion[index]);
            int install = Integer.parseInt(splitInstallVersion[index]);

            // if latest version is smaller than installed version at component index
            if (latest < install) {
                // no need to compare further lower components as installed version is higher
                break;
            }
            // else if latest version is bigger than installed version at component index
            else if (latest > install) {
                // need update
                needUpdate = true;
                // no need to compare further lower components as installed version is lower
                break;
            }
            // else same at component index, keep comparing lower components till hit difference
        }

        // if needUpdate true
        if (needUpdate) {
            if (updateCallback != null) {
                updateCallback.accept(version);
            }
        } else {
            if (cancelCallback != null) {
                cancelCallback.run();
            }
        }
    }

    public static boolean isHasNewVersion(Version version) {
        // get version android from server
        String latestVersion = version.getVersion();

        // get current installed version
        String installVersion = BuildConfig.VERSION_NAME;

        // split version
        String[] splitLatestVersion = latestVersion.split("\\.");
        String[] splitInstallVersion = installVersion.split("\\.");

        // true if not same major.minor.patch
        boolean needUpdate = false;
        for (int index = 0; index < 3; index++) {

            int latest = Integer.parseInt(splitLatestVersion[index]);
            int install = Integer.parseInt(splitInstallVersion[index]);

            // if latest version is smaller than installed version at component index
            if (latest < install) {
                // no need to compare further lower components as installed version is higher
                break;
            }
            // else if latest version is bigger than installed version at component index
            else if (latest > install) {
                // need update
                needUpdate = true;
                // no need to compare further lower components as installed version is lower
                break;
            }
            // else same at component index, keep comparing lower components till hit difference
        }

        // if needUpdate true
        return needUpdate;
    }

    public static boolean isLoggedIn(Context context) {
        return !TextUtils.isEmpty(SharedPrefsHelper.getStringPrefs(context, SharedPrefsHelper.Key.USER_LOGIN_EMAIL.name()));
    }
}