package io.ymusic.app.activities;

import static io.ymusic.app.fragments.detail.VideoDetailFragment.MAX_OVERLAY_ALPHA;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import com.annimon.stream.Stream;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdViewAdListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.ads.MaxAdView;
import com.applovin.sdk.AppLovinSdk;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.kabouzeid.appthemehelper.ATH;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.schabi.newpipe.extractor.StreamingService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.ymusic.app.App;
import io.ymusic.app.R;
import io.ymusic.app.ads.AppInterstitialAd;
import io.ymusic.app.fragments.BackPressable;
import io.ymusic.app.fragments.detail.VideoDetailFragment;
import io.ymusic.app.fragments.library.LibraryFragment;
import io.ymusic.app.fragments.search.SearchFragmentMain;
import io.ymusic.app.fragments.songs.SongsFragment;
import io.ymusic.app.fragments.trending.TrendingFragment;
import io.ymusic.app.local_player.base.AbsSlidingMusicPanelActivity;
import io.ymusic.app.local_player.helper.MusicPlayerRemote;
import io.ymusic.app.local_player.loader.SongLoader;
import io.ymusic.app.local_player.misc.WrappedAsyncTaskLoader;
import io.ymusic.app.local_player.model.Song;
import io.ymusic.app.player.VideoPlayer;
import io.ymusic.app.player.event.OnKeyDownListener;
import io.ymusic.app.player.helper.PlayerHolder;
import io.ymusic.app.player.playqueue.PlayQueue;
import io.ymusic.app.util.AppUtils;
import io.ymusic.app.util.Constants;
import io.ymusic.app.util.NavigationHelper;
import io.ymusic.app.util.SerializedCache;
import io.ymusic.app.util.StateSaver;
import io.ymusic.app.util.ThemeHelper;
import io.ymusic.app.util.rating.RateDialogManager;

public class MainActivity extends AbsSlidingMusicPanelActivity implements LoaderManager.LoaderCallbacks<List<Song>> {

    @BindView(R.id.bottom_navigation)
    BottomNavigationView mBottomNavigation;
    @BindView(R.id.adView)
    AdView adView;
    @BindView(R.id.maxAdView)
    MaxAdView maxAdView;

    private BroadcastReceiver broadcastReceiver;
    private List<Song> songs = new ArrayList<>();

