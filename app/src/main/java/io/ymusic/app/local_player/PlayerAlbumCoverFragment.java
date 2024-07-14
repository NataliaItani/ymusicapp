package io.ymusic.app.local_player;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.ymusic.app.R;
import io.ymusic.app.local_player.adapter.AlbumCoverPagerAdapter;
import io.ymusic.app.local_player.helper.MusicPlayerRemote;
import io.ymusic.app.local_player.helper.MusicProgressViewUpdateHelper;

public class PlayerAlbumCoverFragment extends AbsMusicServiceFragment implements ViewPager.OnPageChangeListener, MusicProgressViewUpdateHelper.Callback {

    public static final int VISIBILITY_ANIM_DURATION = 300;

    private Unbinder unbinder;

    @BindView(R.id.player_album_cover_viewpager)
    ViewPager viewPager;

    private Callbacks callbacks;
    private int currentPosition;

    private MusicProgressViewUpdateHelper progressViewUpdateHelper;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_player_album_cover, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewPager.addOnPageChangeListener(this);
        viewPager.setOnTouchListener(new View.OnTouchListener() {
            final GestureDetector gestureDetector = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    if (callbacks != null) {
                        callbacks.onToolbarToggled();
                        return true;
                    }
                    return super.onSingleTapConfirmed(e);
                }
            });

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });
        progressViewUpdateHelper = new MusicProgressViewUpdateHelper(this, 500, 1000);
        progressViewUpdateHelper.start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewPager.removeOnPageChangeListener(this);
        progressViewUpdateHelper.stop();
        unbinder.unbind();
    }

    @Override
    public void onServiceConnected() {
        updatePlayingQueue();
    }

    @Override
    public void onPlayingMetaChanged() {
        viewPager.setCurrentItem(MusicPlayerRemote.getPosition());
    }

    @Override
    public void onQueueChanged() {
        updatePlayingQueue();
    }

    private void updatePlayingQueue() {
        viewPager.setAdapter(new AlbumCoverPagerAdapter(getFragmentManager(), MusicPlayerRemote.getPlayingQueue()));
        viewPager.setCurrentItem(MusicPlayerRemote.getPosition());
        onPageSelected(MusicPlayerRemote.getPosition());
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        Log.e("TAG", "pos: " + position);
        currentPosition = position;
        ((AlbumCoverPagerAdapter) viewPager.getAdapter()).receiveColor(colorReceiver, position);
        callbacks.setViewFlipper(((AlbumCoverPagerAdapter) viewPager.getAdapter()).getViewFlipper(position));
        if (position != MusicPlayerRemote.getPosition()) {
            MusicPlayerRemote.playSongAt(position);
        }
    }

    private final AlbumCoverPagerAdapter.AlbumCoverFragment.Receiver colorReceiver = new AlbumCoverPagerAdapter.AlbumCoverFragment.Receiver() {
        @Override
        public void onColorReady(int color, int requestCode) {
            if (currentPosition == requestCode) {
                notifyColorChange(color);
            }
        }

        @Override
        public void onViewFlipperReady(ViewFlipper viewFlipper) {
            callbacks.setViewFlipper(viewFlipper);
        }
    };

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    private void notifyColorChange(int color) {
        if (callbacks != null) callbacks.onColorChanged(color);
    }

    public void setCallbacks(Callbacks listener) {
        callbacks = listener;
    }

    @Override
    public void onUpdateProgressViews(int progress, int total) {

    }

    public interface Callbacks {
        void onColorChanged(int color);

        void onToolbarToggled();

        void setViewFlipper(ViewFlipper viewFlipper);

    }
}
