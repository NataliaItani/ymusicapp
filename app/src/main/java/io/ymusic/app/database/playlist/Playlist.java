package io.ymusic.app.database.playlist;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(tableName = "Playlist")
public class Playlist {
    @PrimaryKey(autoGenerate = true)
    int id;

    @ColumnInfo(name = "title")
    String title;

    @ColumnInfo(name = "size")
    int size;


    public Playlist(int id, String title, int size) {
        this.id = id;
        this.title = title;
        this.size = size;
    }

    @Ignore
    public Playlist(String title, int size) {
        this(0, title, size);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Playlist playlist = (Playlist) o;
        return id == playlist.id && size == playlist.size && title.equals(playlist.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, size);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
