package io.ymusic.app.local_player;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.appthemehelper.util.ATHUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.ymusic.app.R;
import io.ymusic.app.local_player.helper.MusicPlayerRemote;
import io.ymusic.app.local_player.helper.MusicProgressViewUpdateHelper;
import io.ymusic.app.local_player.helper.PlayPauseButtonOnClickHandler;
import io.ymusic.app.util.glide.PhonographColoredTarget;
import io.ymusic.app.util.glide.SongGlideRequest;
import io.ymusic.app.util.views.PlayPauseDrawable;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

public class MiniPlayerFragment extends AbsMusicServiceFragment implements MusicProgressViewUpdateHelper.Callback {

    private Unbinder unbinder;

    @BindView(R.id.mini_player_image)
    ImageView miniPlayerImage;
    @BindView(R.id.mini_player_title)
    TextView miniPlayerTitle;
    @BindView(R.id.mini_player_play_pause_button)
    ImageView miniPlayerPlayPauseButton;
    @BindView(R.id.mini_player_close_button)
    ImageView miniPlayerCloseButton;
    @BindView(R.id.progress_bar)
    MaterialProgressBar progressBar;

    private PlayPauseDrawable miniPlayerPlayPauseDrawable;

    private MusicProgressViewUpdateHelper progressViewUpdateHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        progressViewUpdateHelper = new MusicProgressViewUpdateHelper(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mini_player, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        unbinder = ButterKnife.bind(this, view);

        view.setOnTouchListener(new FlingPlayBackController(getActivity()));
        setUpMiniPlayer();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    private void setUpMiniPlayer() {
        setUpPlayPauseButton();
        setUpCloseButton();
        progressBar.setSupportProgressTintList(ColorStateList.valueOf(ThemeStore.accentColor(getActivity())));
    }

    private void setUpPlayPauseButton() {
        miniPlayerPlayPauseDrawable = new PlayPauseDrawable(getActivity());
        miniPlayerPlayPauseButton.setImageDrawable(miniPlayerPlayPauseDrawable);
        miniPlayerPlayPauseButton.setColorFilter(ATHUtil.resolveColor(getActivity(), R.attr.iconColor, ThemeStore.textColorSecondary(getActivity())), PorterDuff.Mode.SRC_IN);
        miniPlayerPlayPauseButton.setOnClickListener(new PlayPauseButtonOnClickHandler());
    }

    private void setUpCloseButton() {
        miniPlayerCloseButton.setOnClickListener(v -> MusicPlayerRemote.clearQueue());
    }

    private void updateSongTitle() {
        miniPlayerTitle.setText(MusicPlayerRemote.getCurrentSong().title);
    }

    private void updateSongImage() {
        SongGlideRequest.Builder.from(Glide.with(activity), MusicPlayerRemote.getCurrentSong())
                .generatePalette(activity).build()
                .into(new PhonographColoredTarget(miniPlayerImage) {
                    @Override
                    public void onLoadCleared(Drawable placeholder) {
                        super.onLoadCleared(placeholder);
                    }

                    @Override
                    public void onColorReady(int color) {

                    }
                });
    }

    @Override
    public void onServiceConnected() {
        updateSongTitle();
        updateSongImage();
        updatePlayPauseDrawableState(false);
    }

    @Override
    public void onPlayingMetaChanged() {
        updateSongTitle();
        updateSongImage();
    }

    @Override
    public void onPlayStateChanged() {
        updatePlayPauseDrawableState(true);
    }

    @Override
    public void onUpdateProgressViews(int progress, int total) {
        progressBar.setMax(total);
        progressBar.setProgress(progress);
    }

    @Override
    public void onResume() {
        super.onResume();
        progressViewUpdateHelper.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        progressViewUpdateHelper.stop();
    }

    private static class FlingPlayBackController implements View.OnTouchListener {

        GestureDetector flingPlayBackController;

        public FlingPlayBackController(Context context) {
            flingPlayBackController = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    if (Math.abs(velocityX) > Math.abs(velocityY)) {
                        if (velocityX < 0) {
                            MusicPlayerRemote.playNextSong();
                            return true;
                        } else if (velocityX > 0) {
                            MusicPlayerRemote.playPreviousSong();
                            return true;
                        }
                    }
                    return false;
                }
            });
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return flingPlayBackController.onTouchEvent(event);
        }
    }

    protected void updatePlayPauseDrawableState(boolean animate) {
        if (MusicPlayerRemote.isPlaying()) {
            miniPlayerPlayPauseDrawable.setPause(animate);
        } else {
            miniPlayerPlayPauseDrawable.setPlay(animate);
        }
    }
}
