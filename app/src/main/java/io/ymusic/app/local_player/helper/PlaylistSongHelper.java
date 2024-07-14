package io.ymusic.app.local_player.helper;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import io.ymusic.app.R;
import io.ymusic.app.database.playlist.Playlist;
import io.ymusic.app.database.playlist.PlaylistSong;
import io.ymusic.app.local_player.loader.PlaylistLoader;
import io.ymusic.app.local_player.model.Song;

public class PlaylistSongHelper {

    public static abstract class OnRemove {
        protected abstract void execute();
    }

    @NonNull
    public static void remove(Context context, int playlistId, Song song, OnRemove onRemove) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.play_queue_remove)
                .setMessage(context.getString(R.string.remove_playlist_x, song.title))
                .setPositiveButton(R.string.play_queue_remove, (dialog, which) -> {
                    new Thread(() -> PlaylistLoader.deletePlaylistSong(context, playlistId, song.id)).start();
                    Toast.makeText(context, "Successfully removed from playlist", Toast.LENGTH_SHORT).show();
                    onRemove.execute();
                })
                .setNegativeButton(R.string.cancel, null)
                .create()
                .show();
    }


    public static void add(Context context, Song song) {
        List<String> addOptions = new ArrayList<>();
        addOptions.add("New Playlist");
        ListView listView = new ListView(context);
        final Dialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle("Add to playlist")
                .setView(listView)
                .create();
        new Thread(() -> {
            List<Playlist> playlists = PlaylistLoader.getAllPlaylist(context);
            for (Playlist playlist : playlists) {
                addOptions.add(playlist.getTitle());
            }
            listView.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, addOptions));
            listView.setOnItemClickListener((adapterView, view, i, l) -> {
                if (i == 0) {
                    dialog.dismiss();
                    View _view = LayoutInflater.from(context).inflate(R.layout.layout_new_playlist, null);
                    EditText editText = _view.findViewById(R.id.playlist_name);
                    new MaterialAlertDialogBuilder(context)
                            .setTitle("New playlist")
                            .setView(_view)
                            .setPositiveButton(R.string.create, (__, which) -> {
                                String playlistName = editText.getText().toString();
                                if (!playlistName.isEmpty()) {
                                    new Thread(() -> {
                                        long playlistId = PlaylistLoader.createPlaylist(context, playlistName);
                                        PlaylistLoader.addPlaylistSong(context, (int) playlistId, song.id);
                                    }).start();
                                    Toast.makeText(context, "Added one song into " + playlistName, Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(context, "Enter playlist name", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .create()
                            .show();
                } else {
                    new Thread(() -> {
                        boolean isAdded = false;
                        List<PlaylistSong> playlistSongs = PlaylistLoader.getAllPlaylistSongs(context, playlists.get(i - 1).getId());
                        for (PlaylistSong playlistSong : playlistSongs) {
                            if (song.id == playlistSong.getSongId()) {
                                isAdded = true;
                                break;
                            }
                        }
                        if (!isAdded) {
                            PlaylistLoader.addPlaylistSong(context, playlists.get(i - 1).getId(), song.id);
                        }
                        boolean finalIsAdded = isAdded;
                        Log.d("TAG", "isAdded " + isAdded);
                        ContextCompat.getMainExecutor(context).execute(() -> {
                            if (finalIsAdded)
                                Toast.makeText(context, "Song already exist in " + addOptions.get(i), Toast.LENGTH_SHORT).show();
                            else
                                Toast.makeText(context, "Added one song into " + addOptions.get(i), Toast.LENGTH_SHORT).show();
                        });
                    }).start();
                    dialog.dismiss();
                }
            });
        }).start();
        dialog.show();
    }

}
