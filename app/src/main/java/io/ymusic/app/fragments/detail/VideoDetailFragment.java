package io.ymusic.app.fragments.detail;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.card.MaterialCardView;

import org.jetbrains.annotations.NotNull;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.stream.StreamInfo;

import java.util.Objects;

import butterknife.ButterKnife;
import icepick.State;
import io.ymusic.app.App;
import io.ymusic.app.BuildConfig;
import io.ymusic.app.R;
import io.ymusic.app.fragments.BackPressable;
import io.ymusic.app.fragments.BaseStateFragment;
import io.ymusic.app.player.MainPlayer;
import io.ymusic.app.player.VideoPlayerImpl;
import io.ymusic.app.player.event.OnKeyDownListener;
import io.ymusic.app.player.event.PlayerServiceExtendedEventListener;
import io.ymusic.app.player.helper.PlayerHolder;
import io.ymusic.app.player.playqueue.PlayQueue;
import io.ymusic.app.player.playqueue.SinglePlayQueue;
import io.ymusic.app.util.ExtractorHelper;
import io.ymusic.app.util.GlideUtils;
import io.ymusic.app.util.NavigationHelper;
import io.ymusic.app.util.ThemeHelper;
import io.ymusic.app.util.dialog.DialogUtils;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class VideoDetailFragment extends BaseStateFragment<StreamInfo> implements BackPressable,
        View.OnClickListener,
        PlayerServiceExtendedEventListener,
        OnKeyDownListener {

    public static final float MAX_OVERLAY_ALPHA = 1.0f;

    public static final String PACKAGE_NAME = BuildConfig.APPLICATION_ID;
    public static final String ACTION_SHOW_MAIN_PLAYER = PACKAGE_NAME + ".VideoDetailFragment.ACTION_SHOW_MAIN_PLAYER";
    public static final String ACTION_HIDE_MAIN_PLAYER = PACKAGE_NAME + ".VideoDetailFragment.ACTION_HIDE_MAIN_PLAYER";
    public static final String ACTION_MINIMIZE_MAIN_PLAYER = PACKAGE_NAME + ".VideoDetailFragment.ACTION_MINIMIZE_MAIN_PLAYER";
    public static final String ACTION_PLAYER_STARTED = PACKAGE_NAME + ".VideoDetailFragment.ACTION_PLAYER_STARTED";
    public static final String ACTION_VIDEO_FRAGMENT_RESUMED = PACKAGE_NAME + ".VideoDetailFragment.ACTION_VIDEO_FRAGMENT_RESUMED";
    public static final String ACTION_VIDEO_FRAGMENT_STOPPED = PACKAGE_NAME + ".VideoDetailFragment.ACTION_VIDEO_FRAGMENT_STOPPED";

    @State
    protected int serviceId = ServiceList.SoundCloud.getServiceId();
    @State
    protected String name;
    @State
    protected String url;
    protected static PlayQueue playQueue;
    @State
    int bottomSheetState = BottomSheetBehavior.STATE_EXPANDED;
    @State
    protected boolean autoPlayEnabled = true;

    private StreamInfo currentInfo;
    private Disposable currentWorker;
    @NonNull
    private final CompositeDisposable disposables = new CompositeDisposable();

    private BottomSheetBehavior<FrameLayout> bottomSheetBehavior;
    private BottomSheetBehavior.BottomSheetCallback bottomSheetCallback;
    private BroadcastReceiver broadcastReceiver;

    private MainPlayer playerService;
    private VideoPlayerImpl player;

    // Views
    private ViewGroup playerPlaceholder;
    //private View preview;

    private TextView titleTextView;
    private TextView artistTextView;
    private ImageView thumbnailImageView;
    private ImageView backgroundImageView;
    private ImageButton playPauseButton;
    private ImageButton minimizeButton;
    private ImageButton closeButton;

    // overlay views
    private MaterialCardView overlay;
    private LinearLayout overlayMetadata;
    private ImageView overlayThumbnailImageView;
    private TextView overlayTitleTextView;
    private TextView overlayChannelTextView;
    private LinearLayout overlayButtons;
    private ImageButton overlayPlayPauseButton;
    private ImageButton overlayCloseButton;
    private MaterialProgressBar miniProgressBar;

    @Override
    public void onServiceConnected(VideoPlayerImpl connectedPlayer, MainPlayer connectedPlayerService, boolean playAfterConnect) {
        player = connectedPlayer;
        playerService = connectedPlayerService;

        if (!player.videoPlayerSelected() && !playAfterConnect) {
            return;
        }

        if (playerIsNotStopped() && player.videoPlayerSelected()) {
            addVideoPlayerView();
        }

        if (playAfterConnect || (currentInfo != null && isAutoplayEnabled() && player.getParentActivity() == null)) {
            openMainPlayer();
        }
    }

    @Override
    public void onServiceDisconnected() {
        playerService = null;
        player = null;
    }

    public static VideoDetailFragment getInstance(int serviceId, String videoUrl, String name, final PlayQueue queue) {
        VideoDetailFragment instance = new VideoDetailFragment();
        instance.setInitialData(serviceId, videoUrl, name, queue);
        return instance;
    }

    public static VideoDetailFragment getInstanceInCollapsedState() {
        final VideoDetailFragment instance = new VideoDetailFragment();
        instance.bottomSheetState = BottomSheetBehavior.STATE_COLLAPSED;
        instance.setBottomNavigationViewVisibility(View.VISIBLE);
        return instance;
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
    }

    // Fragment's Lifecycle
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setupBroadcastReceiver();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video_detail2, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (currentWorker != null) currentWorker.dispose();
    }

    @Override
    public void onResume() {
        super.onResume();
        activity.sendBroadcast(new Intent(ACTION_VIDEO_FRAGMENT_RESUMED));
        // Check if it was loading when the fragment was stopped/paused
        if (wasLoading.getAndSet(false) && !wasCleared()) {
            startLoading(false);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        activity.sendBroadcast(new Intent(ACTION_VIDEO_FRAGMENT_STOPPED));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the service when user leaves the app with double back press
        // if video player is selected. Otherwise unbind
        if (activity.isFinishing() && player != null && player.videoPlayerSelected()) {
            //PlayerHolder.stopService(App.applicationContext);
            NavigationHelper.playOnBackgroundPlayer(activity, null, true);
        } else {
            PlayerHolder.removeListener();
        }

        try {
            activity.unregisterReceiver(broadcastReceiver);
        } catch (Exception ignored) {}

        if (currentWorker != null) {
            currentWorker.dispose();
        }
        disposables.clear();
        currentWorker = null;
        bottomSheetBehavior.removeBottomSheetCallback(bottomSheetCallback);

        if (activity.isFinishing()) {
            playQueue = null;
            currentInfo = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    // OnClick
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.overlay_thumbnail:
            case R.id.overlay_metadata_layout:
            case R.id.overlay_buttons_layout:
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                setBottomNavigationViewVisibility(View.GONE);
                break;

            case R.id.overlay_play_pause_button:
                if (playerIsNotStopped()) {
                    player.onPlayPause();
                } else {
                    openMainPlayer();
                }
                setOverlayPlayPauseImage(player != null && player.isPlaying());
                break;

            case R.id.overlay_close_button:
            case R.id.closeButton:
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                setBottomNavigationViewVisibility(View.VISIBLE);
                break;

            case R.id.playPauseButton:
            case R.id.thumbnailImageView:
            case R.id.backgroundImageView:
                autoPlayEnabled = true;
                openMainPlayer();
                break;

            case R.id.minimizeButton:
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                setBottomNavigationViewVisibility(View.VISIBLE);
                break;
        }
    }

    // Init
    @Override
    protected void initViews(View rootView, Bundle savedInstanceState) {
        super.initViews(rootView, savedInstanceState);

        // video player
        playerPlaceholder = rootView.findViewById(R.id.player_placeholder);
        //preview = rootView.findViewById(R.id.preview);

        this.titleTextView = rootView.findViewById(R.id.titleTextView);
        this.artistTextView = rootView.findViewById(R.id.artistTextView);
        this.thumbnailImageView = rootView.findViewById(R.id.thumbnailImageView);
        this.playPauseButton = rootView.findViewById(R.id.playPauseButton);
        this.backgroundImageView = rootView.findViewById(R.id.backgroundImageView);
        this.minimizeButton = rootView.findViewById(R.id.minimizeButton);
        this.closeButton = rootView.findViewById(R.id.closeButton);

        // overlay views
        overlay = rootView.findViewById(R.id.overlay_layout);
        overlayMetadata = rootView.findViewById(R.id.overlay_metadata_layout);
        overlayThumbnailImageView = rootView.findViewById(R.id.overlay_thumbnail);
        overlayTitleTextView = rootView.findViewById(R.id.overlay_title_text_view);
        overlayChannelTextView = rootView.findViewById(R.id.overlay_channel_text_view);
        overlayButtons = rootView.findViewById(R.id.overlay_buttons_layout);
        overlayPlayPauseButton = rootView.findViewById(R.id.overlay_play_pause_button);
        overlayCloseButton = rootView.findViewById(R.id.overlay_close_button);
        miniProgressBar = rootView.findViewById(R.id.mini_progress_bar);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void initListeners() {
        super.initListeners();
        overlayThumbnailImageView.setOnClickListener(this);
        overlayMetadata.setOnClickListener(this);
        overlayButtons.setOnClickListener(this);
        overlayCloseButton.setOnClickListener(this);
        overlayPlayPauseButton.setOnClickListener(this);
        backgroundImageView.setOnClickListener(this);
        thumbnailImageView.setOnClickListener(this);
        playPauseButton.setOnClickListener(this);
        closeButton.setOnClickListener(this);
        minimizeButton.setOnClickListener(this);

        setupBottomPlayer();
        if (PlayerHolder.bound) {
            PlayerHolder.startService(App.applicationContext, false, this);
        }
    }

    @Override
    public boolean onKeyDown(final int keyCode) {
        return player != null && player.onKeyDown(keyCode);
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    // Info loading and handling
    @Override
    protected void doInitialLoadLogic() {
        if (wasCleared()) {
            return;
        }

        if (currentInfo == null) {
            prepareAndLoadInfo();
        } else {
            prepareAndHandleInfoIfNeededAfterDelay(currentInfo, 0);
        }
    }

    public void selectAndLoadVideo(final int sid, final String videoUrl, final String title, final PlayQueue newQueue) {
        if (player != null && newQueue != null && playQueue != null && !Objects.equals(newQueue.getItem(), playQueue.getItem())) {
            // Preloading can be disabled since playback is surely being replaced.
            player.disablePreloadingOfCurrentTrack();
        }

        setInitialData(sid, videoUrl, title, newQueue);
        startLoading(false, true);
    }

    private void prepareAndHandleInfoIfNeededAfterDelay(final StreamInfo info, final long delay) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (activity == null) {
                return;
            }
            // Data can already be drawn, don't spend time twice
            if (info.getName().equals(titleTextView.getText().toString())) {
                return;
            }
            prepareAndHandleInfo(info);
        }, delay);
    }

    private void prepareAndHandleInfo(final StreamInfo info) {
        showLoading();
        handleResult(info);
    }

    protected void prepareAndLoadInfo() {
        startLoading(false);
    }

    @Override
    public void startLoading(final boolean forceLoad) {
        super.startLoading(forceLoad);
        //preview.setVisibility(View.VISIBLE);
        currentInfo = null;
        if (currentWorker != null) {
            currentWorker.dispose();
        }

        runWorker(forceLoad, false);
    }

    private void startLoading(final boolean forceLoad, final boolean addToBackStack) {
        super.startLoading(forceLoad);
        //preview.setVisibility(View.VISIBLE);
        currentInfo = null;
        if (currentWorker != null) {
            currentWorker.dispose();
        }
        runWorker(forceLoad, addToBackStack);
    }

    private void runWorker(final boolean forceLoad, final boolean addToBackStack) {
        currentWorker = ExtractorHelper.getStreamInfo(serviceId, url, forceLoad)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    isLoading.set(false);
                    hideMainPlayer();
                    handleResult(result);
                    openMainPlayer();
                }, throwable -> {
                    isLoading.set(false);
                    onError(throwable);
                });
    }

    private void openMainPlayer() {
        if (playerService == null) {
            PlayerHolder.startService(App.applicationContext, true, this);
            return;
        }
        if (currentInfo == null) {
            return;
        }

        final PlayQueue queue = setupPlayQueueForIntent(false);

        // Video view can have elements visible from popup,
        // We hide it here but once it ready the view will be shown in handleIntent()
        if (playerService.getView() != null) {
            playerService.getView().setVisibility(View.GONE);
        }
        addVideoPlayerView();

        final Intent playerIntent = NavigationHelper.getPlayerIntent(activity, MainPlayer.class, queue, false, autoPlayEnabled);
        activity.startService(playerIntent);
    }

    private void hideMainPlayer() {
        if (playerService == null || playerService.getView() == null || !player.videoPlayerSelected()) {
            return;
        }

        removeVideoPlayerView();
        playerService.stop(isAutoplayEnabled());
        playerService.getView().setVisibility(View.GONE);
    }

    private PlayQueue setupPlayQueueForIntent(final boolean append) {
        if (append) {
            return new SinglePlayQueue(currentInfo);
        }

        PlayQueue queue = playQueue;
        // Size can be 0 because queue removes bad stream automatically when error occurs
        if (queue == null || queue.isEmpty()) {
            queue = new SinglePlayQueue(currentInfo);
        }

        return queue;
    }

    public void setAutoplay(final boolean autoplay) {
        this.autoPlayEnabled = autoplay;
    }

    // This method overrides default behaviour when setAutoplay() is called.
    // Don't auto play if the user selected an external player or disabled it in settings
    private boolean isAutoplayEnabled() {
        return autoPlayEnabled && (player == null || player.videoPlayerSelected()) && bottomSheetState != BottomSheetBehavior.STATE_HIDDEN;
    }

    private void addVideoPlayerView() {
        if (player == null || getView() == null) {
            //preview.setVisibility(View.VISIBLE);
            return;
        }

        // Check if viewHolder already contains a child
        if (player.getRootView().getParent() != playerPlaceholder) {
            playerService.removeViewFromParent();
        }

        // Prevent from re-adding a view multiple times
        if (player.getRootView().getParent() == null) {
            playerPlaceholder.addView(player.getRootView());
            //preview.setVisibility(View.GONE);
        }
    }

    private void removeVideoPlayerView() {
        makeDefaultHeightForVideoPlaceholder();
        playerService.removeViewFromParent();
    }

    private void makeDefaultHeightForVideoPlaceholder() {
        if (getView() == null) {
            return;
        }
        playerPlaceholder.getLayoutParams().height = FrameLayout.LayoutParams.MATCH_PARENT;
        playerPlaceholder.requestLayout();
    }

    protected void setInitialData(int serviceId, String url, String name, final PlayQueue queue) {
        this.serviceId = serviceId;
        this.url = url;
        this.name = !TextUtils.isEmpty(name) ? name : "";
        playQueue = queue;
    }

    @Override
    public void showError(String message, boolean showRetryButton) {
        showError();
    }

    protected void showError() {
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(View.GONE);
        }
        DialogUtils.showDialogURLNotSupported(activity, (dialog, which) -> {
            if (playerIsNotStopped()) {
                dialog.dismiss();
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                setBottomNavigationViewVisibility(View.VISIBLE);
            }
        });
    }

    private void setupBroadcastReceiver() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                if (intent == null || intent.getAction() == null) return;
                switch (intent.getAction()) {
                    case ACTION_SHOW_MAIN_PLAYER:
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                        setBottomNavigationViewVisibility(View.GONE);
                        break;
                    case ACTION_HIDE_MAIN_PLAYER:
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                        setBottomNavigationViewVisibility(View.VISIBLE);
                        break;
                    case ACTION_MINIMIZE_MAIN_PLAYER:
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        setBottomNavigationViewVisibility(View.VISIBLE);
                        break;
                    case ACTION_PLAYER_STARTED:
                        // If the state is not hidden we don't need to show the mini player
                        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
                            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                            setBottomNavigationViewVisibility(View.VISIBLE);
                        }
                        // Rebound to the service if it was closed via notification or mini player
                        if (!PlayerHolder.bound) {
                            PlayerHolder.startService(App.applicationContext, false, VideoDetailFragment.this);
                        }
                        break;
                }
            }
        };
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_SHOW_MAIN_PLAYER);
        intentFilter.addAction(ACTION_HIDE_MAIN_PLAYER);
        intentFilter.addAction(ACTION_MINIMIZE_MAIN_PLAYER);
        intentFilter.addAction(ACTION_PLAYER_STARTED);
        activity.registerReceiver(broadcastReceiver, intentFilter);
    }

    // Contract
    @Override
    public void showLoading() {
        super.showLoading();
    }

    @SuppressLint("CheckResult")
    @Override
    public void handleResult(@NonNull StreamInfo streamInfo) {
        super.handleResult(streamInfo);
        //preview.setVisibility(View.GONE);
        currentInfo = streamInfo;
        setInitialData(streamInfo.getServiceId(), streamInfo.getOriginalUrl(), streamInfo.getName(), playQueue);

        titleTextView.setText(currentInfo.getName());
        artistTextView.setText(currentInfo.getUploaderName());
        initThumbnailViews(currentInfo);

        if (player == null || player.isPlayerStopped()) {
            updateOverlayData(streamInfo.getName(), streamInfo.getUploaderName(), streamInfo.getThumbnailUrl());
        }

        /*if (!streamInfo.getErrors().isEmpty()) {
            DialogUtils.showDialogURLNotSupported(activity);
        }*/
    }

    private void initThumbnailViews(@NonNull StreamInfo info) {
        GlideUtils.loadThumbnailCircleCrop(activity, thumbnailImageView, info.getThumbnailUrl());
        GlideUtils.loadThumbnail(activity, backgroundImageView, info.getThumbnailUrl());
    }

    @Override
    protected boolean onError(Throwable exception) {
        if (super.onError(exception)) return true;
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(View.GONE);
        }
        DialogUtils.showDialogURLNotSupported(activity, (dialog, which) -> {
            if (playerIsNotStopped()) {
                dialog.dismiss();
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                setBottomNavigationViewVisibility(View.VISIBLE);
            }
        });
        return true;
    }

    @Override
    public void onQueueUpdate(PlayQueue queue) {
        playQueue = queue;
    }

    @Override
    public void onPlaybackUpdate(int state, int repeatMode, boolean shuffled, PlaybackParameters parameters) {
        setOverlayPlayPauseImage(player != null && player.isPlaying());
    }

    @Override
    public void onProgressUpdate(int currentProgress, int duration, int bufferPercent) {
        // Set buffer progress
        miniProgressBar.setSecondaryProgress((int) (miniProgressBar.getMax() * ((float) bufferPercent / 100)));
        // Set Duration
        miniProgressBar.setMax(duration);
        miniProgressBar.setProgress(currentProgress);
    }

    @Override
    public void onMetadataUpdate(StreamInfo info, PlayQueue queue) {
        updateOverlayData(info.getName(), info.getUploaderName(), info.getThumbnailUrl());
        if (currentInfo != null && info.getUrl().equals(currentInfo.getUrl())) {
            return;
        }

        currentInfo = info;
        setInitialData(info.getServiceId(), info.getUrl(), info.getName(), queue);
        setAutoplay(true);
        // Delay execution just because it freezes the main thread, and while playing
        // next/previous video you see visual glitches
        // (when non-vertical video goes after vertical video)
        prepareAndHandleInfoIfNeededAfterDelay(info, 0);
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        if (error.type == ExoPlaybackException.TYPE_SOURCE || error.type == ExoPlaybackException.TYPE_UNEXPECTED) {
            if (playerService == null || player == null) {
                //preview.setVisibility(View.VISIBLE);
            } else {
                //preview.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onServiceStopped() {
        setOverlayPlayPauseImage(false);
        if (currentInfo != null) {
            updateOverlayData(currentInfo.getName(), currentInfo.getUploaderName(), currentInfo.getThumbnailUrl());
        }
    }

    private boolean playerIsNotStopped() {
        return player != null && player.getPlayer() != null && player.getPlayer().getPlaybackState() != Player.STATE_IDLE;
    }

    /*
     * Means that the player fragment was swiped away via BottomSheetLayout
     * and is empty but ready for any new actions. See cleanUp()
     * */
    private boolean wasCleared() {
        return url == null;
    }

    // Remove unneeded information while waiting for a next task
    private void cleanUp() {
        // New beginning
        if (currentWorker != null) {
            currentWorker.dispose();
        }
        PlayerHolder.stopService(App.applicationContext);
        setInitialData(0, null, "", null);
        currentInfo = null;
        updateOverlayData(null, null, null);
    }

    /**
     * Move focus from main fragment to the player or back
     * based on what is currently selected
     *
     * @param toMain if true than the main fragment will be focused or the player otherwise
     */
    private void moveFocusToMainFragment(final boolean toMain) {
        final ViewGroup mainFragment = requireActivity().findViewById(R.id.fragment_holder);
        // Hamburger button steels a focus even under bottomSheet
        final int afterDescendants = ViewGroup.FOCUS_AFTER_DESCENDANTS;
        final int blockDescendants = ViewGroup.FOCUS_BLOCK_DESCENDANTS;
        if (toMain) {
            mainFragment.setDescendantFocusability(afterDescendants);
            ((ViewGroup) requireView()).setDescendantFocusability(blockDescendants);
            mainFragment.requestFocus();
        } else {
            mainFragment.setDescendantFocusability(blockDescendants);
            ((ViewGroup) requireView()).setDescendantFocusability(afterDescendants);
        }
    }

    // Bottom mini player
    private void setupBottomPlayer() {
        final FrameLayout bottomSheetLayout = activity.findViewById(R.id.fragment_player_holder);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
        bottomSheetBehavior.setState(bottomSheetState);
        final int peekHeight = getResources().getDimensionPixelSize(R.dimen.bottom_sheet_height);
        if (bottomSheetState != BottomSheetBehavior.STATE_HIDDEN) {
            //manageSpaceAtTheBottom(false);
            bottomSheetBehavior.setPeekHeight(peekHeight);
            if (bottomSheetState == BottomSheetBehavior.STATE_COLLAPSED) {
                overlay.setAlpha(MAX_OVERLAY_ALPHA);
                setBottomNavigationViewAlpha(MAX_OVERLAY_ALPHA);
                setBottomNavigationViewVisibility(View.VISIBLE);
            } else if (bottomSheetState == BottomSheetBehavior.STATE_EXPANDED) {
                overlay.setAlpha(0);
                setOverlayElementsClickable(false);
                setBottomNavigationViewAlpha(0);
                setBottomNavigationViewVisibility(View.GONE);
            }
        }

        bottomSheetCallback = new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull final View bottomSheet, final int newState) {
                bottomSheetState = newState;

                switch (newState) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                        moveFocusToMainFragment(true);
                        //manageSpaceAtTheBottom(true);
                        bottomSheetBehavior.setPeekHeight(0);
                        setBottomNavigationViewVisibility(View.VISIBLE);
                        cleanUp();
                        break;

                    case BottomSheetBehavior.STATE_EXPANDED:
                        moveFocusToMainFragment(false);
                        //manageSpaceAtTheBottom(false);
                        bottomSheetBehavior.setPeekHeight(peekHeight);
                        setBottomNavigationViewVisibility(View.GONE);
                        // Disable click because overlay buttons located on top of buttons
                        // from the player
                        setOverlayElementsClickable(false);
                        setOverlayLook(1);
                        setBottomNavigationViewLook(1);
                        break;

                    case BottomSheetBehavior.STATE_COLLAPSED:
                        moveFocusToMainFragment(true);
                        //manageSpaceAtTheBottom(false);
                        bottomSheetBehavior.setPeekHeight(peekHeight);
                        setBottomNavigationViewVisibility(View.VISIBLE);

                        // Re-enable clicks
                        setOverlayElementsClickable(true);
                        if (player != null) {
                            player.onQueueClosed();
                        }
                        setOverlayLook(0);
                        setBottomNavigationViewLook(0);
                        break;

                    case BottomSheetBehavior.STATE_DRAGGING:
                    case BottomSheetBehavior.STATE_SETTLING:
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull final View bottomSheet, final float slideOffset) {
                setOverlayLook(slideOffset);
                setBottomNavigationViewLook(slideOffset);
            }
        };
        bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback);

        // User opened a new page and the player will hide itself
        activity.getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                setBottomNavigationViewVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * When the mini player exists the view underneath it is not touchable.
     * Bottom padding should be equal to the mini player's height in this case
     *
     * @param showMore whether main fragment should be expanded or not
     */
    private void manageSpaceAtTheBottom(final boolean showMore) {
        final int peekHeight = getResources().getDimensionPixelSize(R.dimen.mini_player_height);
        final ViewGroup holder = activity.findViewById(R.id.fragment_holder);
        final int newBottomPadding;
        if (showMore) {
            newBottomPadding = 0;
        } else {
            newBottomPadding = peekHeight;
        }
        if (holder.getPaddingBottom() == newBottomPadding) {
            return;
        }
        holder.setPadding(holder.getPaddingLeft(), holder.getPaddingTop(), holder.getPaddingRight(), newBottomPadding);
    }

    private void updateOverlayData(@Nullable final String title, @Nullable final String uploader, @Nullable final String thumbnailUrl) {
        overlayTitleTextView.setText(TextUtils.isEmpty(title) ? "" : title);
        overlayChannelTextView.setText(TextUtils.isEmpty(uploader) ? "" : uploader);
        if (!TextUtils.isEmpty(thumbnailUrl)) {
            GlideUtils.loadThumbnail(App.applicationContext, overlayThumbnailImageView, thumbnailUrl);
        }
    }

    private void setOverlayPlayPauseImage(final boolean playerIsPlaying) {
        final int attr = playerIsPlaying ? R.attr.pause : R.attr.play;
        overlayPlayPauseButton.setImageResource(ThemeHelper.resolveResourceIdFromAttr(activity, attr));
    }

    private void setOverlayLook(final float slideOffset) {
        // SlideOffset < 0 when mini player is about to close via swipe.
        // Stop animation in this case
        if (slideOffset < 0) {
            return;
        }
        overlay.setAlpha(Math.min(MAX_OVERLAY_ALPHA, 1 - slideOffset));
    }

    private void setOverlayElementsClickable(final boolean enable) {
        overlayThumbnailImageView.setClickable(enable);
        overlayMetadata.setClickable(enable);
        overlayButtons.setClickable(enable);
        overlayPlayPauseButton.setClickable(enable);
        overlayCloseButton.setClickable(enable);
    }

    private void setBottomNavigationViewLook(final float slideOffset) {
        if (slideOffset < 0) return;
        setBottomNavigationViewAlpha(Math.min(MAX_OVERLAY_ALPHA, 1 - slideOffset));
    }
}