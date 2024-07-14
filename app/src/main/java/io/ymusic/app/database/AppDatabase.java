package io.ymusic.app.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import io.ymusic.app.database.history.dao.SearchHistoryDAO;
import io.ymusic.app.database.history.dao.StreamHistoryDAO;
import io.ymusic.app.database.history.model.SearchHistoryEntry;
import io.ymusic.app.database.history.model.StreamHistoryEntity;
import io.ymusic.app.database.playlist.Playlist;
import io.ymusic.app.database.playlist.PlaylistDAO;
import io.ymusic.app.database.playlist.PlaylistSong;
import io.ymusic.app.database.stream.dao.StreamDAO;
import io.ymusic.app.database.stream.dao.StreamStateDAO;
import io.ymusic.app.database.stream.model.StreamEntity;
import io.ymusic.app.database.stream.model.StreamStateEntity;
import io.ymusic.app.database.subscription.SubscriptionDAO;
import io.ymusic.app.database.subscription.SubscriptionEntity;

@TypeConverters({Converters.class})
@Database(
        entities = {
                SearchHistoryEntry.class, StreamEntity.class, StreamHistoryEntity.class, StreamStateEntity.class, SubscriptionEntity.class, Playlist.class, PlaylistSong.class
        },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "sdmusic.db";

    public abstract SearchHistoryDAO searchHistoryDAO();

    public abstract StreamDAO streamDAO();

    public abstract StreamHistoryDAO streamHistoryDAO();

    public abstract StreamStateDAO streamStateDAO();

    public abstract SubscriptionDAO subscriptionDAO();

    public abstract PlaylistDAO playlistDAO();
}
