package io.ymusic.app.local_player.loader;

import android.content.Context;

import java.util.List;

import io.ymusic.app.database.SDMusicDatabase;
import io.ymusic.app.database.playlist.Playlist;
import io.ymusic.app.database.playlist.PlaylistDAO;
import io.ymusic.app.database.playlist.PlaylistSong;

public class PlaylistLoader {

    public static List<Playlist> getAllPlaylist(Context context) {
        PlaylistDAO playlistDAO = SDMusicDatabase.getInstance(context.getApplicationContext()).playlistDAO();
        List<Playlist> playlists = playlistDAO.getPlaylists();
        for(Playlist playlist : playlists) {
            playlist.setSize(playlistDAO.getPlaylistSize(playlist.getId()));
        }
        return playlists;
    }

    public static List<PlaylistSong> getAllPlaylistSongs(Context context, int playlistId) {
        PlaylistDAO playlistDAO = SDMusicDatabase.getInstance(context.getApplicationContext()).playlistDAO();
        return playlistDAO.getPlaylistSongs(playlistId);
    }

    public static long createPlaylist(Context context, String playlistName) {
        PlaylistDAO playlistDAO = SDMusicDatabase.getInstance(context.getApplicationContext()).playlistDAO();
        return playlistDAO.insert(new Playlist(playlistName, 0));
    }

    public static void editPlaylist(Context context, Playlist playlist) {
        PlaylistDAO playlistDAO = SDMusicDatabase.getInstance(context.getApplicationContext()).playlistDAO();
        playlistDAO.update(playlist);
    }

    public static void deletePlaylist(Context context, Playlist playlist) {
        PlaylistDAO playlistDAO = SDMusicDatabase.getInstance(context.getApplicationContext()).playlistDAO();
        playlistDAO.deletePlaylist(playlist);
        playlistDAO.deletePlaylistSongs(playlist.getId());

    }

    public static void deletePlaylistSong(Context context, int playlistId, long songId) {
        PlaylistDAO playlistDAO = SDMusicDatabase.getInstance(context.getApplicationContext()).playlistDAO();
        playlistDAO.deletePlaylistSong(playlistId, songId);

    }

    public static void addPlaylistSong(Context context, int playlistId, long songId) {
        PlaylistDAO playlistDAO = SDMusicDatabase.getInstance(context.getApplicationContext()).playlistDAO();
        playlistDAO.insert(new PlaylistSong(playlistId, songId));
    }
}
