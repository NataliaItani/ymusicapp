package io.ymusic.app.player;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;

import org.jetbrains.annotations.NotNull;
import org.schabi.newpipe.extractor.stream.StreamInfo;

import io.ymusic.app.R;
import io.ymusic.app.player.helper.PlayerHelper;
import io.ymusic.app.player.playqueue.PlayQueueItem;
import io.ymusic.app.player.resolver.MediaSourceTag;
import io.ymusic.app.player.resolver.VideoPlaybackResolver;
import io.ymusic.app.util.AnimationUtils;
import io.ymusic.app.util.Constants;

import static io.ymusic.app.util.AnimationUtils.animateView;

@SuppressWarnings({"WeakerAccess"})
public abstract class VideoPlayer extends BasePlayer implements SeekBar.OnSeekBarChangeListener, View.OnClickListener, Player.EventListener {

    // Player
    protected static final int RENDERER_UNAVAILABLE = -1;
    public static final int DEFAULT_CONTROLS_DURATION = 300; // 300 millis

    protected boolean wasPlaying = false;

    @NonNull
    final private VideoPlaybackResolver resolver;

    // Views
    private View rootView;
    private ImageView controlAnimationView;
    private ImageView thumbnailImageView;
    private TextView currentDisplaySeek;
    private SeekBar playbackSeekBar;
    private TextView currentTime;
    private TextView endTime;
    private ValueAnimator controlViewAnimator;
    private String playbackCurrentTimeValue = Constants.PLAYBACK_TIME_DEFAULT;
    private String playbackEndTimeValue = Constants.PLAYBACK_TIME_DEFAULT;

    public VideoPlayer(Context context) {
        super(context);
        this.resolver = new VideoPlaybackResolver(context, dataSource, getQualityResolver());
    }

    public void setup(View rootView) {
        initViews(rootView);
        setup();
    }

    public void initViews(View rootView) {
        this.rootView = rootView;
        this.controlAnimationView = rootView.findViewById(R.id.controlAnimationView);
        this.currentDisplaySeek = rootView.findViewById(R.id.currentDisplaySeek);
        this.thumbnailImageView = rootView.findViewById(R.id.thumbnailImageView);
        this.playbackSeekBar = rootView.findViewById(R.id.playbackSeekBar);
        this.currentTime = rootView.findViewById(R.id.currentTime);
        this.endTime = rootView.findViewById(R.id.endTime);
    }

    @Override
    public void initListeners() {
        super.initListeners();
        playbackSeekBar.setOnSeekBarChangeListener(this);
    }

    @Override
    public void initPlayer(final boolean playOnReady) {
        super.initPlayer(playOnReady);
        // Setup audio session with onboard equalizer
        trackSelector.setParameters(trackSelector.buildUponParameters().setTunnelingAudioSessionId(C.generateAudioSessionIdV21(context)));
    }

    @Override
    public void handleIntent(final Intent intent) {
        if (intent == null) return;
        if (intent.hasExtra(PLAYBACK_QUALITY)) {
            setPlaybackQuality(intent.getStringExtra(PLAYBACK_QUALITY));
        }
        super.handleIntent(intent);
    }

    // Playback Listener
    protected abstract VideoPlaybackResolver.QualityResolver getQualityResolver();

    protected void onMetadataChanged(@NonNull final MediaSourceTag tag) {
        super.onMetadataChanged(tag);
    }

    @Override
    @Nullable
    public MediaSource sourceOf(final PlayQueueItem item, final StreamInfo info) {
        return resolver.resolve(info);
    }

    // States Implementation
    @Override
    public void onBlocked() {
        super.onBlocked();
        playbackSeekBar.setEnabled(false);
    }

    @Override
    public void onPlaying() {
        super.onPlaying();
        showAndAnimateControl(-1, true);
        playbackSeekBar.setEnabled(true);
        animateView(currentDisplaySeek, AnimationUtils.Type.SCALE_AND_ALPHA, false, 200);
    }

    @Override
    public void onBuffering() {

    }

    @Override
    public void onPaused() {

    }

    @Override
    public void onPausedSeek() {
        showAndAnimateControl(-1, true);
    }

    @Override
    public void onCompleted() {
        super.onCompleted();
        animateView(currentDisplaySeek, AnimationUtils.Type.SCALE_AND_ALPHA, false, 200);
    }

    // ExoPlayer Video Listener
    @Override
    public void onTracksChanged(@NotNull TrackGroupArray trackGroups, @NotNull TrackSelectionArray trackSelections) {
        super.onTracksChanged(trackGroups, trackSelections);
    }

    @Override
    public void onPlaybackParametersChanged(@NotNull PlaybackParameters playbackParameters) {
        super.onPlaybackParametersChanged(playbackParameters);
    }

