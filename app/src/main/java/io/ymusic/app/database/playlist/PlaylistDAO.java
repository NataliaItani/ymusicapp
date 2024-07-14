package io.ymusic.app.database.playlist;

import static androidx.room.OnConflictStrategy.REPLACE;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PlaylistDAO {
    // Insert query
    @Insert(onConflict = REPLACE)
    Long insert(Playlist data);

    // Delete query
    @Delete
    void deletePlaylist(Playlist playlist);

    // Delete query
    @Query("DELETE FROM PlaylistSong WHERE playlistId = :playlistId")
    void deletePlaylistSongs(int playlistId);

    // Update query
    @Update
    void update(Playlist data);

    // Get all data query
    @Query("SELECT * FROM Playlist ORDER BY id ASC")
    List<Playlist> getPlaylists();

    @Query("SELECT count(*) as column_size FROM PlaylistSong WHERE playlistId = :playlistId")
    int getPlaylistSize(int playlistId);

    /* Playlist songs Queries*/
    @Insert(onConflict = REPLACE)
    long insert(PlaylistSong data);

    @Query("DELETE FROM PlaylistSong WHERE playlistId = :playlistId AND songId = :songId")
    void deletePlaylistSong(int playlistId, long songId);

    @Query("SELECT * FROM PlaylistSong WHERE playlistId = :playlistId")
    List<PlaylistSong> getPlaylistSongs(int playlistId);
}
