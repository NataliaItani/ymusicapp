package io.ymusic.app.activities;

import static org.schabi.newpipe.extractor.NewPipe.getDownloader;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.annimon.stream.Stream;
import com.kabouzeid.appthemehelper.ATH;

import org.schabi.newpipe.extractor.NewPipe;

import java.util.Locale;

import io.ymusic.app.R;
import io.ymusic.app.ads.AppInterstitialAd;
import io.ymusic.app.util.AppUtils;
import io.ymusic.app.util.Constants;
import io.ymusic.app.util.Localization;
import io.ymusic.app.util.ThemeHelper;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeHelper.setTheme(this);
        ATH.setStatusbarColor(this, ContextCompat.getColor(this, R.color.youtube_primary_color));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);

        AppUtils.fetchShowAdsFromRemoteConfig(this);
        AppUtils.fetchDownloadVisibility(this);

        SharedPreferences defaultPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = defaultPreferences.edit();

        String countryCode = AppUtils.getDeviceCountryIso(this);
        String languageCode = Stream.of(Locale.getAvailableLocales()).filter(locale -> locale.getCountry().equals(AppUtils.getDeviceCountryIso(this))).map(Locale::getLanguage).findFirst().get();
        // save COUNTRY_CODE, LANGUAGE_CODE to preferences
        editor.putString(Constants.COUNTRY_CODE, !TextUtils.isEmpty(countryCode) ? countryCode : "GB");
        editor.putString(Constants.LANGUAGE_CODE, !TextUtils.isEmpty(languageCode) ? languageCode : "en");
        editor.apply();

        // init localization
        NewPipe.init(getDownloader(), Localization.getAppLocalization(this), Localization.getAppCountry(this));

        // init ad
        AppInterstitialAd.getInstance().init(this);


    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("AppOpenManager", "SplashScreen on start");
        // open MainActivity
        new Handler(getMainLooper()).postDelayed(this::openMainActivity, 1500);
    }

    private void openMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        // end here
        finish();
    }
}
