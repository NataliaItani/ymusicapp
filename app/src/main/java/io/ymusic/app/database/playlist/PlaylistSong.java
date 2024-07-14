package io.ymusic.app.database.playlist;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "PlaylistSong")
public class PlaylistSong {

    @PrimaryKey(autoGenerate = true)
    int id = 0;

    @ColumnInfo(name = "playlistId")
    int playlistId;

    @ColumnInfo(name = "songId")
    long songId;

    public PlaylistSong(int id, int playlistId, long songId) {
        this.id = id;
        this.playlistId = playlistId;
        this.songId = songId;
    }

    @Ignore
    public PlaylistSong(int playlistId, long songId) {
        this(0, playlistId, songId);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(int playlistId) {
        this.playlistId = playlistId;
    }

    public long getSongId() {
        return songId;
    }

    public void setSongId(long songId) {
        this.songId = songId;
    }
}