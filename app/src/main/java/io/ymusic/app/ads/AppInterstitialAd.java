package io.ymusic.app.ads;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.ads.MaxInterstitialAd;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import io.ymusic.app.R;

public class AppInterstitialAd {

    private static AppInterstitialAd mInstance;
    private InterstitialAd mInterstitialAd;
    private MaxInterstitialAd maxInterstitialAd;
    private AdClosedListener mAdClosedListener;
    private boolean isReloaded = false;
    private int counterInterstitialAd = 0;
    private static final int CAP = 3;

    public interface AdClosedListener {
        void onAdClosed();
    }

    public static AppInterstitialAd getInstance() {
        if (mInstance == null) {
            mInstance = new AppInterstitialAd();
        }
        return mInstance;
    }

    public void init(Activity activity) {
        loadInterstitialAd(activity);
    }

    private void loadInterstitialAd(Activity activity) {
        // Admob Ad
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(activity, activity.getString(R.string.interstitial_ad), adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                // The mInterstitialAd reference will be null until an ad is loaded.
                mInterstitialAd = interstitialAd;
                interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        // Called when fullscreen content is dismissed.
                        // Make sure to set your reference to null so you don't show it a second time.
                        if (mAdClosedListener != null) {
                            mAdClosedListener.onAdClosed();
                        }
                        // load a new interstitial
                        loadInterstitialAd(activity);
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        // Called when fullscreen content failed to show.
                        // Make sure to set your reference to null so you don't show it a second time.
                        if (!isReloaded) {
                            isReloaded = true;
                            loadInterstitialAd(activity);
                        }
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        // Called when fullscreen content is shown.
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                // Handle the error
                mInterstitialAd = null;
            }
        });

        // Applovin Ad
        loadMaxInterstitialAd(activity);

    }

    private void loadMaxInterstitialAd(Activity activity) {
        maxInterstitialAd = new MaxInterstitialAd(activity.getString(R.string.applovin_interstitial_ad), activity);
        maxInterstitialAd.setListener(new MaxAdListener() {
            @Override
            public void onAdLoaded(MaxAd ad) {
                //maxInterstitialAd.showAd();
            }

            @Override
            public void onAdDisplayed(MaxAd ad) {
            }

            @Override
            public void onAdHidden(MaxAd ad) {
            }

            @Override
            public void onAdClicked(MaxAd ad) {
            }

            @Override
            public void onAdLoadFailed(String adUnitId, MaxError error) {
                // Interstitial ad failed to load
            }

            @Override
            public void onAdDisplayFailed(MaxAd ad, MaxError error) {
                // Interstitial ad failed to display.
                maxInterstitialAd = null;
            }
        });
        maxInterstitialAd.loadAd();
    }


    public void showInterstitialAd(Activity activity, AdClosedListener mAdClosedListener) {
        // increase counter by 1
        counterInterstitialAd = counterInterstitialAd + 1;
        // show ad if counter greater than 3
        if (counterInterstitialAd >= CAP) {
            if (mInterstitialAd != null) {
                isReloaded = false;
                this.mAdClosedListener = mAdClosedListener;
                // show ads
                mInterstitialAd.show(activity);
                // show Applovin add
                if (maxInterstitialAd.isReady()) {
                    maxInterstitialAd.showAd();
                }
            } else {
                // reload a new ad for next time
                loadInterstitialAd(activity);
                // call onAdClosed
                mAdClosedListener.onAdClosed();
            }
            counterInterstitialAd = 0;
        } else {
            // reload a new ad for next time
            loadInterstitialAd(activity);
            // call onAdClosed
            mAdClosedListener.onAdClosed();
        }
    }
}