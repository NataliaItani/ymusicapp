package io.ymusic.app.fragments.songs;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.nativeAds.MaxNativeAdListener;
import com.applovin.mediation.nativeAds.MaxNativeAdLoader;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.File;
import java.io.FileFilter;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.ymusic.app.R;
import io.ymusic.app.ads.nativead.AppNativeAdView;
import io.ymusic.app.ads.nativead.NativeAdStyle;
import io.ymusic.app.base.BaseFragment;
import io.ymusic.app.util.AppUtils;
import io.ymusic.app.util.FileUtil;
import io.ymusic.app.util.NavigationHelper;

public class SongsFragment extends BaseFragment {

    @BindView(R.id.status_bar)
    View statusBarView;
    @BindView(R.id.template_view)
    AppNativeAdView nativeAdView;
    @BindView(R.id.native_ad_layout)
    FrameLayout nativeAdContainer;
    MaxAd nativeAd;

    @BindView(R.id.tabLayout)
    TabLayout tabLayout;
    @BindView(R.id.viewPager)
    ViewPager2 viewPager;

    private SongsPagerAdapter adapter;

//    @BindView(R.id.user_avatar)
//    CircleImageView userAvatar;
//    private GoogleSignInClient mGoogleSignInClient;
//    private FirebaseAuth mAuth;

    public enum SONG_TYPE {
        ARTIST, ALBUM, PLAYLIST, SEARCH
    }

    @NonNull
    public static SongsFragment getInstance() {
        return new SongsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        // Configure sign-in to request the user's ID, email address, and basic
//        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
//        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                .requestIdToken(getString(R.string.client_id))
//                .requestEmail()
//                .build();
//
//        // Build a GoogleSignInClient with the options specified by gso.
//        mGoogleSignInClient = GoogleSignIn.getClient(activity, gso);
//
//        // Initialize Firebase Auth
//        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
//        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(activity);
//        updateUI(account);
    }

//    public void updateUI(GoogleSignInAccount account) {
//        if (account != null) {
//            Glide.with(activity).load(account.getPhotoUrl())
//                    .diskCacheStrategy(DiskCacheStrategy.ALL)
//                    .placeholder(R.drawable.ic_account_circle_white_24dp)
//                    .error(R.drawable.ic_account_circle_white_24dp)
//                    .fallback(R.drawable.ic_account_circle_white_24dp)
//                    .into(userAvatar);
//
//            AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
//            mAuth.signInWithCredential(credential)
//                    .addOnCompleteListener(activity, task -> {
//                    });
//        } else {
//            Glide.with(activity).load(R.drawable.ic_account_circle_white_24dp)
//                    .diskCacheStrategy(DiskCacheStrategy.ALL)
//                    .placeholder(R.drawable.ic_account_circle_white_24dp)
//                    .error(R.drawable.ic_account_circle_white_24dp)
//                    .fallback(R.drawable.ic_account_circle_white_24dp)
//                    .into(userAvatar);
//        }
//    }
//
//    @OnClick(R.id.user_avatar)
//    void onUserAvatarClicked() {
//        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(activity);
//        if (account != null) {
//            AccountInfoDialog accountInfoDialog = AccountInfoDialog.getInstance(account, mGoogleSignInClient, () -> {
//                updateUI(null);
//                SharedPrefsHelper.removePrefs(activity, SharedPrefsHelper.Key.USER_LOGIN_EMAIL.name());
//            });
//            accountInfoDialog.show(getFM(), AccountInfoDialog.class.getSimpleName());
//        } else {
//            boolean showedLoginDialog = SharedPrefsHelper.getBooleanPrefs(activity, SharedPrefsHelper.Key.SHOWED_LOGIN_DIALOG.name());
//            if (showedLoginDialog) {
//                signIn();
//            } else {
//                DialogUtils.showLoginDialog(activity, (dialogInterface, i) -> signIn(), (dialogInterface, i) -> dialogInterface.dismiss());
//            }
//        }
//    }
//
//    private void signIn() {
//        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
//        mStartForResult.launch(signInIntent);
//    }
//
//    ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
//        if (result.getResultCode() == Activity.RESULT_OK) {
//            Intent data = result.getData();
//            // Handle the Intent
//            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
//            handleSignInResult(task);
//        }
//        if (result.getResultCode() == Activity.RESULT_CANCELED) {
//            Intent data = result.getData();
//            // Handle the Intent
//            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
//            handleSignInResult(task);
//        }
//    });
//
//    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
//        try {
//            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
//            // Save user's email logged in
//            SharedPrefsHelper.setStringPrefs(activity, SharedPrefsHelper.Key.USER_LOGIN_EMAIL.name(), account.getEmail());
//            SharedPrefsHelper.setBooleanPrefs(activity, SharedPrefsHelper.Key.SHOWED_LOGIN_DIALOG.name(), true);
//            // Signed in successfully, show authenticated UI.
//            updateUI(account);
//        } catch (ApiException e) {
//            // The ApiException status code indicates the detailed failure reason.
//            // Please refer to the GoogleSignInStatusCodes class reference for more information.
//            updateUI(null);
//        }
//    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_songs_main, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    protected void initViews(View rootView, Bundle savedInstanceState) {
        super.initViews(rootView, savedInstanceState);
        AppUtils.setStatusBarHeight(activity, statusBarView);
        initAdapter();