    // Activity's LifeCycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.setTheme(this);
        ATH.setStatusbarColor(this, ContextCompat.getColor(this, R.color.app_background_color));
        super.onCreate(savedInstanceState);
        ButterKnife.bind(this);

        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            initFragments();
        }
        // setup bottom navigation
        setUpBottomNavigation();

        hideSystemUi();
        openMiniPlayerUponPlayerStarted();

        LoaderManager.getInstance(this).initLoader(0, null, this);

        // init ad
        AppInterstitialAd.getInstance().init(this);
        // show banner ad
        showBannerAd();

        // Applovin Ads
        AppLovinSdk.getInstance(this).setMediationProvider("max");
        AppLovinSdk.initializeSdk(this, configuration -> {
            // Start loading ads
            loadBannerAd();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("AppOpenManager", "Main on start");
    }

    @SuppressLint("NonConstantResourceId")
    private void setUpBottomNavigation() {
        mBottomNavigation.setSelectedItemId(R.id.navigation_search);
        mBottomNavigation.setOnNavigationItemSelectedListener(item -> {
            final Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
            switch (item.getItemId()) {
                case R.id.navigation_trending:
                    if (!(fragment instanceof TrendingFragment)) {
                        NavigationHelper.openTrendingFragment(getSupportFragmentManager());
                    }
                    collapsePanel();
                    return true;

                case R.id.navigation_search:
                    if (!(fragment instanceof SearchFragmentMain)) {
                        NavigationHelper.openMainFragment(getSupportFragmentManager());
                    }
                    collapsePanel();
                    return true;

                case R.id.navigation_songs:
                    if (!(fragment instanceof SongsFragment)) {
                        NavigationHelper.openSongsFragment(this, getSupportFragmentManager());
                    }
                    collapsePanel();
                    return true;

                case R.id.navigation_subs:
                    if (!(fragment instanceof LibraryFragment)) {
                        NavigationHelper.openLibraryFragment(getSupportFragmentManager());
                    }
                    collapsePanel();
                    return true;
            }
            return false;
        });
    }

    private void hideSystemUi() {
        int visibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getWindow().getDecorView().setSystemUiVisibility(visibility);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!isChangingConfigurations()) {
            StateSaver.clearStateFiles();
        }
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
        }
        maxAdView.stopAutoRefresh();
        maxAdView.destroy();
        maxAdView = null;
    }

    @Override
    protected View createContentView() {
        @SuppressLint("InflateParams")
        View contentView = getLayoutInflater().inflate(R.layout.activity_main, null);
        ViewGroup contentContainer = contentView.findViewById(R.id.fragment_content_container);
        contentContainer.addView(wrapSlidingMusicPanel(R.layout.activity_main_content));
        return contentView;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent != null) {
            // Return if launched from a launcher (e.g. Nova Launcher, Pixel Launcher ...)
            // to not destroy the already created backstack
            final String action = intent.getAction();
            if ((action != null && action.equals(Intent.ACTION_MAIN)) && intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
                return;
            }
        }
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        final Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_player_holder);
        if (fragment instanceof OnKeyDownListener && !bottomSheetHiddenOrCollapsed()) {
            // Provide keyDown event to fragment which then sends this event
            // to the main player service
            return ((OnKeyDownListener) fragment).onKeyDown(keyCode) || super.onKeyDown(keyCode, event);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
            collapsePanel();
            return;
        }
        // In case bottomSheet is not visible on the screen or collapsed we can assume that the user
        // interacts with a fragment inside fragment_holder so all back presses should be
        // handled by it
        if (bottomSheetHiddenOrCollapsed()) {
            final Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
            // If current fragment implements BackPressable (i.e. can/wanna handle back press)
            // delegate the back press to it
            if (fragment instanceof BackPressable) {
                if (((BackPressable) fragment).onBackPressed()) {
                    return;
                }
            } else {
                RateDialogManager.showRateDialog(this, null);
                return;
            }
        } else {
            final Fragment fragmentPlayer = getSupportFragmentManager().findFragmentById(R.id.fragment_player_holder);
            // If current fragment implements BackPressable (i.e. can/wanna handle back press)
            // delegate the back press to it
            if (fragmentPlayer instanceof BackPressable) {
                if (!((BackPressable) fragmentPlayer).onBackPressed()) {
                    final FrameLayout bottomSheetLayout = findViewById(R.id.fragment_player_holder);
                    BottomSheetBehavior.from(bottomSheetLayout).setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
                return;
            }
        }

        // if has only fragment in activity
        if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
            RateDialogManager.showRateDialog(this, null);
        } else {
            if (isFragmentInsideBottomNavigationBar()) {
                RateDialogManager.showRateDialog(this, null);
            } else {
                super.onBackPressed();
            }
        }
    }

    // Init fragments
    private void initFragments() {
        StateSaver.clearStateFiles();
        if (getIntent() != null && getIntent().hasExtra(Constants.KEY_LINK_TYPE)) {
            // When user watch a video inside popup and then tries to open the video in main player
            // while the app is closed he will see a blank fragment on place of kiosk.
            // Let's open it first
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                NavigationHelper.openMainFragment(getSupportFragmentManager());
                mBottomNavigation.setSelectedItemId(R.id.navigation_search);
            }
            handleIntent(getIntent());
        } else {
            NavigationHelper.openMainFragment(getSupportFragmentManager());
            mBottomNavigation.setSelectedItemId(R.id.navigation_search);
        }
    }

    private void handleIntent(Intent intent) {
        try {
            if (intent.getData() != null) {
                // stop remote player
                final FrameLayout bottomSheetLayout = findViewById(R.id.fragment_player_holder);
                final BottomSheetBehavior<FrameLayout> bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                setBottomNavigationVisibility(View.VISIBLE);
                // start local player
                String fileName = new File(intent.getData().getPath()).getName();
                String songName = fileName.lastIndexOf(".") > 0 ? fileName.substring(0, fileName.lastIndexOf(".")) : fileName;
                MusicPlayerRemote.openQueue(songs, getSongPosition(songName), true);
            } else if (intent.hasExtra(Constants.KEY_LINK_TYPE)) {
                final String url = intent.getStringExtra(Constants.KEY_URL);
                final int serviceId = intent.getIntExtra(Constants.KEY_SERVICE_ID, 0);
                final String title = intent.getStringExtra(Constants.KEY_TITLE);
                StreamingService.LinkType linkType = ((StreamingService.LinkType) intent.getSerializableExtra(Constants.KEY_LINK_TYPE));
                if (linkType == StreamingService.LinkType.STREAM) {
                    final boolean autoPlay = true;
                    final String intentCacheKey = intent.getStringExtra(VideoPlayer.PLAY_QUEUE_KEY);
                    final PlayQueue playQueue = intentCacheKey != null ? SerializedCache.getInstance().take(intentCacheKey, PlayQueue.class) : null;
                    PlayerHolder.stopService(App.getAppContext());
                    NavigationHelper.openVideoDetailFragment(getSupportFragmentManager(), serviceId, url, title, autoPlay, playQueue);
                }
            } else if (intent.hasExtra(Constants.KEY_OPEN_SEARCH)) {
                String searchString = intent.getStringExtra(Constants.KEY_SEARCH_STRING);
                if (searchString == null) {
                    searchString = "";
                }
                final int serviceId = intent.getIntExtra(Constants.KEY_SERVICE_ID, 0);
                NavigationHelper.openSearchFragment(getSupportFragmentManager(), serviceId, searchString);
            } else {
                NavigationHelper.openMainFragment(getSupportFragmentManager());
                mBottomNavigation.setSelectedItemId(R.id.navigation_search);
            }
        } catch (final Exception e) {
//            ErrorActivity.reportUiError(this, e);
        }
    }

    private boolean bottomSheetHiddenOrCollapsed() {
        final FrameLayout bottomSheetLayout = findViewById(R.id.fragment_player_holder);
        final BottomSheetBehavior<FrameLayout> bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);

        final int sheetState = bottomSheetBehavior.getState();
        return sheetState == BottomSheetBehavior.STATE_HIDDEN || sheetState == BottomSheetBehavior.STATE_COLLAPSED;
    }

    public void setBottomNavigationVisibility(int visibility) {
        mBottomNavigation.setVisibility(visibility);
        int padding = visibility == View.VISIBLE ? AppUtils.dpToPx(this, 56) : 0;
        getFragmentContentContainer().setPadding(0, 0, 0, padding);
    }

    public void setBottomNavigationAlpha(final float slideOffset) {
        mBottomNavigation.setAlpha(Math.min(MAX_OVERLAY_ALPHA, slideOffset));
    }

    private void openMiniPlayerUponPlayerStarted() {
        if (getIntent().getSerializableExtra(Constants.KEY_LINK_TYPE) == StreamingService.LinkType.STREAM) {
            // handleIntent() already takes care of opening video detail fragment
            // due to an intent containing a STREAM link
            return;
        }

        if (PlayerHolder.isPlayerOpen()) {
            // if the player is already open, no need for a broadcast receiver
            openMiniPlayerIfMissing();
        } else {
            // listen for player start intent being sent around
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(final Context context, final Intent intent) {
                    if (Objects.equals(intent.getAction(), VideoDetailFragment.ACTION_PLAYER_STARTED)) {
                        openMiniPlayerIfMissing();
                        // At this point the player is added 100%, we can unregister. Other actions
                        // are useless since the fragment will not be removed after that.
                        unregisterReceiver(broadcastReceiver);
                        broadcastReceiver = null;
                    }
                }
            };
            final IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(VideoDetailFragment.ACTION_PLAYER_STARTED);
            registerReceiver(broadcastReceiver, intentFilter);
        }
    }

    private void openMiniPlayerIfMissing() {
        final Fragment fragmentPlayer = getSupportFragmentManager().findFragmentById(R.id.fragment_player_holder);
        if (fragmentPlayer == null) {
            // We still don't have a fragment attached to the activity. It can happen when a user
            // started popup or background players without opening a stream inside the fragment.
            // Adding it in a collapsed state (only mini player will be visible).
            NavigationHelper.showMiniPlayer(getSupportFragmentManager());
        }
    }

    private void loadBannerAd() {
        maxAdView.setListener(new MaxAdViewAdListener() {
            @Override
            public void onAdExpanded(MaxAd ad) { }

            @Override
            public void onAdCollapsed(MaxAd ad) {}

            @Override
            public void onAdLoaded(MaxAd ad) {
                maxAdView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdDisplayed(MaxAd ad) {
                /* DO NOT USE - THIS IS RESERVED FOR FULLSCREEN ADS ONLY AND WILL BE REMOVED IN A FUTURE SDK RELEASE */
            }

            @Override
            public void onAdHidden(MaxAd ad) {
                /* DO NOT USE - THIS IS RESERVED FOR FULLSCREEN ADS ONLY AND WILL BE REMOVED IN A FUTURE SDK RELEASE */
            }

            @Override
            public void onAdClicked(MaxAd ad) { }

            @Override
            public void onAdLoadFailed(String adUnitId, MaxError error) { }

            @Override
            public void onAdDisplayFailed(MaxAd ad, MaxError error) { }
        });
        maxAdView.loadAd();
        maxAdView.startAutoRefresh();
    }

    private void showBannerAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.setAdListener(new AdListener() {

            @Override
            public void onAdLoaded() {
                // Code to be executed when an ad finishes loading.
                adView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                // Code to be executed when an ad request fails.
                adView.setVisibility(View.GONE);
            }
        });
        adView.loadAd(adRequest);
    }

    private ViewGroup getFragmentContentContainer() {
        View contentView = getLayoutInflater().inflate(R.layout.activity_main, null);
        return contentView.findViewById(R.id.fragment_content_container);
    }

    private boolean isFragmentInsideBottomNavigationBar() {
        final Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
        return (fragment instanceof TrendingFragment)
                || (fragment instanceof SearchFragmentMain)
                || (fragment instanceof SongsFragment)
                || (fragment instanceof LibraryFragment);
    }

    public int getSongPosition(String songName) {
        return Stream.range(0, songs.size())
                .filter(position -> songs.get(position).data.contains(songName))
                .findFirstOrElse(-1);
    }

    @NonNull
    @Override
    public Loader<List<Song>> onCreateLoader(int id, @Nullable Bundle args) {
        return new AsyncSongLoader(this);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<List<Song>> loader, List<Song> data) {
        songs = data;
    }

    @Override
    public void onLoaderReset(@NonNull Loader<List<Song>> loader) {
        songs = new ArrayList<>();
    }

    private static class AsyncSongLoader extends WrappedAsyncTaskLoader<List<Song>> {

        public AsyncSongLoader(Context context) {
            super(context);
        }

        @Override
        public List<Song> loadInBackground() {
            return SongLoader.getAllSongs(getContext());
        }
    }

//    public void updateAccountInfo() {
//        final Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
//        if (fragment instanceof TrendingFragment) {
//            ((TrendingFragment) fragment).updateUI(GoogleSignIn.getLastSignedInAccount(this));
//        } else if (fragment instanceof SearchFragmentMain) {
//            ((SearchFragmentMain) fragment).updateUI(GoogleSignIn.getLastSignedInAccount(this));
//        } else if (fragment instanceof SongsFragment) {
//            ((SongsFragment) fragment).updateUI(GoogleSignIn.getLastSignedInAccount(this));
//        } else if (fragment instanceof LibraryFragment) {
//            ((LibraryFragment) fragment).updateUI(GoogleSignIn.getLastSignedInAccount(this));
//        }
//    }
}
