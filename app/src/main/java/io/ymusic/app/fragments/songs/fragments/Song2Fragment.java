package io.ymusic.app.fragments.songs.fragments;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.ymusic.app.R;
import io.ymusic.app.base.BaseFragment;
import io.ymusic.app.fragments.BackPressable;
import io.ymusic.app.fragments.songs.SongsFragment.SONG_TYPE;
import io.ymusic.app.fragments.songs.adapters.SongAdapter;
import io.ymusic.app.local_player.loader.SongLoader;
import io.ymusic.app.local_player.misc.WrappedAsyncTaskLoader;
import io.ymusic.app.local_player.model.Song;
import io.ymusic.app.util.NavigationHelper;

public class Song2Fragment extends BaseFragment implements LoaderManager.LoaderCallbacks<List<Song>>, BackPressable {

    @BindView(R.id.items_list)
    RecyclerView recyclerView;
    @BindView(R.id.empty_state_view)
    View emptyView;
    @BindView(R.id.back_arrow)
    View backArrow;
    @BindView(R.id.title)
    TextView titleTxt;

    private SongAdapter songAdapter;

    private final long id;
    private final String title;
    private final SONG_TYPE type;

    public static Song2Fragment getInstance(SONG_TYPE type, long id, String title) {
        return new Song2Fragment(type, id, title);
    }

    public Song2Fragment(SONG_TYPE type, long id, String title) {
        this.type = type;
        this.id = id;
        this.title = title;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LoaderManager.getInstance(this).initLoader(0, null, this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_song2, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    protected void initViews(View rootView, Bundle savedInstanceState) {
        super.initViews(rootView, savedInstanceState);

        titleTxt.setText(title);
        backArrow.setOnClickListener(view -> NavigationHelper.goBack(activity.getSupportFragmentManager()));

        initRecyclerView();
    }

    private void initRecyclerView() {
        songAdapter = new SongAdapter(activity, new ArrayList<>(), R.layout.list_song_item, null);
        songAdapter.setType(type);
        songAdapter.setPlaylistId((int)id);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(songAdapter);
    }

    private void showEmptyViews() {
        emptyView.setVisibility(songAdapter.getDataSet().isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(songAdapter.getDataSet().isEmpty() ? View.GONE : View.VISIBLE);
    }

    @NonNull
    @Override
    public Loader<List<Song>> onCreateLoader(int id, @Nullable Bundle args) {
        return new AsyncSongLoader(requireContext(), this.id, this.type);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<List<Song>> loader, List<Song> data) {
        songAdapter.swapDataSet(data);
        showEmptyViews();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<List<Song>> loader) {

    }

    @Override
    public boolean onBackPressed() {
        NavigationHelper.goBack(activity.getSupportFragmentManager());
        return true;
    }

    private static class AsyncSongLoader extends WrappedAsyncTaskLoader<List<Song>> {

        private final long id;
        private final SONG_TYPE type;

        public AsyncSongLoader(Context context, long id, SONG_TYPE type) {
            super(context);
            this.id = id;
            this.type = type;
        }

        @Override
        public List<Song> loadInBackground() {
            if (type == SONG_TYPE.PLAYLIST) {    // Playlist
                return SongLoader.getAllPlaylistSongs(getContext(), (int) id);
            }
            // Artist and Album
            return SongLoader.getAllSongsWithId(getContext(), id, type);
        }
    }
}