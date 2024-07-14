package io.ymusic.app.local_player.adapter;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.bumptech.glide.Glide;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.ymusic.app.R;
import io.ymusic.app.api.Lyrics;
import io.ymusic.app.local_player.misc.CustomFragmentStatePagerAdapter;
import io.ymusic.app.local_player.model.Song;
import io.ymusic.app.util.glide.PhonographColoredTarget;
import io.ymusic.app.util.glide.SongGlideRequest;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;

public class AlbumCoverPagerAdapter extends CustomFragmentStatePagerAdapter {

    private final List<Song> dataSet;

    private AlbumCoverFragment.Receiver currentReceiver;
    private int currentReceiverPosition = -1;

    public AlbumCoverPagerAdapter(FragmentManager fm, List<Song> dataSet) {
        super(fm);
        this.dataSet = dataSet;
    }

    @Override
    public Fragment getItem(final int position) {
        return AlbumCoverFragment.newInstance(dataSet.get(position));
    }

    @Override
    public int getCount() {
        return dataSet.size();
    }

    @Override
    @NonNull
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        Object o = super.instantiateItem(container, position);
        if (currentReceiver != null && currentReceiverPosition == position) {
            receiveColor(currentReceiver, currentReceiverPosition);
        }
        return o;
    }

    /**
     * Only the latest passed {@link AlbumCoverFragment.Receiver} is guaranteed to receive a response
     */
    public void receiveColor(AlbumCoverFragment.Receiver colorReceiver, int position) {
        AlbumCoverFragment fragment = (AlbumCoverFragment) getFragment(position);
        if (fragment != null) {
            currentReceiver = null;
            currentReceiverPosition = -1;
            fragment.receiveColor(colorReceiver, position);
        } else {
            currentReceiver = colorReceiver;
            currentReceiverPosition = position;
        }
    }

    public ViewFlipper getViewFlipper(int position) {
        AlbumCoverFragment fragment = (AlbumCoverFragment) getFragment(position);
        return fragment != null ? fragment.viewFlipper : null;
    }

    public static class AlbumCoverFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        private static final String SONG_ARG = "song";

        private Unbinder unbinder;

        @BindView(R.id.player_image)
        ImageView albumCover;

        @BindView(R.id.tvlyrics)
        TextView tvLyrics;

        @BindView(R.id.view_flipper)
        ViewFlipper viewFlipper;

        private boolean isColorReady;
        private int color;
        private Song song;
        private Receiver receiver;
        private int request;
        private int reload = 0;

        public static AlbumCoverFragment newInstance(final Song song) {
            AlbumCoverFragment frag = new AlbumCoverFragment();
            final Bundle args = new Bundle();
            args.putParcelable(SONG_ARG, song);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            song = getArguments().getParcelable(SONG_ARG);
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_album_cover, container, false);
            unbinder = ButterKnife.bind(this, view);
            return view;
        }

        @Override
        public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            forceSquareAlbumCover(false);
            setToolbarControls();
            loadAlbumCover();
            viewFlipper.addOnLayoutChangeListener((view1, i, i1, i2, i3, i4, i5, i6, i7) -> {
                if (viewFlipper.getDisplayedChild() == 1 && reload == 0) {
                    loadLyrics();
                    reload = 1;
                }
            });

        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            unbinder.unbind();
            receiver = null;
        }

        private synchronized void loadAlbumCover() {
            SongGlideRequest.Builder.from(Glide.with(getContext()), song)
                    .generatePalette(getActivity()).build()
                    .into(new PhonographColoredTarget(albumCover) {
                        @Override
                        public void onColorReady(int color) {
                            setColor(color);
                        }
                    });
        }

        private void loadLyrics() {
            String text = tvLyrics.getText().toString();
            if (text.equals(getString(R.string.searching)) ||
                    text.equals(getString(R.string.no_lyrics_found))) {
                return;
            }
            tvLyrics.setText(R.string.searching);
            Lyrics.getLyrics(song.artistName, song.title)
                    .subscribe(new SingleObserver<>() {
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
                                    reload = 0;
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
                                reload = 0;
                            }
                        }
                    });
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        }

        public void forceSquareAlbumCover(boolean forceSquareAlbumCover) {
            albumCover.setScaleType(forceSquareAlbumCover ? ImageView.ScaleType.FIT_CENTER : ImageView.ScaleType.CENTER_CROP);
        }

        private void setColor(int color) {
            this.color = color;
            isColorReady = true;
            if (receiver != null) {
                receiver.onColorReady(color, request);
                receiver = null;
            }
        }

        private void setToolbarControls() {
            if (receiver != null) {
                receiver.onViewFlipperReady(viewFlipper);
            }
        }

        public void receiveColor(Receiver receiver, int request) {
            if (isColorReady) {
                receiver.onColorReady(color, request);
            } else {
                this.receiver = receiver;
                this.request = request;
            }
        }

        public interface Receiver {
            void onColorReady(int color, int request);

            void onViewFlipperReady(ViewFlipper viewFlipper);
        }

    }
}

