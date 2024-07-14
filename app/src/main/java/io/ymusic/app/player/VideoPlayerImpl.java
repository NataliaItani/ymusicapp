package io.ymusic.app.player;

import static io.ymusic.app.player.MainPlayer.ACTION_CLOSE;
import static io.ymusic.app.player.MainPlayer.ACTION_FAST_FORWARD;
import static io.ymusic.app.player.MainPlayer.ACTION_FAST_REWIND;
import static io.ymusic.app.player.MainPlayer.ACTION_OPEN_CONTROLS;
import static io.ymusic.app.player.MainPlayer.ACTION_PLAY_NEXT;
import static io.ymusic.app.player.MainPlayer.ACTION_PLAY_PAUSE;
import static io.ymusic.app.player.MainPlayer.ACTION_PLAY_PREVIOUS;
import static io.ymusic.app.player.MainPlayer.ACTION_RECREATE_NOTIFICATION;
import static io.ymusic.app.player.MainPlayer.ACTION_REPEAT;
import static io.ymusic.app.player.MainPlayer.ACTION_SHUFFLE;
import static io.ymusic.app.player.helper.PlayerHelper.MinimizeMode.MINIMIZE_ON_EXIT_MODE_BACKGROUND;
import static io.ymusic.app.util.ListHelper.getPopupResolutionIndex;
import static io.ymusic.app.util.ListHelper.getResolutionIndex;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.jakewharton.rxbinding2.view.RxView;
import com.nostra13.universalimageloader.core.assist.FailReason;

import org.jetbrains.annotations.NotNull;
import org.schabi.newpipe.extractor.channel.ChannelInfo;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.ymusic.app.App;
import io.ymusic.app.R;
import io.ymusic.app.ads.AppInterstitialAd;
import io.ymusic.app.api.Lyrics;
import io.ymusic.app.database.subscription.SubscriptionEntity;
import io.ymusic.app.fragments.OnScrollBelowItemsListener;
import io.ymusic.app.fragments.detail.VideoDetailFragment;
import io.ymusic.app.fragments.download.DownloadMusicService;
import io.ymusic.app.local.subscription.SubscriptionService;
import io.ymusic.app.player.event.PlayerEventListener;
import io.ymusic.app.player.event.PlayerServiceEventListener;
import io.ymusic.app.player.helper.PlayerHelper;
import io.ymusic.app.player.playqueue.PlayQueue;
import io.ymusic.app.player.playqueue.PlayQueueItem;
import io.ymusic.app.player.playqueue.PlayQueueItemBuilder;
import io.ymusic.app.player.resolver.AudioPlaybackResolver;
import io.ymusic.app.player.resolver.MediaSourceTag;
import io.ymusic.app.player.resolver.VideoPlaybackResolver;
import io.ymusic.app.util.AnimationUtils;
import io.ymusic.app.util.AppUtils;
import io.ymusic.app.util.ExtractorHelper;
import io.ymusic.app.util.GlideUtils;
import io.ymusic.app.util.ListHelper;
import io.ymusic.app.util.NavigationHelper;
import io.ymusic.app.util.PermissionHelper;
import io.ymusic.app.util.dialog.DialogUtils;
import io.reactivex.Observable;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Unified UI for all players.
 */

public class VideoPlayerImpl extends VideoPlayer {

    private TextView musicButton;
    private TextView lyricsButton;
    private TextView tvLyrics;
    private TextView titleTextView;
    private TextView artistTextView;
    private MaterialButton artistButton;
    private ImageView thumbnailImageView;
    private ImageView backgroundImageView;
    private ImageButton playPauseButton;
    private ImageButton playPreviousButton;
    private ImageButton playNextButton;
    private ImageButton repeatButton;
    private ImageButton shuffleButton;
    private ImageButton minimizeButton;
    private ImageButton closeButton;
    private MaterialButton nextUpButton;
    private MaterialButton downloadButton;
    private ImageView closeQueue;

    private ViewFlipper viewFlipper;

    private BottomSheetBehavior<FrameLayout> bottomSheetBehavior;
    private BottomSheetBehavior.BottomSheetCallback bottomSheetCallback;
    private FrameLayout queueBottomSheet;
    private RecyclerView itemsList;

    private MainPlayer.PlayerType playerType = MainPlayer.PlayerType.AUDIO;

    private boolean audioOnly = false;
    private boolean fragmentIsVisible = false;
    boolean shouldUpdateOnProgress;

    private final MainPlayer service;
    private PlayerServiceEventListener fragmentListener;
    private PlayerEventListener activityListener;
    @NonNull
    private final AudioPlaybackResolver resolver;

    private final CompositeDisposable disposables = new CompositeDisposable();
    private Disposable subscribeButtonMonitor;
    private SubscriptionService subscriptionService;

