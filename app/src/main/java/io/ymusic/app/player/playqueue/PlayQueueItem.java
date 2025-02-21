package io.ymusic.app.player.playqueue;

import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamType;

import java.io.Serializable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.ymusic.app.util.ExtractorHelper;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class PlayQueueItem implements Serializable {
	
	public final static long RECOVERY_UNSET = Long.MIN_VALUE;
	private final static String EMPTY_STRING = "";
	
	@NonNull final private String title;
	@NonNull final private String url;
	final private int serviceId;
	final private long duration;
	@NonNull final private String thumbnailUrl;
	@NonNull final private String uploader;
	@NonNull final private String uploaderUrl;
	@NonNull final private StreamType streamType;
	private boolean isAutoQueued;
	
	private long recoveryPosition;
	private Throwable error;
	
	public PlayQueueItem(@NonNull final StreamInfo info) {
		
		this(info.getName(), info.getUrl(), info.getServiceId(), info.getDuration(), info.getThumbnailUrl(), info.getUploaderName(), info.getUploaderUrl(), info.getStreamType());
		
		if (info.getStartPosition() > 0) {
			setRecoveryPosition(info.getStartPosition() * 1000);
		}
	}
	
	public PlayQueueItem(@NonNull final StreamInfoItem item) {
		this(item.getName(), item.getUrl(), item.getServiceId(), item.getDuration(), item.getThumbnailUrl(), item.getUploaderName(), item.getUploaderUrl(), item.getStreamType());
	}

	private PlayQueueItem(@Nullable final String name, @Nullable final String url, final int serviceId, final long duration, @Nullable final String thumbnailUrl, @Nullable final String uploader, @Nullable final String uploaderUrl, @NonNull final StreamType streamType) {

		this.title = name != null ? name : EMPTY_STRING;
		this.url = url != null ? url : EMPTY_STRING;
		this.serviceId = serviceId;
		this.duration = duration;
		this.thumbnailUrl = thumbnailUrl != null ? thumbnailUrl : EMPTY_STRING;
		this.uploader = uploader != null ? uploader : EMPTY_STRING;
		this.uploaderUrl = uploaderUrl != null ? uploaderUrl : EMPTY_STRING;
		this.streamType = streamType;

		this.recoveryPosition = RECOVERY_UNSET;
	}
	
	@NonNull
	public String getTitle() {
		return title;
	}
	
	@NonNull
	public String getUrl() {
		return url;
	}
	
	public int getServiceId() {
		return serviceId;
	}
	
	public long getDuration() {
		return duration;
	}
	
	@NonNull
	public String getThumbnailUrl() {
		return thumbnailUrl;
	}
	
	@NonNull
	public String getUploader() {
		return uploader;
	}

	@NonNull
	public String getUploaderUrl() {
		return uploaderUrl;
	}

	@NonNull
	public StreamType getStreamType() {
		return streamType;
	}
	
	public long getRecoveryPosition() {
		return recoveryPosition;
	}
	
	@Nullable
	public Throwable getError() {
		return error;
	}
	
	@NonNull
	public Single<StreamInfo> getStream() {
		
		return ExtractorHelper.getStreamInfo(this.serviceId, this.url, false)
				.subscribeOn(Schedulers.io())
				.doOnError(throwable -> error = throwable);
	}
	
	public boolean isAutoQueued() {
		return isAutoQueued;
	}
	
	public void setAutoQueued(boolean autoQueued) {
		isAutoQueued = autoQueued;
	}
	
	void setRecoveryPosition(final long recoveryPosition) {
		this.recoveryPosition = recoveryPosition;
	}
}
