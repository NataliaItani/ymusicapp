package io.ymusic.app.fragments.songs.adapters;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import io.ymusic.app.R;
import io.ymusic.app.database.playlist.Playlist;
import io.ymusic.app.fragments.songs.SongsFragment;
import io.ymusic.app.local_player.base.AbsMultiSelectAdapter;
import io.ymusic.app.local_player.base.MediaEntryViewHolder;
import io.ymusic.app.local_player.helper.SongMenuHelper;
import io.ymusic.app.local_player.interfaces.CabHolder;
import io.ymusic.app.local_player.loader.PlaylistLoader;
import io.ymusic.app.local_player.model.Song;
import io.ymusic.app.util.NavigationHelper;

public class PlaylistAdapter extends AbsMultiSelectAdapter<PlaylistAdapter.ViewHolder, Playlist> {

    protected final AppCompatActivity activity;
    protected List<Playlist> dataSet;
    protected int itemLayoutRes;

    public PlaylistAdapter(AppCompatActivity activity, List<Playlist> dataSet, @LayoutRes int itemLayoutRes, @Nullable CabHolder cabHolder) {
        super(activity, cabHolder, R.menu.menu_media_selection);
        this.activity = activity;
        this.dataSet = dataSet;
        this.itemLayoutRes = itemLayoutRes;
        setHasStableIds(true);
    }

    public void swapDataSet(List<Playlist> dataSet) {
        this.dataSet = dataSet;
        notifyDataSetChanged();
    }

    public List<Playlist> getDataSet() {
        return dataSet;
    }

    @Override
    public long getItemId(int position) {
        return dataSet.get(position).getId();
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false);
        return createViewHolder(view);
    }

    protected ViewHolder createViewHolder(View view) {
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final Playlist playlist = dataSet.get(position);

        boolean isChecked = isChecked(playlist);
        holder.itemView.setActivated(isChecked);

        if (holder.title != null) {
            holder.title.setText(playlist.getTitle());
        }
        if (holder.text != null) {
            int size = playlist.getSize();
            holder.text.setText(size + (size > 1 ? " songs" : " song"));
        }

        loadAlbumCover(playlist, holder);
    }

    protected void loadAlbumCover(Playlist playlist, final ViewHolder holder) {
        if (holder.image == null) return;

//        SongGlideRequest.Builder.from(Glide.with(activity), song)
//                .generatePalette(activity).build()
//                .into(new PhonographColoredTarget(holder.image) {
//                    @Override
//                    public void onLoadCleared(Drawable placeholder) {
//                        super.onLoadCleared(placeholder);
//                        setColors(getDefaultFooterColor(), holder);
//                    }
//
//                    @Override
//                    public void onColorReady(int color) {
//                        setColors(getDefaultFooterColor(), holder);
//                    }
//                });
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    @Override
    protected Playlist getIdentifier(int position) {
        return dataSet.get(position);
    }

    @Override
    protected void onMultipleItemAction(MenuItem menuItem, List<Playlist> selection) {

    }

    @Override
    protected String getName(Playlist playlist) {
        return playlist.getTitle();
    }


    public class ViewHolder extends MediaEntryViewHolder {

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            setImageTransitionName(activity.getString(R.string.transition_album_art));

            if (menu == null) {
                return;
            }
            menu.setVisibility(View.VISIBLE);
            menu.setOnClickListener(new SongMenuHelper.OnClickSongMenu(activity) {
                @Override
                public Song getSong() {
                    return null;
                }

                @Override
                public SongsFragment.SONG_TYPE getType() {
                    return null;
                }

                @Override
                public int getMenuRes() {
                    return R.menu.menu_playlist;
                }

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return onPlaylistMenuItemClick(item) || super.onMenuItemClick(item);
                }
            });
        }

        protected boolean onPlaylistMenuItemClick(MenuItem item) {
            Playlist playlist = dataSet.get(getBindingAdapterPosition());
            switch (item.getItemId()) {
                case R.id.action_rename_playlist:
                    View _view = LayoutInflater.from(itemView.getContext()).inflate(R.layout.layout_new_playlist, null);
                    EditText editText = _view.findViewById(R.id.playlist_name);
                    editText.setText(playlist.getTitle());
                    new MaterialAlertDialogBuilder(itemView.getContext())
                            .setTitle("Edit playlist")
                            .setView(_view)
                            .setPositiveButton(R.string.ok, (dialog, which) -> {
                                String playlistName = editText.getText().toString();
                                if (!playlistName.isEmpty()) {
                                    playlist.setTitle(playlistName);
                                    new Thread(() -> PlaylistLoader.editPlaylist(itemView.getContext(), playlist)).start();
                                    notifyItemChanged(getBindingAdapterPosition());
                                } else {
                                    Toast.makeText(itemView.getContext(), "Playlist name cannot be empty", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                            .create()
                            .show();
                    break;
                case R.id.action_delete_playlist:
                    new MaterialAlertDialogBuilder(itemView.getContext())
                            .setTitle(R.string.delete_song_title)
                            .setMessage(itemView.getContext().getString(R.string.delete_playlist_x, playlist.getTitle()))
                            .setPositiveButton(R.string.delete, (dialog, which) -> {
                                new Thread(() -> PlaylistLoader.deletePlaylist(itemView.getContext(), playlist)).start();
                                dataSet.remove(getBindingAdapterPosition());
                                notifyItemRemoved(getBindingAdapterPosition());
                            })
                            .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                            .create()
                            .show();
                    break;
            }
            return false;
        }

        @Override
        public void onClick(View v) {
            // show songs in playlist
            Playlist playlist = dataSet.get(getBindingAdapterPosition());
            NavigationHelper.openSongs(activity.getSupportFragmentManager(), SongsFragment.SONG_TYPE.PLAYLIST, playlist.getId(), playlist.getTitle());

        }
    }
}
