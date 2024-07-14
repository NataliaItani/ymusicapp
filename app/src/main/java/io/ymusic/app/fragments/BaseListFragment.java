package io.ymusic.app.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

import io.ymusic.app.App;
import io.ymusic.app.R;
import io.ymusic.app.ads.AppInterstitialAd;
import io.ymusic.app.fragments.channel.ChannelFragment;
import io.ymusic.app.fragments.download.DownloadMusicService;
import io.ymusic.app.info_list.InfoListAdapter;
import io.ymusic.app.local_player.helper.MusicPlayerRemote;
import io.ymusic.app.player.helper.PlayerHolder;
import io.ymusic.app.player.playqueue.PlayQueue;
import io.ymusic.app.player.playqueue.SinglePlayQueue;
import io.ymusic.app.util.AnimationUtils;
import io.ymusic.app.util.AppUtils;
import io.ymusic.app.util.ExtractorHelper;
import io.ymusic.app.util.NavigationHelper;
import io.ymusic.app.util.OnClickGesture;
import io.ymusic.app.util.PermissionHelper;
import io.ymusic.app.util.StateSaver;
import io.ymusic.app.util.dialog.DialogUtils;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public abstract class BaseListFragment<I, N> extends BaseStateFragment<I> implements ListViewContract<I, N>, StateSaver.WriteRead {

    // Views
    protected InfoListAdapter infoListAdapter;
    protected RecyclerView itemsList;

//    private GoogleSignInClient mGoogleSignInClient;
//    private FullScreenLoadingDialog loadingIndicator;
//    private FirebaseAuth mAuth;

    // LifeCycle
    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        infoListAdapter = new InfoListAdapter(activity, this instanceof ChannelFragment);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

//        loadingIndicator = new FullScreenLoadingDialog(activity);

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
    public void onDestroy() {
        super.onDestroy();
        StateSaver.onDestroy(savedState);
    }

    // State Saving
    protected StateSaver.SavedState savedState;

    @Override
    public String generateSuffix() {
        // Naive solution, but it's good for now (the items don't change)
        return "." + infoListAdapter.getItemsList().size() + ".list";
    }

    @Override
    public void writeTo(Queue<Object> objectsToSave) {
        objectsToSave.add(infoListAdapter.getItemsList());
    }

    @Override
    public void readFrom(@NonNull Queue<Object> savedObjects) throws Exception {
        infoListAdapter.getItemsList().clear();
        infoListAdapter.getItemsList().addAll((List<InfoItem>) savedObjects.poll());
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle bundle) {
        super.onSaveInstanceState(bundle);
        savedState = StateSaver.tryToSave(activity.isChangingConfigurations(), savedState, bundle, this);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        savedState = StateSaver.tryToRestore(bundle, this);
    }

    // Init
    protected View getListHeader() {
        return null;
    }

    protected View getListFooter() {
        return activity.getLayoutInflater().inflate(R.layout.pignate_footer, itemsList, false);
    }

    protected RecyclerView.LayoutManager getListLayoutManager() {
        return new LinearLayoutManager(activity);
    }

    @Override
    protected void initViews(View rootView, Bundle savedInstanceState) {
        super.initViews(rootView, savedInstanceState);
        itemsList = rootView.findViewById(R.id.items_list);
        itemsList.setLayoutManager(getListLayoutManager());

        infoListAdapter.setFooter(getListFooter());
        infoListAdapter.setHeader(getListHeader());
        itemsList.setAdapter(infoListAdapter);
    }

    protected void onItemSelected(InfoItem selectedItem) {

    }

    @Override
    protected void initListeners() {
        super.initListeners();
        infoListAdapter.setOnStreamSelectedListener(new OnClickGesture<StreamInfoItem>() {

            @Override
            public void selected(StreamInfoItem selectedItem) {
                AppInterstitialAd.getInstance().showInterstitialAd(activity, () -> onStreamSelected(selectedItem));
            }

            @Override
            public void download(StreamInfoItem selectedItem) {
//                if (AppUtils.isLoggedIn(activity)) {
                    if (!PermissionHelper.checkStoragePermissions(getActivity(), PermissionHelper.DOWNLOADS_REQUEST_CODE)) {
                        return;
                    }

                    AppInterstitialAd.getInstance().showInterstitialAd(activity, () -> {
                        // download
                        onStreamDownload(selectedItem);
                    });
//                } else {
//                    boolean showedLoginDialog = SharedPrefsHelper.getBooleanPrefs(activity, SharedPrefsHelper.Key.SHOWED_LOGIN_DIALOG.name());
//                    if (showedLoginDialog) {
//                        signIn();
//                    } else {
//                        DialogUtils.showLoginDialog(activity, (dialogInterface, i) -> signIn(), (dialogInterface, i) -> dialogInterface.dismiss());
//                    }
//                }
            }
        });

        itemsList.clearOnScrollListeners();
        itemsList.addOnScrollListener(new OnScrollBelowItemsListener() {
            @Override
            public void onScrolledDown(RecyclerView recyclerView) {
                onScrollToBottom();
            }
        });
    }

//    private void showProgress() {
//        loadingIndicator.show();
//    }
//
//    private void hideProgress() {
//        loadingIndicator.cancel();
//    }

//    ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
//        if (result.getResultCode() == Activity.RESULT_OK) {
//            Intent data = result.getData();
//            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
//            handleSignInResult(task);
//        }
//
//        if (result.getResultCode() == Activity.RESULT_CANCELED) {
//            Intent data = result.getData();
//            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
//            handleSignInResult(task);
//        }
//    });
//
//    private void signIn() {
//        showProgress();
//        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
//        mStartForResult.launch(signInIntent);
//    }
//
//    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
//        try {
//            hideProgress();
//            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
//            Toast.makeText(activity, R.string.login_success, Toast.LENGTH_SHORT).show();
//            // Signed in successfully, show authenticated UI.
//            doSocialLogin(account);
//            ((MainActivity) activity).updateAccountInfo();
//        } catch (ApiException e) {
//            // The ApiException status code indicates the detailed failure reason.
//            // Please refer to the GoogleSignInStatusCodes class reference for more information.
//            Log.w("TAG", "signInResult:failed code=" + e.getStatusCode());
//            hideProgress();
//            hideLoading();
//        }
//    }
//
//    private void doSocialLogin(GoogleSignInAccount account) {
//        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
//        mAuth.signInWithCredential(credential)
//                .addOnCompleteListener(activity, task -> {
//                });
//
//        String email = account.getEmail();
//        // Save user's email logged in
//        SharedPrefsHelper.setStringPrefs(activity, SharedPrefsHelper.Key.USER_LOGIN_EMAIL.name(), email);
//        SharedPrefsHelper.setBooleanPrefs(activity, SharedPrefsHelper.Key.SHOWED_LOGIN_DIALOG.name(), true);
//    }

    @SuppressLint("CheckResult")
    private void onStreamDownload(StreamInfoItem selectedItem) {
        // create getting link dialog
        AlertDialog gettingLinkDialog = DialogUtils.gettingLinkDialog(activity);
        gettingLinkDialog.show();

        // extractor from video URL
        ExtractorHelper.getStreamInfo(AppUtils.getServiceId(selectedItem.getUrl()), selectedItem.getUrl(), true)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(streamInfo -> {
                    try {
                        // dismiss getting dialog
                        gettingLinkDialog.dismiss();
                        // download
                        if (streamInfo.getAudioStreams().size() > 0) {
                            download(streamInfo);
                        } else {
                            // show dialog URL not supported
                            DialogUtils.showDialogURLNotSupported(activity);
                        }
                    } catch (Exception e) {
                        // dismiss getting dialog
                        gettingLinkDialog.dismiss();
                        // show dialog URL not supported
                        DialogUtils.showDialogURLNotSupported(activity);
                    }
                }, throwable -> {
                    // dismiss getting dialog
                    gettingLinkDialog.dismiss();
                    // show dialog URL not supported
                    DialogUtils.showDialogURLNotSupported(activity);
                });
    }

    public void download(StreamInfo streamInfo) {
        Intent intent = new Intent(App.applicationContext, DownloadMusicService.class);
        intent.putExtra(DownloadMusicService.Extra.STREAM_INFO.name(), streamInfo);
        DownloadMusicService.enqueueWork(App.applicationContext, intent);
    }

    private void onStreamSelected(StreamInfoItem selectedItem) {
        // remove local player first
        MusicPlayerRemote.clearQueue();

        // start remote player
        onItemSelected(selectedItem);
        PlayQueue playQueue = getPlayQueue(infoListAdapter.getPosition(selectedItem));
        PlayerHolder.stopService(App.getAppContext());
        NavigationHelper.openVideoDetailFragment(getFM(), selectedItem.getServiceId(), selectedItem.getUrl(), selectedItem.getName(), true, playQueue);
    }

    protected void onScrollToBottom() {
        if (hasMoreItems() && !isLoading.get()) {
            loadMoreItems();
        }
    }

    private PlayQueue getPlayQueue(final int index) {
        if (infoListAdapter == null) {
            return new SinglePlayQueue(Collections.emptyList(), 0);
        }

        final List<InfoItem> infoItems = infoListAdapter.getItemsList();
        List<StreamInfoItem> streamInfoItems = new ArrayList<>(infoItems.size());
        for (final InfoItem item : infoItems) {
            if (item instanceof StreamInfoItem) {
                streamInfoItems.add((StreamInfoItem) item);
            }
        }
        return new SinglePlayQueue(streamInfoItems, index);
    }

    // Menu
    @Override
    public void onCreateOptionsMenu(@NotNull Menu menu, @NotNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        ActionBar supportActionBar = activity.getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayShowTitleEnabled(true);
            supportActionBar.setDisplayHomeAsUpEnabled(!useAsFrontPage);
        }
    }

    // Load and handle
    protected abstract void loadMoreItems();

    protected abstract boolean hasMoreItems();

    // Contract
    @Override
    public void showLoading() {
        super.showLoading();
    }

    @Override
    public void hideLoading() {
        super.hideLoading();
        AnimationUtils.animateView(itemsList, true, 200);
    }

    @Override
    public void showError(String message, boolean showRetryButton) {
        super.showError(message, showRetryButton);
        showListFooter(false);
        AnimationUtils.animateView(itemsList, false, 200);
    }

    @Override
    public void showEmptyState() {
        super.showEmptyState();
        showListFooter(false);
    }

    @Override
    public void showListFooter(final boolean show) {
        itemsList.post(() -> {
            if (infoListAdapter != null && itemsList != null) {
                infoListAdapter.showFooter(show);
            }
        });
    }

    @Override
    public void handleNextItems(N result) {
        isLoading.set(false);
    }
}
