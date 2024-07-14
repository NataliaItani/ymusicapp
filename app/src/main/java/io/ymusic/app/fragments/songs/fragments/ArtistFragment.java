package io.ymusic.app.fragments.songs.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.ymusic.app.R;
import io.ymusic.app.base.BaseFragment;
import io.ymusic.app.fragments.songs.SongsFragment;
import io.ymusic.app.fragments.songs.adapters.ArtistAdapter;
import io.ymusic.app.local_player.loader.SongLoader;
import io.ymusic.app.local_player.misc.WrappedAsyncTaskLoader;
import io.ymusic.app.local_player.model.Song;

public class ArtistFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<List<Song>> {
    @BindView(R.id.items_list)
    RecyclerView recyclerView;
    @BindView(R.id.empty_state_view)
    View emptyView;

    private ArtistAdapter artistAdapter;
    private List<Song> songList = new ArrayList<>();

    public static ArtistFragment getInstance() {
        return new ArtistFragment();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LoaderManager.getInstance(this).initLoader(0, null, this);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_songs, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    protected void initViews(View rootView, Bundle savedInstanceState) {
        super.initViews(rootView, savedInstanceState);

        initRecyclerView();

    }

    private void initRecyclerView() {
        artistAdapter = new ArtistAdapter(activity, songList, R.layout.list_song_item, SongsFragment.SONG_TYPE.ARTIST, null);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(artistAdapter);
    }

    @Override
    protected void initListeners() {
        super.initListeners();
    }

    private void showEmptyViews() {
        emptyView.setVisibility(artistAdapter.getDataSet().isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(artistAdapter.getDataSet().isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        LoaderManager.getInstance(this).restartLoader(0, null, this);
        showEmptyViews();
    }

    @NonNull
    @Override
    public Loader<List<Song>> onCreateLoader(int id, @Nullable Bundle args) {
        return new AsyncSongLoader(getActivity());
    }

    @Override
    public void onLoadFinished(@NonNull Loader<List<Song>> loader, List<Song> data) {
        artistAdapter.swapDataSet(data);
        showEmptyViews();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<List<Song>> loader) {
        artistAdapter.swapDataSet(new ArrayList<>());
        showEmptyViews();
    }

    private static class AsyncSongLoader extends WrappedAsyncTaskLoader<List<Song>> {

        public AsyncSongLoader(Context context) {
            super(context);
        }

        @Override
        public List<Song> loadInBackground() {
            return SongLoader.getAllArtists(getContext());
        }
    }
}