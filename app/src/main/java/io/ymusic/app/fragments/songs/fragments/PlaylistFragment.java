package io.ymusic.app.fragments.songs.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.ymusic.app.R;
import io.ymusic.app.base.BaseFragment;
import io.ymusic.app.database.playlist.Playlist;
import io.ymusic.app.fragments.songs.adapters.PlaylistAdapter;
import io.ymusic.app.local_player.loader.PlaylistLoader;
import io.ymusic.app.local_player.misc.WrappedAsyncTaskLoader;

public class PlaylistFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<List<Playlist>> {
    @BindView(R.id.items_list)
    RecyclerView recyclerView;
    @BindView(R.id.create_playlist)
    View createPlaylist;

    private PlaylistAdapter playlistAdapter;
    private List<Playlist> playlists = new ArrayList<>();

    public static PlaylistFragment getInstance() {
        return new PlaylistFragment();
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
        View view = inflater.inflate(R.layout.fragment_playlists, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    protected void initViews(View rootView, Bundle savedInstanceState) {
        super.initViews(rootView, savedInstanceState);

        initRecyclerView();

        createPlaylist.setOnClickListener(view -> {
            View _view = LayoutInflater.from(requireContext()).inflate(R.layout.layout_new_playlist, null);
            EditText editText = _view.findViewById(R.id.playlist_name);
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("New playlist")
                    .setView(_view)
                    .setPositiveButton(R.string.create, (dialog, which) -> {
                        String playlistName = editText.getText().toString();
                        if (!playlistName.isEmpty()) {
                            new Thread(() -> PlaylistLoader.createPlaylist(requireContext(), playlistName)).start();
                            LoaderManager.getInstance(this).restartLoader(0, null, this);
                        } else {
                            Toast.makeText(getContext(), "Enter playlist name", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                    .create()
                    .show();
        });
    }

    private void initRecyclerView() {
        playlistAdapter = new PlaylistAdapter(activity, playlists, R.layout.list_song_item, null);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(playlistAdapter);
    }

    @Override
    protected void initListeners() {
        super.initListeners();
    }

    private void showEmptyViews() {
        recyclerView.setVisibility(playlistAdapter.getDataSet().isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        LoaderManager.getInstance(this).restartLoader(0, null, this);
        showEmptyViews();
    }

    @NonNull
    @Override
    public Loader<List<Playlist>> onCreateLoader(int id, @Nullable Bundle args) {
        return new AsyncSongLoader(getActivity());
    }

    @Override
    public void onLoadFinished(@NonNull Loader<List<Playlist>> loader, List<Playlist> data) {
        playlistAdapter.swapDataSet(data);
        showEmptyViews();
    }

    @Override
    public void onLoaderReset(@NonNull Loader<List<Playlist>> loader) {
        playlistAdapter.swapDataSet(new ArrayList<>());
        showEmptyViews();
    }

    private static class AsyncSongLoader extends WrappedAsyncTaskLoader<List<Playlist>> {

        public AsyncSongLoader(Context context) {
            super(context);
        }

        @Override
        public List<Playlist> loadInBackground() {
            return PlaylistLoader.getAllPlaylist(getContext());
        }
    }
}