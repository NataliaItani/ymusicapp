package io.ymusic.app.local_player.loader;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AudioColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.ymusic.app.database.playlist.PlaylistSong;
import io.ymusic.app.fragments.songs.SongsFragment;
import io.ymusic.app.local_player.model.Song;
import io.ymusic.app.local_player.provider.BlacklistStore;
import io.ymusic.app.util.MusicUtil;


public class SongLoader {
    protected static final String BASE_SELECTION = AudioColumns.IS_MUSIC + "=1" + " AND " + AudioColumns.TITLE + " != ''";
    protected static final String[] BASE_PROJECTION = new String[]{
            BaseColumns._ID,// 0
            AudioColumns.TITLE,// 1
            AudioColumns.TRACK,// 2
            AudioColumns.YEAR,// 3
            AudioColumns.DURATION,// 4
            AudioColumns.DATA,// 5
            AudioColumns.DATE_MODIFIED,// 6
            AudioColumns.ALBUM_ID,// 7
            AudioColumns.ALBUM,// 8
            AudioColumns.ARTIST_ID,// 9
            AudioColumns.ARTIST,// 10
    };

    @NonNull
    public static List<Song> getAllSongs(@NonNull Context context) {
        Cursor cursor = makeSongCursor(context, null, null);
        return getSongs(cursor);
    }

    @NonNull
    public static List<Song> getAllArtists(@NonNull Context context) {
        Cursor cursor = makeSongCursor(context, AudioColumns.ARTIST + " != 0", null);
        List<Song> songs = getSongs(cursor);
        List<Song> artists = new ArrayList<>();
        songs.stream().collect(Collectors.groupingBy(s -> s.artistId, Collectors.counting()))
                .forEach((key, value) -> {
                    Optional<Song> data = songs.stream().filter(s -> s.artistId == key).
                            findFirst();
                    if (data.isPresent()) {
                        Song song = data.get();
                        // We will use the trackNumber to store the number of songs by the artist
                        song.trackNumber = value.intValue();
                        artists.add(song);
                    }
                });
        return artists;
    }

    @NonNull
    public static List<Song> getAllAlbums(@NonNull Context context) {
        Cursor cursor = makeSongCursor(context, AudioColumns.ALBUM + " != 0", null);
        List<Song> songs = getSongs(cursor);
        List<Song> albums = new ArrayList<>();
        songs.stream().collect(Collectors.groupingBy(s -> s.albumId, Collectors.counting()))
                .forEach((key, value) -> {
                    Optional<Song> data = songs.stream().filter(s -> s.albumId == key).
                            findFirst();
                    if (data.isPresent()) {
                        Song song = data.get();
                        albums.add(song);
                    }
                });
        return albums;
    }

    @NonNull
    public static List<Song> getAllSongsWithId(@NonNull Context context, long id, SongsFragment.SONG_TYPE type) {
        String selection;
        if (type == SongsFragment.SONG_TYPE.ARTIST) {
            selection = AudioColumns.ARTIST_ID + " =?";
        } else {
            selection = AudioColumns.ALBUM_ID + " =?";
        }
        Cursor cursor = makeSongCursor(context, selection, new String[]{""+id});
        return getSongs(cursor);
    }

    @NonNull
    public  static List<Song> getAllPlaylistSongs(@NonNull Context context, int id) {
        List<Song> songs = new ArrayList<>();
        String selection = AudioColumns._ID + " =?";
        List<PlaylistSong> playlistSongs = PlaylistLoader.getAllPlaylistSongs(context, id);
        for (PlaylistSong playlistSong : playlistSongs) {
            String[] selectionArgs = new String[]{""+playlistSong.getSongId()};

            Cursor cursor = makeSongCursor(context, selection, selectionArgs);
            songs.add(getSong(cursor));
        }
        return songs;
    }

    @NonNull
    public static List<Song> getSongs(@NonNull final Context context, final String query) {
        Cursor cursor = makeSongCursor(context, AudioColumns.TITLE + " LIKE ?", new String[]{"%" + query + "%"});
        return getSongs(cursor);
    }

    @NonNull
    public static Song getSong(@NonNull final Context context, final long queryId) {
        Cursor cursor = makeSongCursor(context, AudioColumns._ID + "=?", new String[]{String.valueOf(queryId)});
        return getSong(cursor);
    }

    @NonNull
    public static List<Song> getSongs(@Nullable final Cursor cursor) {
        List<Song> songs = new ArrayList<>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                songs.add(getSongFromCursorImpl(cursor));
            } while (cursor.moveToNext());
        }

        if (cursor != null)
            cursor.close();
        return songs;
    }

    @NonNull
    public static Song getSong(@Nullable Cursor cursor) {
        Song song;
        if (cursor != null && cursor.moveToFirst()) {
            song = getSongFromCursorImpl(cursor);
        } else {
            song = Song.EMPTY_SONG;
        }
        if (cursor != null) {
            cursor.close();
        }
        return song;
    }

    @NonNull
    private static Song getSongFromCursorImpl(@NonNull Cursor cursor) {
        final long id = cursor.getLong(0);
        final String title = cursor.getString(1);
        final int trackNumber = cursor.getInt(2);
        final int year = cursor.getInt(3);
        final long duration = cursor.getLong(4);
        final String data = cursor.getString(5);
        final long dateModified = cursor.getLong(6);
        final long albumId = cursor.getLong(7);
        final String albumName = cursor.getString(8);
        final long artistId = cursor.getLong(9);
        final String artistName = cursor.getString(10);

        return new Song(id, title, trackNumber, year, duration, data, dateModified, albumId, albumName, artistId, artistName);
    }

    @Nullable
    public static Cursor makeSongCursor(@NonNull final Context context, @Nullable final String selection, final String[] selectionValues) {
        return makeSongCursor(context, selection, selectionValues, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
    }

    @Nullable
    public static Cursor makeSongCursor(@NonNull final Context context, @Nullable String selection, String[] selectionValues, final String sortOrder) {
        if (selection != null && !selection.trim().equals("")) {
            selection = BASE_SELECTION + " AND " + selection;
        } else {
            selection = BASE_SELECTION;
        }

        // Blacklist
        List<String> paths = BlacklistStore.getInstance(context).getPaths();
        if (!paths.isEmpty()) {
            selection = generateBlacklistSelection(selection, paths.size());
            selectionValues = addBlacklistSelectionValues(selectionValues, paths);
        }

        try {
            return context.getContentResolver().query(MusicUtil.getCollection(), BASE_PROJECTION, selection, selectionValues, sortOrder);
        } catch (SecurityException e) {
            return null;
        }
    }

    private static String generateBlacklistSelection(String selection, int pathCount) {
        String newSelection = selection != null && !selection.trim().equals("") ? selection + " AND " : "";
        newSelection += AudioColumns.DATA + " NOT LIKE ?";
        for (int i = 0; i < pathCount - 1; i++) {
            newSelection += " AND " + AudioColumns.DATA + " NOT LIKE ?";
        }
        return newSelection;
    }

    private static String[] addBlacklistSelectionValues(String[] selectionValues, List<String> paths) {
        if (selectionValues == null) selectionValues = new String[0];
        String[] newSelectionValues = new String[selectionValues.length + paths.size()];
        System.arraycopy(selectionValues, 0, newSelectionValues, 0, selectionValues.length);
        for (int i = selectionValues.length; i < newSelectionValues.length; i++) {
            newSelectionValues[i] = paths.get(i - selectionValues.length) + "%";
        }
        return newSelectionValues;
    }
}
