package io.ymusic.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.multidex.MultiDexApplication;
import androidx.preference.PreferenceManager;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.appset.AppSet;
import com.google.android.gms.appset.AppSetIdClient;
import com.google.android.gms.appset.AppSetIdInfo;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.nostra13.universalimageloader.cache.memory.impl.LRULimitedMemoryCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.downloader.Downloader;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;

import io.ymusic.app.activities.MainActivity;
import io.ymusic.app.activities.ReCaptchaActivity;
import io.ymusic.app.ads.AppOpenManager;
import io.ymusic.app.notification.NotificationOreo;
import io.ymusic.app.util.DownloaderImpl;
import io.ymusic.app.util.ExtractorHelper;
import io.ymusic.app.util.Localization;
import io.ymusic.app.util.SharedPrefsHelper;
import io.ymusic.app.util.StateSaver;
import io.reactivex.annotations.NonNull;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.exceptions.OnErrorNotImplementedException;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;

@SuppressLint("Registered")
public class App extends MultiDexApplication implements LifecycleObserver, Application.ActivityLifecycleCallbacks {

    public static Context applicationContext;

    public static Context getAppContext() {
        return applicationContext;
    }

    private int count = 0;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        applicationContext = this;

        // initialize localization
        NewPipe.init(getDownloader(),
                Localization.getAppLocalization(this),
                Localization.getAppCountry(this));
        StateSaver.init(this);

        // image loader
        ImageLoader.getInstance().init(getImageLoaderConfigurations());

        // initialize notification channels for android-o
        NotificationOreo.init(this);
        initNotificationChannel();

        // initialize firebase
        FirebaseApp.initializeApp(this);

        configureRxJavaErrorHandler();

        // AdMob
        MobileAds.initialize(this, initializationStatus -> {
            // Open app ad
            new AppOpenManager(this).showAdIfAvailable();
        });

        // Test device
        RequestConfiguration builder = new RequestConfiguration.Builder()
                .setTestDeviceIds(Collections.singletonList("EDD4DA7D9F7F13ACCA11E2BC4DF465B4"))
                .build();
        MobileAds.setRequestConfiguration(builder);

        registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        Context context = getApplicationContext();
        AppSetIdClient client = AppSet.getClient(context);
        com.google.android.gms.tasks.Task<AppSetIdInfo> task = client.getAppSetIdInfo();

        task.addOnSuccessListener(info -> {
            // Determine current scope of app set ID.
            int scope = info.getScope();

            // Read app set ID value, which uses version 4 of the
            // universally unique identifier (UUID) format.
            String id = info.getId();
        });

        // setup app theme
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
        if (preference.getBoolean(getString(R.string.dark_theme_key), false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

    }

    protected Downloader getDownloader() {
        DownloaderImpl downloader = DownloaderImpl.init(null);
        setCookiesToDownloader(downloader);
        return downloader;
    }

    protected void setCookiesToDownloader(final DownloaderImpl downloader) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final String key = getApplicationContext().getString(R.string.recaptcha_cookies_key);
        downloader.setCookie(ReCaptchaActivity.RECAPTCHA_COOKIES_KEY, prefs.getString(key, ""));
        downloader.updateYoutubeRestrictedModeCookies(getApplicationContext());
    }

