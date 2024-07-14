package io.ymusic.app.fragments.songs.fragments;

import static io.ymusic.app.util.FileUtil.AUDIO_FILE_FILTER;

import android.content.Context;
import android.media.MediaScannerConnection;
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
import io.ymusic.app.App;
import io.ymusic.app.R;
import io.ymusic.app.fragments.songs.adapters.SongAdapter;
import io.ymusic.app.fragments.songs.SongsFragment;
import io.ymusic.app.local_player.AbsMusicServiceFragment;
import io.ymusic.app.local_player.loader.SongLoader;
import io.ymusic.app.local_player.misc.WrappedAsyncTaskLoader;
import io.ymusic.app.local_player.model.Song;
import io.ymusic.app.util.FileUtil;

public class SongFragment extends AbsMusicServiceFragment implements LoaderManager.LoaderCallbacks<List<Song>> {
    @BindView(R.id.items_list)
    RecyclerView recyclerView;
    @BindView(R.id.empty_state_view)
    View emptyView;

    private SongAdapter songAdapter;

    public static SongFragment getInstance() {
        return new SongFragment();
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

        // scan files
        scanAudioFiles();

    }

    private void initRecyclerView() {
        songAdapter = new SongAdapter(activity, new ArrayList<>(), R.layout.list_song_item, null);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(songAdapter);
    }

    private void scanAudioFiles() {
        new SongsFragment.ArrayListPathsAsyncTask(paths -> MediaScannerConnection.scanFile(App.applicationContext, paths, null, null)).execute(new SongsFragment.ArrayListPathsAsyncTask.LoadingInfo(FileUtil.getDefaultStartDirectory(), AUDIO_FILE_FILTER));
    }

    @Override
    protected void initListeners() {
        super.initListeners();
    }

    @Override
    public void onMediaStoreChanged() {
        LoaderManager.getInstance(this).restartLoader(0, null, this);
        showEmptyViews();
    }

    private void showEmptyViews() {
        emptyView.setVisibility(songAdapter.getDataSet().isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(songAdapter.getDataSet().isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        scanAudioFiles();
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
        songAdapter.swapDataSet(data);
        showEmptyViews();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<List<Song>> loader) {
        songAdapter.swapDataSet(new ArrayList<>());
        showEmptyViews();
    }

    public static class AsyncSongLoader extends WrappedAsyncTaskLoader<List<Song>> {

        public AsyncSongLoader(Context context) {
            super(context);
        }

        @Override
        public List<Song> loadInBackground() {
            return SongLoader.getAllSongs(getContext());
        }
    }
}