    @Override
    public void handleIntent(final Intent intent) {
        if (intent.getStringExtra(VideoPlayer.PLAY_QUEUE_KEY) == null) {
            return;
        }

        final MainPlayer.PlayerType oldPlayerType = playerType;
        choosePlayerTypeFromIntent(intent);
        audioOnly = audioPlayerSelected();

        // We need to setup audioOnly before super(), see "sourceOf"
        super.handleIntent(intent);

        if (oldPlayerType != playerType && playQueue != null) {
            // If playerType changes from one to another we should reload the player
            // (to disable/enable video stream or to set quality)
            setRecovery();
            reload();
        }

        if (audioPlayerSelected()) {
            service.removeViewFromParent();
        } else {
            getRootView().setVisibility(View.VISIBLE);
            initVideoPlayer();
            onQueueClosed();
            if (simpleExoPlayer.getPlayWhenReady()) {
                onPlay();
            } else {
                onPause();
            }
        }
        NavigationHelper.sendPlayerStartedEvent(service);

        subscriptionService = SubscriptionService.getInstance(getParentActivity());
    }

    VideoPlayerImpl(final MainPlayer service) {
        super(service);
        this.service = service;
        this.shouldUpdateOnProgress = true;
        this.resolver = new AudioPlaybackResolver(context, dataSource);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void initViews(final View view) {
        super.initViews(view);
        this.musicButton = view.findViewById(R.id.music);
        this.lyricsButton = view.findViewById(R.id.lyrics);
        this.tvLyrics = view.findViewById(R.id.tvlyrics);
        this.titleTextView = view.findViewById(R.id.titleTextView);
        this.artistTextView = view.findViewById(R.id.artistTextView);
        this.artistButton = view.findViewById(R.id.artistButton);
        this.thumbnailImageView = view.findViewById(R.id.thumbnailImageView);
        this.playPauseButton = view.findViewById(R.id.playPauseButton);
        this.playPreviousButton = view.findViewById(R.id.playPreviousButton);
        this.playNextButton = view.findViewById(R.id.playNextButton);
        this.repeatButton = view.findViewById(R.id.repeatButton);
        this.shuffleButton = view.findViewById(R.id.shuffleButton);
        this.minimizeButton = view.findViewById(R.id.minimizeButton);
        this.closeButton = view.findViewById(R.id.closeButton);
        this.nextUpButton = view.findViewById(R.id.nextUpButton);
        this.downloadButton = view.findViewById(R.id.downloadButton);
        this.closeQueue = view.findViewById(R.id.closeQueue);
        this.viewFlipper = view.findViewById(R.id.view_flipper);
        this.backgroundImageView = view.findViewById(R.id.backgroundImageView);

        lyricsButton.setAlpha(0.6f);
        int padding = AppUtils.dpToPx(context, 16);
        int paddingTop = AppUtils.getStatusBarHeight(context) + padding;
        minimizeButton.setPadding(padding, paddingTop, padding, padding);
        closeButton.setPadding(padding, paddingTop, padding, padding);

        this.queueBottomSheet = view.findViewById(R.id.queue_bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(queueBottomSheet);
        bottomSheetBehavior.setDraggable(false);

        this.itemsList = view.findViewById(R.id.items_list);
        // Prevent hiding of bottom sheet via swipe inside queue
        this.itemsList.setNestedScrollingEnabled(false);
    }

    @Override
    public void initListeners() {
        super.initListeners();
        musicButton.setOnClickListener(this);
        lyricsButton.setOnClickListener(this);
        artistTextView.setOnClickListener(this);
        playPauseButton.setOnClickListener(this);
        playPreviousButton.setOnClickListener(this);
        playNextButton.setOnClickListener(this);
        minimizeButton.setOnClickListener(this);
        closeButton.setOnClickListener(this);
        repeatButton.setOnClickListener(this);
        shuffleButton.setOnClickListener(this);
        nextUpButton.setOnClickListener(this);
        downloadButton.setOnClickListener(this);
        closeQueue.setOnClickListener(this);

        bottomSheetCallback = new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull final View bottomSheet, final int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        bottomSheetBehavior.setPeekHeight(0);
                        break;

                    case BottomSheetBehavior.STATE_HALF_EXPANDED:
                    case BottomSheetBehavior.STATE_EXPANDED:
                        final int peekHeight = context.getResources().getDimensionPixelSize(R.dimen.queue_bottom_sheet_height);
                        bottomSheetBehavior.setPeekHeight(peekHeight);
                        break;

                    case BottomSheetBehavior.STATE_DRAGGING:
                    case BottomSheetBehavior.STATE_SETTLING:
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull final View bottomSheet, final float slideOffset) {
                // unimplemented
            }
        };
        bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback);
    }

    private void monitorSubscription(final ChannelInfo info) {

        final Consumer<Throwable> onError = throwable -> {
            AnimationUtils.animateView(artistButton, false, 100);
        };

        final Observable<List<SubscriptionEntity>> observable = subscriptionService.subscriptionTable()
                .getSubscription(info.getServiceId(), info.getUrl())
                .toObservable();

        disposables.add(observable.observeOn(AndroidSchedulers.mainThread()).subscribe(getSubscribeUpdateMonitor(info), onError));

        disposables.add(observable
                // Some updates are very rapid (when calling the updateSubscription(info), for example)
                // so only update the UI for the latest emission ("sync" the subscribe button's state)
                .debounce(100, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(subscriptionEntities -> updateSubscribeButton(!subscriptionEntities.isEmpty()), onError));

    }

    private Function<Object, Object> mapOnSubscribe(final SubscriptionEntity subscription) {

        return object -> {
            subscriptionService.subscriptionTable().insert(subscription);
            return object;
        };
    }

    private Function<Object, Object> mapOnUnsubscribe(final SubscriptionEntity subscription) {

        return object -> {
            subscriptionService.subscriptionTable().delete(subscription);
            return object;
        };
    }

    private void updateSubscription(final ChannelInfo info) {

        final Action onComplete = () -> {
        };

        final Consumer<Throwable> onError = throwable -> {
        };

        disposables.add(subscriptionService.updateChannelInfo(info)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(onComplete, onError));
    }

    private Disposable monitorSubscribeButton(final Button subscribeButton, final Function<Object, Object> action) {

        final Consumer<Object> onNext = object -> {
        };

        final Consumer<Throwable> onError = throwable -> {
        };

        /* Emit clicks from main thread unto io thread */
        return RxView.clicks(subscribeButton)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .debounce(100, TimeUnit.MILLISECONDS) // Ignore rapid clicks
                .map(action)
                .subscribe(onNext, onError);
    }

    private Consumer<List<SubscriptionEntity>> getSubscribeUpdateMonitor(final ChannelInfo info) {

        return subscriptionEntities -> {

            if (subscribeButtonMonitor != null) subscribeButtonMonitor.dispose();

            if (subscriptionEntities.isEmpty()) {
                SubscriptionEntity channel = new SubscriptionEntity();
                channel.setServiceId(info.getServiceId());
                channel.setUrl(info.getUrl());
                channel.setData(info.getName(), info.getAvatarUrl(), info.getDescription(), info.getSubscriberCount());
                subscribeButtonMonitor = monitorSubscribeButton(artistButton, mapOnSubscribe(channel));
            } else {
                final SubscriptionEntity subscription = subscriptionEntities.get(0);
                subscribeButtonMonitor = monitorSubscribeButton(artistButton, mapOnUnsubscribe(subscription));
            }
        };
    }

    private void updateSubscribeButton(boolean isSubscribed) {
        boolean isButtonVisible = artistButton.getVisibility() == View.VISIBLE;
        int backgroundDuration = isButtonVisible ? 300 : 0;
        int textDuration = isButtonVisible ? 200 : 0;

        int subscribeBackground = ContextCompat.getColor(getParentActivity(), R.color.white);
        int subscribeText = ContextCompat.getColor(getParentActivity(), R.color.youtube_primary_color);

        if (!isSubscribed) {
            artistButton.setText(R.string.subscribe);
            Drawable subscribe = ContextCompat.getDrawable(getParentActivity(), R.drawable.ic_subs_white_24dp);
            if (subscribe != null) {
                artistButton.setIcon(subscribe);
                artistButton.setIconTintResource(R.color.youtube_primary_color);
            }
        } else {
            artistButton.setText(R.string.subscribed);
            Drawable unsubscribe = ContextCompat.getDrawable(getParentActivity(), R.drawable.ic_subs_check_white_24dp);
            if (unsubscribe != null) {
                artistButton.setIcon(unsubscribe);
                artistButton.setIconTintResource(R.color.youtube_primary_color);
            }
        }
        AnimationUtils.animateBackgroundColor(artistButton, backgroundDuration, subscribeBackground, subscribeBackground);
        AnimationUtils.animateTextColor(artistButton, textDuration, subscribeText, subscribeText);
        AnimationUtils.animateView(artistButton, AnimationUtils.Type.LIGHT_SCALE_AND_ALPHA, true, 100);
    }

    @SuppressLint("CheckResult")
    private void onStreamDownload(String selectedItemURL) {
        // create getting link dialog
        AlertDialog gettingLinkDialog = DialogUtils.gettingLinkDialog(getParentActivity());
        gettingLinkDialog.show();

        // extractor from video URL
        ExtractorHelper.getStreamInfo(AppUtils.getServiceId(selectedItemURL), selectedItemURL, true)
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
                            Toast.makeText(context, R.string.download_not_supported_url_msg, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        // dismiss getting dialog
                        gettingLinkDialog.dismiss();
                        // show dialog URL not supported
                        Toast.makeText(context, R.string.download_not_supported_url_msg, Toast.LENGTH_SHORT).show();
                    }
                }, throwable -> {
                    // dismiss getting dialog
                    gettingLinkDialog.dismiss();
                    // show dialog URL not supported
                    Toast.makeText(context, R.string.download_not_supported_url_msg, Toast.LENGTH_SHORT).show();
                });
    }

    private void download(StreamInfo streamInfo) {
        Intent intent = new Intent(App.applicationContext, DownloadMusicService.class);
        intent.putExtra(DownloadMusicService.Extra.STREAM_INFO.name(), streamInfo);
        DownloadMusicService.enqueueWork(App.applicationContext, intent);
    }

    public boolean onKeyDown(final int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (getRootView().hasFocus()) {
                    // do not interfere with focus in playlist etc.
                    return false;
                }

                if (getCurrentState() == BasePlayer.STATE_BLOCKED) {
                    return true;
                }
                break;
            default:
                break;
        }

        return false;
    }

    public AppCompatActivity getParentActivity() {
        // ! instanceof ViewGroup means that view was added via windowManager for Popup
        if (getRootView() == null || getRootView().getParent() == null || !(getRootView().getParent() instanceof ViewGroup)) {
            return null;
        }

        final ViewGroup parent = (ViewGroup) getRootView().getParent();
        return (AppCompatActivity) parent.getContext();
    }

    private void updatePlaybackButtons() {
        if (repeatButton == null || shuffleButton == null || simpleExoPlayer == null) {
            return;
        }

        setRepeatModeButton(repeatButton, getRepeatMode());
        setShuffleButton(shuffleButton, playQueue.isShuffled());
    }

    // View
    private void setRepeatModeButton(final ImageButton imageButton, final int repeatMode) {
        switch (repeatMode) {
            case Player.REPEAT_MODE_OFF:
                imageButton.setImageResource(R.drawable.exo_controls_repeat_off);
                break;
            case Player.REPEAT_MODE_ONE:
                imageButton.setImageResource(R.drawable.exo_controls_repeat_one);
                break;
            case Player.REPEAT_MODE_ALL:
                imageButton.setImageResource(R.drawable.exo_controls_repeat_all);
                break;
        }
    }

    private void setShuffleButton(final ImageButton button, final boolean shuffled) {
        final int shuffleAlpha = shuffled ? 255 : 77;
        button.setImageAlpha(shuffleAlpha);
    }

    // ExoPlayer Video Listener
    void onShuffleOrRepeatModeChanged() {
        updatePlaybackButtons();
        updatePlayback();
        NotificationUtil.getInstance().createNotificationIfNeededAndUpdate(this, false);
    }

    @Override
    public void onRepeatModeChanged(final int i) {
        super.onRepeatModeChanged(i);
        onShuffleOrRepeatModeChanged();
    }

    @Override
    public void onShuffleClicked() {
        super.onShuffleClicked();
        onShuffleOrRepeatModeChanged();
    }

    // Playback Listener
    @Override
    public void onPlayerError(@NotNull final ExoPlaybackException error) {
        super.onPlayerError(error);
        if (fragmentListener != null) {
            fragmentListener.onPlayerError(error);
        }
    }

    @Override
    public void onTimelineChanged(@NotNull final Timeline timeline, final int reason) {
        super.onTimelineChanged(timeline, reason);
        // force recreate notification to ensure seek bar is shown when preparation finishes
        NotificationUtil.getInstance().createNotificationIfNeededAndUpdate(this, true);
    }

    @SuppressLint("CheckResult")
    protected void onMetadataChanged(@NonNull final MediaSourceTag tag) {
        super.onMetadataChanged(tag);
        String artist = tag.getMetadata().getUploaderName();
        String title = tag.getMetadata().getName();
        titleTextView.setText(title);
        titleTextView.setSelected(true);
        artistTextView.setText(String.format("%s %s", context.getString(R.string.artist), artist));
        if (disposables != null) disposables.clear();
        if (subscribeButtonMonitor != null) subscribeButtonMonitor.dispose();

        ExtractorHelper.getChannelInfo(AppUtils.getServiceId(tag.getMetadata().getUploaderUrl()), tag.getMetadata().getUploaderUrl(), true)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(channelInfo -> {
                    updateSubscription(channelInfo);
                    monitorSubscription(channelInfo);
                }, throwable -> {

                });
        initThumbnailViews(tag.getMetadata());
        NotificationUtil.getInstance().createNotificationIfNeededAndUpdate(this, false);
        updateMetadata();

        // Get lyrics using OKHttp class
        loadLyrics(artist, title);
    }

    private void loadLyrics(String artist, String title) {
        tvLyrics.setText(R.string.searching);
        Lyrics.getLyrics(artist, title).subscribe(new SingleObserver<>() {
            Disposable dis;

            @Override
            public void onSubscribe(@NonNull Disposable d) {
                dis = d;
            }

            @Override
            public void onSuccess(@NonNull String lyrics) {
                if (tvLyrics != null) {
                    if (lyrics.equals("")) {
                        tvLyrics.setText(R.string.no_lyrics_found);
                    } else {
                        tvLyrics.setText(lyrics);
                    }
                }
                dis.dispose();
            }

            @Override
            public void onError(@NonNull Throwable e) {
                if (tvLyrics != null) {
                    tvLyrics.setText(R.string.check_internet_connection);
                }
            }
        });
    }

    private void initThumbnailViews(@NonNull StreamInfo info) {
        GlideUtils.loadThumbnailCircleCrop(context, thumbnailImageView, info.getThumbnailUrl());
        GlideUtils.loadThumbnail(context, backgroundImageView, info.getThumbnailUrl());
        thumbnailImageView.startAnimation(rotateAnimation());
    }

    public Animation rotateAnimation() {
        return android.view.animation.AnimationUtils.loadAnimation(context, R.anim.rotate);
    }

    @Override
    public void onPlaybackShutdown() {
        service.onDestroy();
    }

    @Override
    public void onUpdateProgress(final int currentProgress, final int duration, final int bufferPercent) {
        super.onUpdateProgress(currentProgress, duration, bufferPercent);
        updateProgress(currentProgress, duration, bufferPercent);

        // setMetadata only updates the metadata when any of the metadata keys are null
        mediaSessionManager.setMetadata(getVideoTitle(), getUploaderName(), getThumbnail(), duration);
    }

    @Override
    public void onPlayQueueEdited() {
        updatePlayback();
        NotificationUtil.getInstance().createNotificationIfNeededAndUpdate(this, false);
    }

    @Override
    @Nullable
    public MediaSource sourceOf(final PlayQueueItem item, final StreamInfo info) {
        // For LiveStream or video/popup players we can use super() method
        // but not for audio player
        if (!audioOnly) {
            return super.sourceOf(item, info);
        } else {
            return resolver.resolve(info);
        }
    }

    @Override
    public void onPlayPrevious() {
        super.onPlayPrevious();
        triggerProgressUpdate();
    }

    @Override
    public void onPlayNext() {
        super.onPlayNext();
        triggerProgressUpdate();
    }

    @Override
    protected void initPlayback(@NonNull final PlayQueue queue, final int repeatMode,
                                final float playbackSpeed, final float playbackPitch,
                                final boolean playbackSkipSilence,
                                final boolean playOnReady, final boolean isMuted) {
        super.initPlayback(queue, repeatMode, playbackSpeed, playbackPitch, playbackSkipSilence, playOnReady, isMuted);
        updateQueue();
    }

    @Override
    public void onClick(final View v) {
        super.onClick(v);
        if (v.getId() == playPauseButton.getId()) {
            onPlayPause();
        } else if (v.getId() == playPreviousButton.getId()) {
            onPlayPrevious();
        } else if (v.getId() == playNextButton.getId()) {
            onPlayNext();
        } else if (v.getId() == minimizeButton.getId()) {
            service.sendBroadcast(new Intent(VideoDetailFragment.ACTION_MINIMIZE_MAIN_PLAYER));
        } else if (v.getId() == closeButton.getId()) {
            service.sendBroadcast(new Intent(VideoDetailFragment.ACTION_HIDE_MAIN_PLAYER));
        } else if (v.getId() == repeatButton.getId()) {
            onRepeatClicked();
        } else if (v.getId() == shuffleButton.getId()) {
            onShuffleClicked();
        } else if (v.getId() == nextUpButton.getId()) {
            onQueueClicked();
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        } else if (v.getId() == closeQueue.getId()) {
            onQueueClosed();
        } else if (v.getId() == downloadButton.getId()) {
//            if (AppUtils.isLoggedIn(getParentActivity())) {
            if (!PermissionHelper.checkStoragePermissions(getParentActivity(), PermissionHelper.DOWNLOADS_REQUEST_CODE)) {
                return;
            }
            // download
            if (currentItem != null) {
                AppInterstitialAd.getInstance().showInterstitialAd(getParentActivity(), () -> onStreamDownload(currentItem.getUrl()));
            } else {
                // show dialog URL not supported
                Toast.makeText(context, R.string.download_not_supported_url_msg, Toast.LENGTH_SHORT).show();
            }
//            } else {
//                DialogUtils.showSignupDialog(getParentActivity(), (dialogInterface, i) -> {
//                    Intent intent = new Intent(getParentActivity(), SettingsActivity.class);
//                    intent.putExtra(SettingsActivity.Extra.LOGIN.name(), true);
//                    getParentActivity().startActivity(intent);
//                }, (dialogInterface, i) -> dialogInterface.dismiss());
//            }
        } else if (v.getId() == artistTextView.getId()) {
            if (currentItem != null) {
                // Requires the parent fragment to find holder for fragment replacement
                NavigationHelper.openChannelFragment(getParentActivity().getSupportFragmentManager(),
                        currentItem.getServiceId(),
                        currentItem.getUploaderUrl(),
                        currentItem.getUploader());
            }
        } else if (v.getId() == musicButton.getId()) {
            if (musicButton.getAlpha() == 0.6f) {
                viewFlipper.setInAnimation(context, android.R.anim.slide_in_left);
                viewFlipper.setOutAnimation(context, android.R.anim.slide_out_right);
                viewFlipper.showPrevious();
                musicButton.setAlpha(1f);
                lyricsButton.setAlpha(0.6f);
            }
        } else if (v.getId() == lyricsButton.getId()) {
            if (lyricsButton.getAlpha() == 0.6f) {
                viewFlipper.setInAnimation(context, R.anim.slide_in_right);
                viewFlipper.setOutAnimation(context, R.anim.slide_out_left);
                viewFlipper.showNext();
                lyricsButton.setAlpha(1);
                musicButton.setAlpha(0.6f);

                String text = tvLyrics.getText().toString();
                if (text.equals(context.getString(R.string.searching)) ||
                        text.equals(context.getString(R.string.no_lyrics_found))
                        || !text.equals(context.getString(R.string.check_internet_connection))
                ) {
                    return;
                }
                String artist = artistTextView.getText().toString().split(context.getString(R.string.artist) + " ")[1];
                String title = titleTextView.getText().toString();
                loadLyrics(artist, title);
            }
        }
    }

    private void onQueueClicked() {
        buildQueue();
        updatePlaybackButtons();
        queueBottomSheet.requestFocus();
        itemsList.scrollToPosition(playQueue.getIndex());
    }

    public void onQueueClosed() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    @Override
    public void onStopTrackingTouch(final SeekBar seekBar) {
        super.onStopTrackingTouch(seekBar);
    }

    @Override
    protected VideoPlaybackResolver.QualityResolver getQualityResolver() {
        return new VideoPlaybackResolver.QualityResolver() {
            @Override
            public int getDefaultResolutionIndex(final List<VideoStream> sortedVideos) {
                return videoPlayerSelected() ? ListHelper.getDefaultResolutionIndex(context, sortedVideos) : ListHelper.getPopupDefaultResolutionIndex(context, sortedVideos);
            }

            @Override
            public int getOverrideResolutionIndex(final List<VideoStream> sortedVideos, final String playbackQuality) {
                return videoPlayerSelected() ? getResolutionIndex(context, sortedVideos, playbackQuality) : getPopupResolutionIndex(context, sortedVideos, playbackQuality);
            }
        };
    }

    @Override
    public void changeState(final int state) {
        super.changeState(state);
        updatePlayback();
    }

    @Override
    public void onBlocked() {
        super.onBlocked();
        playPauseButton.setImageResource(R.drawable.ic_play_circle_outline_24dp);
        thumbnailImageView.clearAnimation();
        getRootView().setKeepScreenOn(false);
        NotificationUtil.getInstance().createNotificationIfNeededAndUpdate(this, false);
    }

    @Override
    public void onBuffering() {
        super.onBuffering();
        getRootView().setKeepScreenOn(true);
        if (NotificationUtil.getInstance().shouldUpdateBufferingSlot()) {
            NotificationUtil.getInstance().createNotificationIfNeededAndUpdate(this, false);
        }
    }

    @Override
    public void onPlaying() {
        super.onPlaying();
        playPauseButton.setImageResource(R.drawable.ic_pause_circle_outline_24dp);
        if (thumbnailImageView.getAnimation() == null) {
            thumbnailImageView.startAnimation(rotateAnimation());
        }
        getRootView().setKeepScreenOn(true);
        NotificationUtil.getInstance().createNotificationIfNeededAndUpdate(this, false);
    }

    @Override
    public void onPaused() {
        super.onPaused();
        playPauseButton.setImageResource(R.drawable.ic_play_circle_outline_24dp);
        thumbnailImageView.clearAnimation();
        // Remove running notification when user don't want music (or video in popup)
        if (!backgroundPlaybackEnabled() && videoPlayerSelected()) {
            NotificationUtil.getInstance().cancelNotificationAndStopForeground(service);
        } else {
            NotificationUtil.getInstance().createNotificationIfNeededAndUpdate(this, false);
        }
        getRootView().setKeepScreenOn(false);
    }

    @Override
    public void onPausedSeek() {
        super.onPausedSeek();
        getRootView().setKeepScreenOn(true);
        NotificationUtil.getInstance().createNotificationIfNeededAndUpdate(this, false);
    }

    @Override
    public void onCompleted() {
        playPauseButton.setImageResource(R.drawable.ic_play_circle_outline_24dp);
        thumbnailImageView.clearAnimation();
        getRootView().setKeepScreenOn(false);
        NotificationUtil.getInstance().createNotificationIfNeededAndUpdate(this, false);
        super.onCompleted();
    }

    @Override
    public void destroy() {
        super.destroy();
        if (disposables != null) disposables.clear();
        if (subscribeButtonMonitor != null) subscribeButtonMonitor.dispose();
        bottomSheetBehavior.removeBottomSheetCallback(bottomSheetCallback);
    }

    // Broadcast Receiver
    @Override
    protected void setupBroadcastReceiver(final IntentFilter intentFilter) {
        super.setupBroadcastReceiver(intentFilter);
        intentFilter.addAction(ACTION_CLOSE);
        intentFilter.addAction(ACTION_PLAY_PAUSE);
        intentFilter.addAction(ACTION_OPEN_CONTROLS);
        intentFilter.addAction(ACTION_REPEAT);
        intentFilter.addAction(ACTION_PLAY_PREVIOUS);
        intentFilter.addAction(ACTION_PLAY_NEXT);
        intentFilter.addAction(ACTION_FAST_REWIND);
        intentFilter.addAction(ACTION_FAST_FORWARD);
        intentFilter.addAction(ACTION_SHUFFLE);
        intentFilter.addAction(ACTION_RECREATE_NOTIFICATION);

        intentFilter.addAction(VideoDetailFragment.ACTION_VIDEO_FRAGMENT_RESUMED);
        intentFilter.addAction(VideoDetailFragment.ACTION_VIDEO_FRAGMENT_STOPPED);

        intentFilter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);

        intentFilter.addAction(Intent.ACTION_HEADSET_PLUG);
    }

    @Override
    public void onBroadcastReceived(final Intent intent) {
        super.onBroadcastReceived(intent);
        if (intent.getAction() == null) {
            return;
        }

        switch (intent.getAction()) {
            case ACTION_CLOSE:
                service.onDestroy();
                break;
            case ACTION_PLAY_NEXT:
                onPlayNext();
                break;
            case ACTION_PLAY_PREVIOUS:
                onPlayPrevious();
                break;
            case ACTION_FAST_FORWARD:
                onFastForward();
                break;
            case ACTION_FAST_REWIND:
                onFastRewind();
                break;
            case ACTION_PLAY_PAUSE:
                onPlayPause();
                if (!fragmentIsVisible) {
                    // Ensure that we have audio-only stream playing when a user
                    // started to play from notification's play button from outside of the app
                    onFragmentStopped();
                }
                break;
            case ACTION_REPEAT:
                onRepeatClicked();
                break;
            case ACTION_SHUFFLE:
                onShuffleClicked();
                break;
            case ACTION_RECREATE_NOTIFICATION:
                NotificationUtil.getInstance().createNotificationIfNeededAndUpdate(this, true);
                break;
            case Intent.ACTION_HEADSET_PLUG:
                /*notificationManager.cancel(NOTIFICATION_ID);
                mediaSessionManager.dispose();
                mediaSessionManager.enable(getBaseContext(), basePlayerImpl.simpleExoPlayer);*/
                break;
            case VideoDetailFragment.ACTION_VIDEO_FRAGMENT_RESUMED:
                fragmentIsVisible = true;
                useVideoSource(true);
                break;
            case VideoDetailFragment.ACTION_VIDEO_FRAGMENT_STOPPED:
                fragmentIsVisible = false;
                onFragmentStopped();
                break;
            case Intent.ACTION_CONFIGURATION_CHANGED:
                // Close it because when changing orientation from portrait
                // (in fullscreen mode) the size of queue layout can be larger than the screen size
                onQueueClosed();
                break;
            case Intent.ACTION_SCREEN_ON:
                shouldUpdateOnProgress = true;
                // Interrupt playback only when screen turns on
                // and user is watching video in popup player.
                // Same actions for video player will be handled in ACTION_VIDEO_FRAGMENT_RESUMED
                if ((isPlaying() || isLoading())) {
                    useVideoSource(true);
                }
                break;
            case Intent.ACTION_SCREEN_OFF:
                shouldUpdateOnProgress = false;
                // Interrupt playback only when screen turns off with popup player working
                if ((isPlaying() || isLoading())) {
                    useVideoSource(false);
                }
                break;
        }
    }

    // Thumbnail Loading
    @Override
    public void onLoadingComplete(final String imageUri, final View view, final Bitmap loadedImage) {
        super.onLoadingComplete(imageUri, view, loadedImage);
        NotificationUtil.getInstance().createNotificationIfNeededAndUpdate(this, false);
    }

    @Override
    public void onLoadingFailed(final String imageUri, final View view, final FailReason failReason) {
        super.onLoadingFailed(imageUri, view, failReason);
        NotificationUtil.getInstance().createNotificationIfNeededAndUpdate(this, false);
    }

    @Override
    public void onLoadingCancelled(final String imageUri, final View view) {
        super.onLoadingCancelled(imageUri, view);
        NotificationUtil.getInstance().createNotificationIfNeededAndUpdate(this, false);
    }

    // Utils
    private void choosePlayerTypeFromIntent(final Intent intent) {
        // If you want to open popup from the app just include Constants.POPUP_ONLY into an extra
        if (intent.getIntExtra(PLAYER_TYPE, PLAYER_TYPE_VIDEO) == PLAYER_TYPE_AUDIO) {
            playerType = MainPlayer.PlayerType.AUDIO;
        } else {
            playerType = MainPlayer.PlayerType.VIDEO;
        }
    }

    public boolean backgroundPlaybackEnabled() {
        return PlayerHelper.getMinimizeOnExitAction(service) == MINIMIZE_ON_EXIT_MODE_BACKGROUND;
    }

    public boolean videoPlayerSelected() {
        return playerType == MainPlayer.PlayerType.VIDEO;
    }

    public boolean audioPlayerSelected() {
        return playerType == MainPlayer.PlayerType.AUDIO;
    }

    public boolean isPlayerStopped() {
        return getPlayer() == null || getPlayer().getPlaybackState() == SimpleExoPlayer.STATE_IDLE;
    }

    public void disablePreloadingOfCurrentTrack() {
        getLoadController().disablePreloadingOfCurrentTrack();
    }

    private void buildQueue() {
        itemsList.setAdapter(playQueueAdapter);
        itemsList.setClickable(true);
        itemsList.setLongClickable(true);
        itemsList.clearOnScrollListeners();
        itemsList.addOnScrollListener(getQueueScrollListener());
        playQueueAdapter.setSelectedListener(getOnSelectedListener());
    }

    public void useVideoSource(final boolean video) {
        if (playQueue == null || audioOnly == !video || audioPlayerSelected()) {
            return;
        }
        audioOnly = !video;
        setRecovery();
        reload();
    }

    private OnScrollBelowItemsListener getQueueScrollListener() {
        return new OnScrollBelowItemsListener() {
            @Override
            public void onScrolledDown(final RecyclerView recyclerView) {
                if (playQueue != null && !playQueue.isComplete()) {
                    playQueue.fetch();
                } else if (itemsList != null) {
                    itemsList.clearOnScrollListeners();
                }
            }
        };
    }

    private PlayQueueItemBuilder.OnSelectedListener getOnSelectedListener() {
        return new PlayQueueItemBuilder.OnSelectedListener() {
            @Override
            public void selected(final PlayQueueItem item, final View view) {
                AppInterstitialAd.getInstance().showInterstitialAd(getParentActivity(), () -> onSelected(item));
            }

            @Override
            public void held(final PlayQueueItem item, final View view) {
                final int index = playQueue.indexOf(item);
                if (index != -1) {
                    playQueue.remove(index);
                }
            }
        };
    }

    private void initVideoPlayer() {
        getRootView().setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    }

    // Manipulations with listener
    public void setFragmentListener(final PlayerServiceEventListener listener) {
        fragmentListener = listener;
        fragmentIsVisible = true;
        updateQueue();
        updateMetadata();
        updatePlayback();
        triggerProgressUpdate();
    }

    public void removeFragmentListener(final PlayerServiceEventListener listener) {
        if (fragmentListener == listener) {
            fragmentListener = null;
        }
    }

    void setActivityListener(final PlayerEventListener listener) {
        activityListener = listener;
        updateMetadata();
        updatePlayback();
        triggerProgressUpdate();
    }

    void removeActivityListener(final PlayerEventListener listener) {
        if (activityListener == listener) {
            activityListener = null;
        }
    }

    private void updateQueue() {
        if (fragmentListener != null && playQueue != null) {
            fragmentListener.onQueueUpdate(playQueue);
        }
        if (activityListener != null && playQueue != null) {
            activityListener.onQueueUpdate(playQueue);
        }
    }

    private void updateMetadata() {
        if (fragmentListener != null && getCurrentMetadata() != null) {
            fragmentListener.onMetadataUpdate(getCurrentMetadata().getMetadata(), playQueue);
        }
        if (activityListener != null && getCurrentMetadata() != null) {
            activityListener.onMetadataUpdate(getCurrentMetadata().getMetadata(), playQueue);
        }
    }

    private void updatePlayback() {
        if (fragmentListener != null && simpleExoPlayer != null && playQueue != null) {
            fragmentListener.onPlaybackUpdate(currentState, getRepeatMode(), playQueue.isShuffled(), simpleExoPlayer.getPlaybackParameters());
        }
        if (activityListener != null && simpleExoPlayer != null && playQueue != null) {
            activityListener.onPlaybackUpdate(currentState, getRepeatMode(), playQueue.isShuffled(), getPlaybackParameters());
        }
    }

    private void updateProgress(final int currentProgress, final int duration, final int bufferPercent) {
        if (fragmentListener != null) {
            fragmentListener.onProgressUpdate(currentProgress, duration, bufferPercent);
        }
        if (activityListener != null) {
            activityListener.onProgressUpdate(currentProgress, duration, bufferPercent);
        }
    }

    void stopActivityBinding() {
        if (fragmentListener != null) {
            fragmentListener.onServiceStopped();
            fragmentListener = null;
        }
        if (activityListener != null) {
            activityListener.onServiceStopped();
            activityListener = null;
        }
    }

    /**
     * This will be called when a user goes to another app/activity, turns off a screen.
     * We don't want to interrupt playback and don't want to see notification so
     * next lines of code will enable audio-only playback only if needed
     */
    private void onFragmentStopped() {
        if (videoPlayerSelected() && (isPlaying() || isLoading())) {
            if (backgroundPlaybackEnabled()) {
                useVideoSource(false);
            } else {
                onPause();
            }
        }
    }
}