    // General Player
    @Override
    public void onPrepared(boolean playWhenReady) {
        if (simpleExoPlayer != null) {
            playbackSeekBar.setMax((int) simpleExoPlayer.getDuration());
            playbackEndTimeValue = PlayerHelper.getTimeString((int) simpleExoPlayer.getDuration());
        }
        super.onPrepared(playWhenReady);
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    public void onUpdateProgress(int currentProgress, int duration, int bufferPercent) {
        if (!isPrepared()) return;
        if (duration != playbackSeekBar.getMax()) {
            playbackEndTimeValue = PlayerHelper.getTimeString(duration);
            playbackSeekBar.setMax(duration);
        }

        if (currentState != STATE_PAUSED && currentState != STATE_PAUSED_SEEK) {
            playbackSeekBar.setProgress(currentProgress);
        }

        // update playback time
        playbackCurrentTimeValue = PlayerHelper.getTimeString(currentProgress);
        // isLive = 00:00 • Live else 00:00 / 00:00
        currentTime.setText(playbackCurrentTimeValue);
        endTime.setText(playbackEndTimeValue);

        if (simpleExoPlayer != null) {
            if (simpleExoPlayer.isLoading() || bufferPercent > 90) {
                playbackSeekBar.setSecondaryProgress((int) (playbackSeekBar.getMax() * ((float) bufferPercent / 100)));
            }
        }
    }

    @Override
    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
        super.onLoadingComplete(imageUri, view, loadedImage);
        thumbnailImageView.setImageBitmap(loadedImage);
    }

    @Override
    public void onFastRewind() {
        super.onFastRewind();
        showAndAnimateControl(R.drawable.ic_fast_rewind, true);
    }

    @Override
    public void onFastForward() {
        super.onFastForward();
        showAndAnimateControl(R.drawable.ic_fast_forward, true);
    }

    // OnClick related
    @Override
    public void onClick(View v) {

    }

    // SeekBar Listener
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) currentDisplaySeek.setText(PlayerHelper.getTimeString(progress));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        if (getCurrentState() != STATE_PAUSED_SEEK) changeState(STATE_PAUSED_SEEK);
        if (simpleExoPlayer != null) {
            wasPlaying = simpleExoPlayer.getPlayWhenReady();
            if (isPlaying()) simpleExoPlayer.setPlayWhenReady(false);
        }
        animateView(currentDisplaySeek, AnimationUtils.Type.SCALE_AND_ALPHA, true, DEFAULT_CONTROLS_DURATION);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        seekTo(seekBar.getProgress());
        if (simpleExoPlayer != null) {
            if (wasPlaying || simpleExoPlayer.getDuration() == seekBar.getProgress())
                simpleExoPlayer.setPlayWhenReady(true);
        }
        playbackCurrentTimeValue = PlayerHelper.getTimeString(seekBar.getProgress());
        currentTime.setText(playbackCurrentTimeValue);
        endTime.setText(playbackEndTimeValue);
        animateView(currentDisplaySeek, AnimationUtils.Type.SCALE_AND_ALPHA, false, 0);

        if (getCurrentState() == STATE_PAUSED_SEEK) changeState(STATE_BUFFERING);
        if (!isProgressLoopRunning()) startProgressLoop();
    }

    // Utils
    public int getRendererIndex(final int trackIndex) {
        if (simpleExoPlayer == null) return RENDERER_UNAVAILABLE;
        for (int t = 0; t < simpleExoPlayer.getRendererCount(); t++) {
            if (simpleExoPlayer.getRendererType(t) == trackIndex) {
                return t;
            }
        }
        return RENDERER_UNAVAILABLE;
    }

    public void setPlaybackQuality(final String quality) {
        this.resolver.setPlaybackQuality(quality);
    }

    public void showAndAnimateControl(final int drawableId, final boolean goneOnEnd) {
        if (controlViewAnimator != null && controlViewAnimator.isRunning()) {
            controlViewAnimator.end();
        }

        if (drawableId == -1) {
            if (controlAnimationView.getVisibility() == View.VISIBLE) {
                controlViewAnimator = ObjectAnimator.ofPropertyValuesHolder(controlAnimationView,
                        PropertyValuesHolder.ofFloat(View.ALPHA, 1.0f, 0.0f),
                        PropertyValuesHolder.ofFloat(View.SCALE_X, 1.4f, 1.0f),
                        PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.4f, 1.0f)
                ).setDuration(DEFAULT_CONTROLS_DURATION);
                controlViewAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(final Animator animation) {
                        controlAnimationView.setVisibility(View.GONE);
                    }
                });
                controlViewAnimator.start();
            }
            return;
        }

        final float scaleFrom = goneOnEnd ? 1f : 1f;
        final float scaleTo = goneOnEnd ? 1.8f : 1.4f;
        final float alphaFrom = goneOnEnd ? 1f : 0f;
        final float alphaTo = goneOnEnd ? 0f : 1f;


        controlViewAnimator = ObjectAnimator.ofPropertyValuesHolder(controlAnimationView,
                PropertyValuesHolder.ofFloat(View.ALPHA, alphaFrom, alphaTo),
                PropertyValuesHolder.ofFloat(View.SCALE_X, scaleFrom, scaleTo),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, scaleFrom, scaleTo)
        );
        controlViewAnimator.setDuration(goneOnEnd ? 1000 : 500);
        controlViewAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(final Animator animation) {
                if (goneOnEnd) {
                    controlAnimationView.setVisibility(View.GONE);
                } else {
                    controlAnimationView.setVisibility(View.VISIBLE);
                }
            }
        });
        controlAnimationView.setVisibility(View.VISIBLE);
        controlAnimationView.setImageDrawable(AppCompatResources.getDrawable(context, drawableId));
        controlViewAnimator.start();
    }

    public View getRootView() {
        return rootView;
    }

    public void setRootView(View rootView) {
        this.rootView = rootView;
    }
}