        // Show ad
        showNativeAd();
        showApplovinNativeAd();
    }

    private void initAdapter() {
        adapter = new SongsPagerAdapter(getChildFragmentManager(), getViewLifecycleOwner().getLifecycle());
        // set adapter to viewPager
        viewPager.setAdapter(adapter);
        // setup tabLayout with viewPager
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText(Arrays.asList(
                getString(R.string.songs), getString(R.string.artists),
                getString(R.string.albums), getString(R.string.playlists)
        ).get(position))).attach();
    }


    public static class ArrayListPathsAsyncTask extends AsyncTask<ArrayListPathsAsyncTask.LoadingInfo, String, String[]> {

        private final WeakReference<OnPathsListedCallback> onPathsListedCallbackWeakReference;

        public ArrayListPathsAsyncTask(OnPathsListedCallback callback) {
            super();
            onPathsListedCallbackWeakReference = new WeakReference<>(callback);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            checkCallbackReference();
        }

        @Override
        protected String[] doInBackground(LoadingInfo... params) {
            try {
                if (isCancelled() || checkCallbackReference() == null) return null;

                LoadingInfo info = params[0];

                final String[] paths;

                if (info.file.isDirectory()) {
                    List<File> files = FileUtil.listFilesDeep(info.file, info.fileFilter);

                    if (isCancelled() || checkCallbackReference() == null) return null;

                    paths = new String[files.size()];
                    for (int i = 0; i < files.size(); i++) {
                        File f = files.get(i);
                        paths[i] = FileUtil.safeGetCanonicalPath(f);

                        if (isCancelled() || checkCallbackReference() == null) return null;
                    }
                } else {
                    paths = new String[1];
                    paths[0] = FileUtil.safeGetCanonicalPath(info.file);
                }

                return paths;
            } catch (Exception e) {
                e.printStackTrace();
                cancel(false);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String[] paths) {
            super.onPostExecute(paths);
            OnPathsListedCallback callback = checkCallbackReference();
            if (callback != null && paths != null) {
                callback.onPathsListed(paths);
            }
        }

        private OnPathsListedCallback checkCallbackReference() {
            OnPathsListedCallback callback = onPathsListedCallbackWeakReference.get();
            if (callback == null) {
                cancel(false);
            }
            return callback;
        }

        public static class LoadingInfo {
            public final File file;
            public final FileFilter fileFilter;

            public LoadingInfo(File file, FileFilter fileFilter) {
                this.file = file;
                this.fileFilter = fileFilter;
            }
        }

        public interface OnPathsListedCallback {
            void onPathsListed(@NonNull String[] paths);
        }
    }

    @Override
    protected void initListeners() {
        super.initListeners();
    }

    private void showNativeAd() {

        // ad options
        VideoOptions videoOptions = new VideoOptions.Builder()
                .setStartMuted(true)
                .build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder()
                .setVideoOptions(videoOptions)
                .build();

        AdLoader adLoader = new AdLoader.Builder(activity, getString(R.string.native_ad))
                .forNativeAd(nativeAd -> {

                    // show the ad
                    NativeAdStyle styles = new NativeAdStyle.Builder().build();
                    nativeAdView.setStyles(styles);
                    nativeAdView.setNativeAd(nativeAd);
                })
                .withAdListener(new AdListener() {

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        // gone
                        nativeAdView.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        // visible
                        nativeAdView.setVisibility(View.VISIBLE);
                    }
                })
                .withNativeAdOptions(adOptions)
                .build();

        // loadAd
        AdRequest.Builder builder = new AdRequest.Builder();
        adLoader.loadAd(builder.build());
    }

    private void showApplovinNativeAd() {
        MaxNativeAdLoader nativeAdLoader = new MaxNativeAdLoader(getString(R.string.applovin_native_ad), requireContext());
        nativeAdLoader.setNativeAdListener(new MaxNativeAdListener() {
            @Override
            public void onNativeAdLoaded(final MaxNativeAdView nativeAdView, final MaxAd ad) {
                // Clean up any pre-existing native ad to prevent memory leaks.
                if (nativeAd != null) {
                    nativeAdLoader.destroy(nativeAd);
                }

                // Save ad for cleanup.
                nativeAd = ad;

                // Add ad view to view.
                nativeAdContainer.removeAllViews();
                nativeAdContainer.addView(nativeAdView);
                nativeAdContainer.setVisibility(View.VISIBLE);
            }

            @Override
            public void onNativeAdLoadFailed(final String adUnitId, final MaxError error) {
                // We recommend retrying with exponentially higher delays up to a maximum delay
                nativeAdContainer.setVisibility(View.GONE);
            }

            @Override
            public void onNativeAdClicked(final MaxAd ad) {
                // Optional click callback
            }
        });
        nativeAdLoader.loadAd();
    }

    @Override
    public void onDestroy() {
        if (nativeAdView != null) {
            nativeAdView.destroyNativeAd();
        }
        super.onDestroy();
    }

    @OnClick(R.id.action_search)
    void onSearch() {
        // open search
        NavigationHelper.openLocalSearchFragment(getFM());
    }

    @OnClick(R.id.action_settings)
    void onSettings() {
        // open Settings
        NavigationHelper.openSettings(activity);
    }
}
