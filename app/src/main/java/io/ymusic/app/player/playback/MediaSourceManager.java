package io.ymusic.app.player.playback;

import android.os.Handler;
import android.os.Looper;

import com.google.android.exoplayer2.source.MediaSource;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;
import io.ymusic.app.player.mediasource.FailedMediaSource;
import io.ymusic.app.player.mediasource.LoadedMediaSource;
import io.ymusic.app.player.mediasource.ManagedMediaSource;
import io.ymusic.app.player.mediasource.ManagedMediaSourcePlaylist;
import io.ymusic.app.player.mediasource.PlaceholderMediaSource;
import io.ymusic.app.player.playqueue.PlayQueue;
import io.ymusic.app.player.playqueue.PlayQueueItem;
import io.ymusic.app.player.playqueue.events.MoveEvent;
import io.ymusic.app.player.playqueue.events.PlayQueueEvent;
import io.ymusic.app.player.playqueue.events.RemoveEvent;
import io.ymusic.app.player.playqueue.events.ReorderEvent;
import io.ymusic.app.util.ServiceHelper;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.subscriptions.EmptySubscription;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class MediaSourceManager {
	
	/**
	 * Determines how many streams before and after the current stream should be loaded.
	 * The default value (1) ensures seamless playback under typical network settings.
	 * <br><br>
	 * The streams after the current will be loaded into the playlist timeline while the
	 * streams before will only be cached for future usage.
	 *
	 * @see #onMediaSourceReceived(PlayQueueItem, ManagedMediaSource)
	 */
	private final static int WINDOW_SIZE = 1;
	
	@NonNull private final PlaybackListener playbackListener;
	@NonNull private final PlayQueue playQueue;
	
	/**
	 * Determines the gap time between the playback position and the playback duration which
	 * the {@link #getEdgeIntervalSignal()} begins to request loading.
	 *
	 * @see #progressUpdateIntervalMillis
	 */
	private final long playbackNearEndGapMillis;
	/**
	 * Determines the interval which the {@link #getEdgeIntervalSignal()} waits for between
	 * each request for loading, once {@link #playbackNearEndGapMillis} has reached.
	 */
	private final long progressUpdateIntervalMillis;
	@NonNull private final Observable<Long> nearEndIntervalSignal;
	
	/**
	 * Process only the last load order when receiving a stream of load orders (lessens I/O).
	 * <br><br>
	 * The higher it is, the less loading occurs during rapid noncritical timeline changes.
	 * <br><br>
	 * Not recommended to go below 100ms.
	 *
	 * @see #loadDebounced()
	 */
	private final long loadDebounceMillis;
	@NonNull private final Disposable debouncedLoader;
	@NonNull private final PublishSubject<Long> debouncedSignal;
	@NonNull private Subscription playQueueReactor;
	
	/**
	 * Determines the maximum number of disposables allowed in the {@link #loaderReactor}.
	 * Once exceeded, new calls to {@link #loadImmediate()} will evict all disposables in the
	 * {@link #loaderReactor} in order to load a new set of items.
	 *
	 * @see #loadImmediate()
	 * @see #maybeLoadItem(PlayQueueItem)
	 */
	private final static int MAXIMUM_LOADER_SIZE = WINDOW_SIZE * 2 + 1;
	@NonNull private final CompositeDisposable loaderReactor;
	@NonNull private final Set<PlayQueueItem> loadingItems;
	@NonNull private final AtomicBoolean isBlocked;
	@NonNull private ManagedMediaSourcePlaylist playlist;
	
	private Handler removeMediaSourceHandler = new Handler(Looper.getMainLooper());
	
	public MediaSourceManager(@NonNull final PlaybackListener listener, @NonNull final PlayQueue playQueue) {
		
		this(listener, playQueue, 400L, TimeUnit.MILLISECONDS.convert(3, TimeUnit.SECONDS), TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS));
	}
	
	private MediaSourceManager(@NonNull final PlaybackListener listener, @NonNull final PlayQueue playQueue, final long loadDebounceMillis, final long playbackNearEndGapMillis, final long progressUpdateIntervalMillis) {
		
		if (playQueue.getBroadcastReceiver() == null) {
			throw new IllegalArgumentException("Play Queue has not been initialized.");
		}
		if (playbackNearEndGapMillis < progressUpdateIntervalMillis) {
			throw new IllegalArgumentException("Playback end gap=[" + playbackNearEndGapMillis +
													   " ms] must be longer than update interval=[ " + progressUpdateIntervalMillis +
													   " ms] for them to be useful.");
		}
		
		this.playbackListener = listener;
		this.playQueue = playQueue;
		
		this.playbackNearEndGapMillis = playbackNearEndGapMillis;
		this.progressUpdateIntervalMillis = progressUpdateIntervalMillis;
		this.nearEndIntervalSignal = getEdgeIntervalSignal();
		
		this.loadDebounceMillis = loadDebounceMillis;
		this.debouncedSignal = PublishSubject.create();
		this.debouncedLoader = getDebouncedLoader();
		
		this.playQueueReactor = EmptySubscription.INSTANCE;
		this.loaderReactor = new CompositeDisposable();
		
		this.isBlocked = new AtomicBoolean(false);
		
		this.playlist = new ManagedMediaSourcePlaylist();
		
		this.loadingItems = Collections.synchronizedSet(new ArraySet<>());
		
		playQueue.getBroadcastReceiver()
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(getReactor());
	}
	
	/**
	 * Dispose the manager and releases all message buses and loaders.
	 */
	public void dispose() {
		
		debouncedSignal.onComplete();
		debouncedLoader.dispose();
		
		playQueueReactor.cancel();
		loaderReactor.dispose();
	}
	
	private Subscriber<PlayQueueEvent> getReactor() {
		
		return new Subscriber<PlayQueueEvent>() {
			
			@Override
			public void onSubscribe(@NonNull Subscription d) {
				
				playQueueReactor.cancel();
				playQueueReactor = d;
				playQueueReactor.request(1);
			}
			
			@Override
			public void onNext(@NonNull PlayQueueEvent playQueueMessage) {
				onPlayQueueChanged(playQueueMessage);
			}
			
			@Override
			public void onError(@NonNull Throwable e) {
				// unimplemented
			}
			
			@Override
			public void onComplete() {
				// unimplemented
			}
		};
	}
	
	private void onPlayQueueChanged(final PlayQueueEvent event) {
		
		if (playQueue.isEmpty() && playQueue.isComplete()) {
			playbackListener.onPlaybackShutdown();
			return;
		}
		
		// Event specific action
		switch (event.type()) {
			case INIT:
			case ERROR:
				maybeBlock();
			case APPEND:
				populateSources();
				break;
			case SELECT:
				maybeRenewCurrentIndex();
				break;
			case REMOVE:
				final RemoveEvent removeEvent = (RemoveEvent) event;
				playlist.remove(removeEvent.getRemoveIndex());
				break;
			case MOVE:
				final MoveEvent moveEvent = (MoveEvent) event;
				playlist.move(moveEvent.getFromIndex(), moveEvent.getToIndex());
				break;
			case REORDER:
				// Need to move to ensure the playing index from play queue matches that of
				// the source timeline, and then window correction can take care of the rest
				final ReorderEvent reorderEvent = (ReorderEvent) event;
				playlist.move(reorderEvent.getFromSelectedIndex(), reorderEvent.getToSelectedIndex());
				break;
			case RECOVERY:
			default:
				break;
		}
		
		// Loading and Syncing
		switch (event.type()) {
			case INIT:
			case REORDER:
			case ERROR:
			case SELECT:
				loadImmediate(); // low frequency, critical events
				break;
			case APPEND:
			case REMOVE:
			case MOVE:
			case RECOVERY:
			default:
				loadDebounced(); // high frequency or noncritical events
				break;
		}
		
		if (!isPlayQueueReady()) {
			maybeBlock();
			playQueue.fetch();
		}
		playQueueReactor.request(1);
	}
	
	// Playback Locking
	private boolean isPlayQueueReady() {
		
		final boolean isWindowLoaded = playQueue.size() - playQueue.getIndex() > WINDOW_SIZE;
		return playQueue.isComplete() || isWindowLoaded;
	}
	
	private boolean isPlaybackReady() {
		
		if (playlist.size() != playQueue.size()) return false;
		
		final ManagedMediaSource mediaSource = playlist.get(playQueue.getIndex());
		if (mediaSource == null) return false;
		
		final PlayQueueItem playQueueItem = playQueue.getItem();
		return mediaSource.isStreamEqual(playQueueItem);
	}
	
	private void maybeBlock() {
		
		if (isBlocked.get()) return;
		
		playbackListener.onPlaybackBlock();
		resetSources();
		
		isBlocked.set(true);
	}
	
	private void maybeUnblock() {
		
		if (isBlocked.get()) {
			isBlocked.set(false);
			playbackListener.onPlaybackUnblock(playlist.getParentMediaSource());
		}
	}
	
	// Metadata Synchronization
	private void maybeSync() {
		
		final PlayQueueItem currentItem = playQueue.getItem();
		if (isBlocked.get() || currentItem == null) return;
		
		playbackListener.onPlaybackSynchronize(currentItem);
	}
	
	private synchronized void maybeSynchronizePlayer() {
		
		if (isPlayQueueReady() && isPlaybackReady()) {
			maybeUnblock();
			maybeSync();
		}
	}
	
	// MediaSource Loading
	private Observable<Long> getEdgeIntervalSignal() {
		
		return Observable.interval(progressUpdateIntervalMillis, TimeUnit.MILLISECONDS)
				.observeOn(AndroidSchedulers.mainThread())
				.filter(ignored -> playbackListener.isApproachingPlaybackEdge(playbackNearEndGapMillis))
				// ignored error
				.onErrorReturn(throwable -> 0L);
	}
	
	private Disposable getDebouncedLoader() {
		
		return debouncedSignal.mergeWith(nearEndIntervalSignal)
				.debounce(loadDebounceMillis, TimeUnit.MILLISECONDS)
				.subscribeOn(Schedulers.single())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(timestamp -> loadImmediate());
	}
	
	private void loadDebounced() {
		debouncedSignal.onNext(System.currentTimeMillis());
	}
	
	private void loadImmediate() {
		
		final ItemsToLoad itemsToLoad = getItemsToLoad(playQueue);
		if (itemsToLoad == null) return;
		
		// Evict the previous items being loaded to free up memory, before start loading new ones
		maybeClearLoaders();
		
		maybeLoadItem(itemsToLoad.center);
		for (final PlayQueueItem item : itemsToLoad.neighbors) {
			maybeLoadItem(item);
		}
	}
	
	private void maybeLoadItem(@NonNull final PlayQueueItem item) {
		
		if (playQueue.indexOf(item) >= playlist.size()) return;
		
		if (!loadingItems.contains(item) && isCorrectionNeeded(item)) {
			
			loadingItems.add(item);
			final Disposable loader = getLoadedMediaSource(item)
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(mediaSource -> onMediaSourceReceived(item, mediaSource));
			loaderReactor.add(loader);
		}
	}
	
	private Single<ManagedMediaSource> getLoadedMediaSource(@NonNull final PlayQueueItem stream) {
		
		return stream.getStream().map(streamInfo -> {
			
			final MediaSource source = playbackListener.sourceOf(stream, streamInfo);
			if (source == null) {
				final String message = "Unable to resolve source from stream info." +
						" URL: " + stream.getUrl() +
						", audio count: " + streamInfo.getAudioStreams().size() +
						", video count: " + streamInfo.getVideoOnlyStreams().size() +
						streamInfo.getVideoStreams().size();
				return new FailedMediaSource(stream, new FailedMediaSource.MediaSourceResolutionException(message));
			}
			
			final long expiration = System.currentTimeMillis() + ServiceHelper.getCacheExpirationMillis();
			return new LoadedMediaSource(source, stream, expiration);
		}).onErrorReturn(throwable -> new FailedMediaSource(stream, new FailedMediaSource.StreamInfoLoadException(throwable)));
	}
	
	private void onMediaSourceReceived(@NonNull final PlayQueueItem item, @NonNull final ManagedMediaSource mediaSource) {
		
		loadingItems.remove(item);
		
		final int itemIndex = playQueue.indexOf(item);
		// Only update the playlist timeline for items at the current index or after.
		if (isCorrectionNeeded(item)) {
			playlist.update(itemIndex, mediaSource, removeMediaSourceHandler, this::maybeSynchronizePlayer);
		}
	}
	
	/**
	 * Checks if the corresponding MediaSource in
	 * {@link com.google.android.exoplayer2.source.ConcatenatingMediaSource}
	 * for a given {@link PlayQueueItem} needs replacement, either due to gapless playback
	 * readiness or playlist desynchronization.
	 * <br><br>
	 * If the given {@link PlayQueueItem} is currently being played and is already loaded,
	 * then correction is not only needed if the playlist is desynchronized. Otherwise, the
	 * check depends on the status (e.g. expiration or placeholder) of the
	 * {@link ManagedMediaSource}.
	 */
	private boolean isCorrectionNeeded(@NonNull final PlayQueueItem item) {
		
		final int index = playQueue.indexOf(item);
		final ManagedMediaSource mediaSource = playlist.get(index);
		return mediaSource != null && mediaSource.shouldBeReplacedWith(item, index != playQueue.getIndex());
	}
	
	/**
	 * Checks if the current playing index contains an expired {@link ManagedMediaSource}.
	 * If so, the expired source is replaced by a {@link PlaceholderMediaSource} and
	 * {@link #loadImmediate()} is called to reload the current item.
	 * <br><br>
	 * If not, then the media source at the current index is ready for playback, and
	 * {@link #maybeSynchronizePlayer()} is called.
	 * <br><br>
	 * Under both cases, {@link #maybeSync()} will be called to ensure the listener
	 * is up-to-date.
	 */
	private void maybeRenewCurrentIndex() {
		
		final int currentIndex = playQueue.getIndex();
		final ManagedMediaSource currentSource = playlist.get(currentIndex);
		if (currentSource == null) return;
		
		final PlayQueueItem currentItem = playQueue.getItem();
		if (!currentSource.shouldBeReplacedWith(currentItem, true)) {
			maybeSynchronizePlayer();
			return;
		}
		
		playlist.invalidate(currentIndex, removeMediaSourceHandler, this::loadImmediate);
	}
	
	private void maybeClearLoaders() {
		
		if (!loadingItems.contains(playQueue.getItem()) && loaderReactor.size() > MAXIMUM_LOADER_SIZE) {
			loaderReactor.clear();
			loadingItems.clear();
		}
	}
	
	// MediaSource Playlist Helpers
	private void resetSources() {
		playlist = new ManagedMediaSourcePlaylist();
	}
	
	private void populateSources() {
		
		while (playlist.size() < playQueue.size()) {
			playlist.expand();
		}
	}
	
	// Manager Helpers
	@Nullable
	private static ItemsToLoad getItemsToLoad(@NonNull final PlayQueue playQueue) {
		
		// The current item has higher priority
		final int currentIndex = playQueue.getIndex();
		final PlayQueueItem currentItem = playQueue.getItem(currentIndex);
		if (currentItem == null) return null;
		
		// The rest are just for seamless playback
		// Although timeline is not updated prior to the current index, these sources are still
		// loaded into the cache for faster retrieval at a potentially later time.
		final int leftBound = Math.max(0, currentIndex - MediaSourceManager.WINDOW_SIZE);
		final int rightLimit = currentIndex + MediaSourceManager.WINDOW_SIZE + 1;
		final int rightBound = Math.min(playQueue.size(), rightLimit);
		final Set<PlayQueueItem> neighbors = new ArraySet<>(playQueue.getStreams().subList(leftBound, rightBound));
		
		// Do a round robin
		final int excess = rightLimit - playQueue.size();
		if (excess >= 0) {
			neighbors.addAll(playQueue.getStreams().subList(0, Math.min(playQueue.size(), excess)));
		}
		neighbors.remove(currentItem);
		
		return new ItemsToLoad(currentItem, neighbors);
	}
	
	private static class ItemsToLoad {
		
		@NonNull final private PlayQueueItem center;
		@NonNull final private Collection<PlayQueueItem> neighbors;
		
		ItemsToLoad(@NonNull final PlayQueueItem center, @NonNull final Collection<PlayQueueItem> neighbors) {
			
			this.center = center;
			this.neighbors = neighbors;
		}
	}
}
