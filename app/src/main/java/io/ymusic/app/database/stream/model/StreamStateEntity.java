package io.ymusic.app.database.stream.model;

import java.util.concurrent.TimeUnit;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;

import static androidx.room.ForeignKey.CASCADE;
import static io.ymusic.app.database.stream.model.StreamStateEntity.JOIN_STREAM_ID;
import static io.ymusic.app.database.stream.model.StreamStateEntity.STREAM_STATE_TABLE;

@Entity(tableName = STREAM_STATE_TABLE,
		primaryKeys = {JOIN_STREAM_ID},
		foreignKeys = {
				@ForeignKey(entity = StreamEntity.class,
						parentColumns = StreamEntity.STREAM_ID,
						childColumns = JOIN_STREAM_ID,
						onDelete = CASCADE, onUpdate = CASCADE)
		})
public class StreamStateEntity {
	
	final public static String STREAM_STATE_TABLE = "stream_state";
	final public static String JOIN_STREAM_ID = "stream_id";
	final public static String STREAM_PROGRESS_TIME = "progress_time";
	
	/**
	 * Playback state will not be saved, if playback time less than this threshold
	 */
	private static final int PLAYBACK_SAVE_THRESHOLD_START_SECONDS = 5;
	/**
	 * Playback state will not be saved, if time left less than this threshold
	 */
	private static final int PLAYBACK_SAVE_THRESHOLD_END_SECONDS = 10;
	
	@ColumnInfo(name = JOIN_STREAM_ID)
	private long streamUid;
	
	@ColumnInfo(name = STREAM_PROGRESS_TIME)
	private long progressTime;
	
	public StreamStateEntity(long streamUid, long progressTime) {
		this.streamUid = streamUid;
		this.progressTime = progressTime;
	}
	
	public long getStreamUid() {
		return streamUid;
	}
	
	public void setStreamUid(long streamUid) {
		this.streamUid = streamUid;
	}
	
	public long getProgressTime() {
		return progressTime;
	}
	
	public void setProgressTime(long progressTime) {
		this.progressTime = progressTime;
	}
	
	public boolean isValid(int durationInSeconds) {
		
		final int seconds = (int) TimeUnit.MILLISECONDS.toSeconds(progressTime);
		return seconds > PLAYBACK_SAVE_THRESHOLD_START_SECONDS && seconds < durationInSeconds - PLAYBACK_SAVE_THRESHOLD_END_SECONDS;
	}
}