    private void configureRxJavaErrorHandler() {

        // https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0#error-handling
        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {

            @Override
            public void accept(@NonNull Throwable throwable) {

                if (throwable instanceof UndeliverableException) {
                    // As UndeliverableException is a wrapper, get the cause of it to get the "real" exception
                    throwable = throwable.getCause();
                }

                final List<Throwable> errors;
                if (throwable instanceof CompositeException) {
                    errors = ((CompositeException) throwable).getExceptions();
                } else {
                    errors = Collections.singletonList(throwable);
                }

                for (final Throwable error : errors) {
                    if (isThrowableIgnored(error)) return;
                    if (isThrowableCritical(error)) {
                        reportException(error);
                        return;
                    }
                }
            }

            private boolean isThrowableIgnored(@NonNull final Throwable throwable) {

                // Don't crash the application over a simple network problem
                return ExtractorHelper.hasAssignableCauseThrowable(throwable, IOException.class, SocketException.class, // network api cancellation
                        InterruptedException.class, InterruptedIOException.class); // blocking code disposed
            }

            private boolean isThrowableCritical(@NonNull final Throwable throwable) {

                // Though these exceptions cannot be ignored
                return ExtractorHelper.hasAssignableCauseThrowable(throwable,
                        NullPointerException.class, IllegalArgumentException.class, // bug in app
                        OnErrorNotImplementedException.class, MissingBackpressureException.class,
                        IllegalStateException.class); // bug in operator
            }

            private void reportException(@NonNull final Throwable throwable) {

                // Throw uncaught exception that will trigger the report system
                Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), throwable);
            }
        });
    }

    private ImageLoaderConfiguration getImageLoaderConfigurations() {

        return new ImageLoaderConfiguration.Builder(this)
                .memoryCache(new LRULimitedMemoryCache(100 * 1024 * 1024))
                .diskCacheSize(500 * 1024 * 1024)
                .build();
    }

    public void initNotificationChannel() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        final String id = getString(R.string.notification_channel_id);
        final CharSequence name = getString(R.string.notification_channel_name);
        final String description = getString(R.string.notification_channel_description);

        // Keep this below DEFAULT to avoid making noise on every notification update
        final int importance = NotificationManager.IMPORTANCE_LOW;

        NotificationChannel mChannel = new NotificationChannel(id, name, importance);
        mChannel.setDescription(description);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (mNotificationManager != null) {
            mNotificationManager.createNotificationChannel(mChannel);
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {

    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {

    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        if (activity instanceof MainActivity) {

            // Save time open app
            count = SharedPrefsHelper.getIntPrefs(this, SharedPrefsHelper.Key.IN_APP_REVIEWS.name());
            SharedPrefsHelper.setIntPrefs(this, SharedPrefsHelper.Key.IN_APP_REVIEWS.name(), count + 1);

            /*// In-app updates
            AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(this);

            // Returns an intent object that you use to check for an update.
            Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

            // Checks that the platform will allow the specified type of update.
            appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        // This example applies an immediate update. To apply a flexible update
                        // instead, pass in AppUpdateType.FLEXIBLE
                        && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    // Request the update.
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                                // Pass the intent that is returned by 'getAppUpdateInfo()'.
                                appUpdateInfo,
                                // Or 'AppUpdateType.FLEXIBLE' for flexible updates.
                                AppUpdateType.IMMEDIATE,
                                // The current activity making the update request.
                                activity,
                                // Include a request code to later monitor this update request.
                                9999);
                    } catch (IntentSender.SendIntentException e) {
                        e.printStackTrace();
                    }
                }
            });*/

            // In-app reviews
            if (count == 3) {
                // Uncomment this line to use in production environment
                ReviewManager manager = ReviewManagerFactory.create(activity);

                // Uncomment this line to use in test environment
                // ReviewManager manager = new FakeReviewManager(activity);

                Task<ReviewInfo> request = manager.requestReviewFlow();
                request.addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // We can get the ReviewInfo object
                        ReviewInfo reviewInfo = task.getResult();
                        Task<Void> flow = manager.launchReviewFlow(activity, reviewInfo);
                        flow.addOnCompleteListener(_task -> {
                            // The flow has finished. The API does not indicate whether the user
                            // reviewed or not, or even whether the review dialog was shown. Thus, no
                            // matter the result, we continue our app flow.
                        });
                    } else {
                        SharedPrefsHelper.setIntPrefs(this, SharedPrefsHelper.Key.IN_APP_REVIEWS.name(), 0);
                    }
                });
            }
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

    }
 
    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {

    }
}
