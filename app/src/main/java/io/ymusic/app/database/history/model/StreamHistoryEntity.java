package io.ymusic.app.database.history.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

import java.util.Date;

import io.ymusic.app.database.stream.model.StreamEntity;

import static androidx.room.ForeignKey.CASCADE;
import static io.ymusic.app.database.history.model.StreamHistoryEntity.JOIN_STREAM_ID;
import static io.ymusic.app.database.history.model.StreamHistoryEntity.STREAM_ACCESS_DATE;
import static io.ymusic.app.database.history.model.StreamHistoryEntity.STREAM_HISTORY_TABLE;

@Entity(tableName = STREAM_HISTORY_TABLE,
        primaryKeys = {JOIN_STREAM_ID, STREAM_ACCESS_DATE},
        // No need to index for timestamp as they will almost always be unique
        indices = {@Index(value = {JOIN_STREAM_ID})},
        foreignKeys = {
                @ForeignKey(entity = StreamEntity.class,
                        parentColumns = StreamEntity.STREAM_ID,
                        childColumns = JOIN_STREAM_ID,
                        onDelete = CASCADE, onUpdate = CASCADE)
        })
public class StreamHistoryEntity {
    final public static String STREAM_HISTORY_TABLE = "stream_history";
    final public static String JOIN_STREAM_ID = "stream_id";
    final public static String STREAM_ACCESS_DATE = "access_date";

    @ColumnInfo(name = JOIN_STREAM_ID)
    private long streamUid;

    @NonNull
    @ColumnInfo(name = STREAM_ACCESS_DATE)
    private Date accessDate;


    public StreamHistoryEntity(long streamUid, @NonNull Date accessDate) {
        this.streamUid = streamUid;
        this.accessDate = accessDate;
    }

    public long getStreamUid() {
        return streamUid;
    }

    public void setStreamUid(long streamUid) {
        this.streamUid = streamUid;
    }

    public Date getAccessDate() {
        return accessDate;
    }

    public void setAccessDate(@NonNull Date accessDate) {
        this.accessDate = accessDate;
    }
}
