package io.ymusic.app.database.history.dao;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Query;

import java.util.List;

import io.ymusic.app.database.history.model.StreamHistoryEntity;
import io.ymusic.app.database.history.model.StreamHistoryEntry;
import io.ymusic.app.database.stream.StreamStatisticsEntry;
import io.ymusic.app.database.stream.model.StreamEntity;
import io.reactivex.Flowable;

@Dao
public abstract class StreamHistoryDAO implements HistoryDAO<StreamHistoryEntity> {
    @Query("SELECT * FROM " + StreamHistoryEntity.STREAM_HISTORY_TABLE +
            " WHERE " + StreamHistoryEntity.STREAM_ACCESS_DATE + " = " +
            "(SELECT MAX(" + StreamHistoryEntity.STREAM_ACCESS_DATE + ") FROM " + StreamHistoryEntity.STREAM_HISTORY_TABLE + ")")
    @Override
    @Nullable
    public abstract StreamHistoryEntity getLatestEntry();

    @Override
    @Query("SELECT * FROM " + StreamHistoryEntity.STREAM_HISTORY_TABLE)
    public abstract Flowable<List<StreamHistoryEntity>> getAll();

    @Override
    @Query("DELETE FROM " + StreamHistoryEntity.STREAM_HISTORY_TABLE)
    public abstract int deleteAll();

    @Override
    public Flowable<List<StreamHistoryEntity>> listByService(int serviceId) {
        throw new UnsupportedOperationException();
    }

    @Query("SELECT * FROM " + StreamEntity.STREAM_TABLE +
            " INNER JOIN " + StreamHistoryEntity.STREAM_HISTORY_TABLE +
            " ON " + StreamEntity.STREAM_ID + " = " + StreamHistoryEntity.JOIN_STREAM_ID +
            " ORDER BY " + StreamHistoryEntity.STREAM_ACCESS_DATE + " DESC")
    public abstract Flowable<List<StreamHistoryEntry>> getHistory();

    @Query("DELETE FROM " + StreamHistoryEntity.STREAM_HISTORY_TABLE + " WHERE " + StreamHistoryEntity.JOIN_STREAM_ID + " = :streamId")
    public abstract int deleteStreamHistory(final long streamId);

    @Query("SELECT * FROM " + StreamEntity.STREAM_TABLE +

            // Select the latest entry and watch count for each stream id on history table
            " INNER JOIN " +
            "(SELECT " + StreamHistoryEntity.JOIN_STREAM_ID + ", " +
            "  MAX(" + StreamHistoryEntity.STREAM_ACCESS_DATE + ") AS " + StreamStatisticsEntry.STREAM_LATEST_DATE +
            " FROM " + StreamHistoryEntity.STREAM_HISTORY_TABLE + " GROUP BY " + StreamHistoryEntity.JOIN_STREAM_ID + ")" +

            " ON " + StreamEntity.STREAM_ID + " = " + StreamHistoryEntity.JOIN_STREAM_ID)
    public abstract Flowable<List<StreamStatisticsEntry>> getStatistics();
}
