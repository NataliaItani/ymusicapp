package io.ymusic.app.util;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import io.ymusic.app.R;
import io.ymusic.app.local_player.helper.MusicPlayerRemote;
import io.ymusic.app.local_player.loader.SongLoader;
import io.ymusic.app.local_player.model.Song;

public class MusicUtil {

    public static Uri getMediaStoreAlbumCoverUri(long albumId) {
        final Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
        return ContentUris.withAppendedId(sArtworkUri, albumId);
    }

    public static Uri getSongFileUri(long songId) {
        return ContentUris.withAppendedId(MusicUtil.getCollection(), songId);
    }

    @NonNull
    public static String getSongInfoString(@NonNull final Song song) {
        return song.artistName;
    }

    public static String getReadableDurationString(long songDurationMillis) {
        long minutes = (songDurationMillis / 1000) / 60;
        long seconds = (songDurationMillis / 1000) % 60;
        if (minutes < 60) {
            return String.format(Locale.getDefault(), "%01d:%02d", minutes, seconds);
        } else {
            long hours = minutes / 60;
            minutes = minutes % 60;
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        }
    }

    /**
     * Build a concatenated string from the provided arguments
     * The intended purpose is to show extra annotations
     * to a music library item.
     * Ex: for a given album --> buildInfoString(album.artist, album.songCount)
     */
    @NonNull
    public static String buildInfoString(@Nullable final String string1, @Nullable final String string2) {
        // Skip empty strings
        if (TextUtils.isEmpty(string1)) {
            return TextUtils.isEmpty(string2) ? "" : string2;
        }
        if (TextUtils.isEmpty(string2)) {
            return TextUtils.isEmpty(string1) ? "" : string1;
        }

        return string1 + "  •  " + string2;
    }

    @NonNull
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static File createAlbumArtDir() {
        File albumArtDir = new File(Environment.getExternalStorageDirectory(), "/albumthumbs/");
        if (!albumArtDir.exists()) {
            albumArtDir.mkdirs();
            try {
                new File(albumArtDir, ".nomedia").createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return albumArtDir;
    }

    public static void deleteTracks(@NonNull final Context context, @NonNull final List<Song> songs) {
        final String[] projection = new String[]{
                BaseColumns._ID, MediaStore.MediaColumns.DATA
        };
        final StringBuilder selection = new StringBuilder();
        selection.append(BaseColumns._ID + " IN (");
        for (int i = 0; i < songs.size(); i++) {
            selection.append(songs.get(i).id);
            if (i < songs.size() - 1) {
                selection.append(",");
            }
        }
        selection.append(")");

        int deletedCount = 0;

        try {
            final Cursor cursor = context.getContentResolver().query(MusicUtil.getCollection(), projection, selection.toString(),
                    null, null);
            if (cursor != null) {
                // Step 1: Remove selected tracks from the current playlist, as well
                // as from the album art cache
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    final long id = cursor.getLong(0);
                    final Song song = SongLoader.getSong(context, id);
                    MusicPlayerRemote.removeFromQueue(song);
                    cursor.moveToNext();
                }

                // Step 2: Remove files from card
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    final long id = cursor.getLong(0);
                    final String name = cursor.getString(1);
                    try { // File.delete can throw a security exception
                        final File f = new File(name);
                        if (f.delete()) {
                            // Step 3: Remove selected track from the database
                            context.getContentResolver().delete(ContentUris.withAppendedId(MusicUtil.getCollection(), id), null, null);
                            deletedCount++;
                        } else {
                            // I'm not sure if we'd ever get here (deletion would
                            // have to fail, but no exception thrown)
                            Log.e("MusicUtils", "Failed to delete file " + name);
                        }

                        cursor.moveToNext();
                    } catch (@NonNull final SecurityException ex) {
                        Toast.makeText(context, "Failed to find file: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                        cursor.moveToNext();
                    } catch (NullPointerException e) {
                        Log.e("MusicUtils", "Failed to find file " + name);
                        Toast.makeText(context, "Failed to find file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
                cursor.close();
            }
            if (deletedCount > 0) {
                Toast.makeText(context, context.getString(R.string.deleted_x_songs, deletedCount), Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException ignored) {
        }
    }

    public static Uri getCollection() {
        return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    }
}
