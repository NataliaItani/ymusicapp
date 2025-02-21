package io.ymusic.app.player.playqueue;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.ymusic.app.player.playqueue.events.AppendEvent;
import io.ymusic.app.player.playqueue.events.ErrorEvent;
import io.ymusic.app.player.playqueue.events.InitEvent;
import io.ymusic.app.player.playqueue.events.MoveEvent;
import io.ymusic.app.player.playqueue.events.PlayQueueEvent;
import io.ymusic.app.player.playqueue.events.RecoveryEvent;
import io.ymusic.app.player.playqueue.events.RemoveEvent;
import io.ymusic.app.player.playqueue.events.ReorderEvent;
import io.ymusic.app.player.playqueue.events.SelectEvent;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;

/**
 * PlayQueue is responsible for keeping track of a list of streams and the index of
 * the stream that should be currently playing.
 * <p>
 * This class contains basic manipulation of a playlist while also functions as a
 * message bus, providing all listeners with new updates to the play queue.
 * <p>
 * This class can be serialized for passing intents, but in order to start the
 * message bus, it must be initialized.
 */
public abstract class PlayQueue implements Serializable {
	
	private ArrayList<PlayQueueItem> backup;
	private ArrayList<PlayQueueItem> streams;
	@NonNull
	private final AtomicInteger queueIndex;
	private final ArrayList<PlayQueueItem> history;
	
	private transient BehaviorSubject<PlayQueueEvent> eventBroadcast;
	private transient Flowable<PlayQueueEvent> broadcastReceiver;
	private transient Subscription reportingReactor;

	private transient boolean disposed;

	PlayQueue(final int index, final List<PlayQueueItem> startWith) {
		
		streams = new ArrayList<>();
		streams.addAll(startWith);
		history = new ArrayList<>();
		if (streams.size() > index) {
			history.add(streams.get(index));
		}
		queueIndex = new AtomicInteger(index);
		disposed = false;
	}
	
	// Playlist actions
	
	/**
	 * Initializes the play queue message buses.
	 * <p>
	 * Also starts a self reporter for logging if debug mode is enabled.
	 */
	public void init() {
		eventBroadcast = BehaviorSubject.create();
		broadcastReceiver = eventBroadcast.toFlowable(BackpressureStrategy.BUFFER)
				.observeOn(AndroidSchedulers.mainThread())
				.startWith(new InitEvent());
	}
	
	/**
	 * Dispose the play queue by stopping all message buses.
	 */
	public void dispose() {
		
		if (eventBroadcast != null) eventBroadcast.onComplete();
		if (reportingReactor != null) reportingReactor.cancel();
		
		eventBroadcast = null;
		broadcastReceiver = null;
		reportingReactor = null;
		disposed = true;
	}
	
	/**
	 * Checks if the queue is complete.
	 * <p>
	 * A queue is complete if it has loaded all items in an external playlist
	 * single stream or local queues are always complete.
	 */
	public abstract boolean isComplete();
	
	/**
	 * Load partial queue in the background, does nothing if the queue is complete.
	 */
	public abstract void fetch();
	
	// Readonly ops
	
	/**
	 * Returns the current index that should be played.
	 */
	public int getIndex() {
		return queueIndex.get();
	}

	/**
	 * Returns the current item that should be played.
	 */
	public PlayQueueItem getItem() {
		return getItem(getIndex());
	}
	
	public PlayQueueItem getItem(int index) {
		if (index < 0 || index >= streams.size() || streams.get(index) == null) return null;
		return streams.get(index);
	}
	
	/**
	 * Returns the index of the given item using referential equality.
	 * May be null despite play queue contains identical item.
	 */
	public int indexOf(@NonNull final PlayQueueItem item) {
		// referential equality, can't think of a better way to do this
		return streams.indexOf(item);
	}
	
	/**
	 * Returns the current size of play queue.
	 */
	public int size() {
		return streams.size();
	}
	
	/**
	 * Checks if the play queue is empty.
	 */
	public boolean isEmpty() {
		return streams.isEmpty();
	}
	
	/**
	 * Determines if the current play queue is shuffled.
	 */
	public boolean isShuffled() {
		return backup != null;
	}
	
	/**
	 * Returns an immutable view of the play queue.
	 */
	@NonNull
	public List<PlayQueueItem> getStreams() {
		return Collections.unmodifiableList(streams);
	}
	
	/**
	 * Returns the play queue's update broadcast.
	 * May be null if the play queue message bus is not initialized.
	 */
	@Nullable
	public Flowable<PlayQueueEvent> getBroadcastReceiver() {
		return broadcastReceiver;
	}
	
	// Write ops

	/**
	 * Changes the current playing index to a new index.
	 * <p>
	 * This method is guarded using in a circular manner for index exceeding the play queue size.
	 * </p>
	 * <p>
	 * Will emit a {@link SelectEvent} if the index is not the current playing index.
	 * </p>
	 *
	 * @param index the index to be set
	 */
	public synchronized void setIndex(final int index) {
		final int oldIndex = getIndex();

		int newIndex = index;
		if (index < 0) {
			newIndex = 0;
		}
		if (index >= streams.size()) {
			newIndex = isComplete() ? index % streams.size() : streams.size() - 1;
		}
		if (oldIndex != newIndex) {
			history.add(streams.get(newIndex));
		}

		queueIndex.set(newIndex);
		broadcast(new SelectEvent(oldIndex, newIndex));
	}
	
	/**
	 * Changes the current playing index by an offset amount.
	 * <p>
	 * Will emit a {@link SelectEvent} if offset is non-zero.
	 */
	public synchronized void offsetIndex(final int offset) {
		setIndex(getIndex() + offset);
	}
	
	/**
	 * Appends the given {@link PlayQueueItem}s to the current play queue.
	 *
	 * @see #append(List items)
	 */
	public synchronized void append(@NonNull final PlayQueueItem... items) {
		append(Arrays.asList(items));
	}
	
	/**
	 * Appends the given {@link PlayQueueItem}s to the current play queue.
	 * <p>
	 * If the play queue is shuffled, then append the items to the backup queue as is and
	 * append the shuffle items to the play queue.
	 * <p>
	 * Will emit a {@link AppendEvent} on any given context.
	 */
	public synchronized void append(@NonNull final List<PlayQueueItem> items) {
		
		List<PlayQueueItem> itemList = new ArrayList<>(items);
		
		if (isShuffled()) {
			backup.addAll(itemList);
			Collections.shuffle(itemList);
		}
		
		if (!streams.isEmpty() && streams.get(streams.size() - 1).isAutoQueued() && !itemList.get(0).isAutoQueued()) {
			streams.remove(streams.size() - 1);
		}
		
		streams.addAll(itemList);
		broadcast(new AppendEvent(itemList.size()));
	}
	
	/**
	 * Removes the item at the given index from the play queue.
	 * <p>
	 * The current playing index will decrement if it is greater than the index being removed.
	 * On cases where the current playing index exceeds the playlist range, it is set to 0.
	 * <p>
	 * Will emit a {@link RemoveEvent} if the index is within the play queue index range.
	 */
	public synchronized void remove(final int index) {
		
		if (index >= streams.size() || index < 0) return;
		removeInternal(index);
		broadcast(new RemoveEvent(index, getIndex()));
	}
	
	/**
	 * Report an exception for the item at the current index in order and skip to the next one
	 * <p>
	 * This is done as a separate event as the underlying manager may have
	 * different implementation regarding exceptions.
	 * </p>
	 */
	public synchronized void error() {
		final int oldIndex = getIndex();
		queueIndex.incrementAndGet();
		if (streams.size() > queueIndex.get()) {
			history.add(streams.get(queueIndex.get()));
		}
		broadcast(new ErrorEvent(oldIndex, getIndex()));
	}
	
	private synchronized void removeInternal(final int removeIndex) {
		
		final int currentIndex = queueIndex.get();
		final int size = size();
		
		if (currentIndex > removeIndex) {
			queueIndex.decrementAndGet();
			
		}
		else if (currentIndex >= size) {
			queueIndex.set(currentIndex % (size - 1));
			
		}
		else if (currentIndex == removeIndex && currentIndex == size - 1) {
			queueIndex.set(0);
		}
		
		if (backup != null) {
			backup.remove(getItem(removeIndex));
		}

		history.remove(streams.remove(removeIndex));
		if (streams.size() > queueIndex.get()) {
			history.add(streams.get(queueIndex.get()));
		}
	}
	
	/**
	 * Moves a queue item at the source index to the target index.
	 * <p>
	 * If the item being moved is the currently playing, then the current playing index is set
	 * to that of the target.
	 * If the moved item is not the currently playing and moves to an index <b>AFTER</b> the
	 * current playing index, then the current playing index is decremented.
	 * Vice versa if the an item after the currently playing is moved <b>BEFORE</b>.
	 */
	public synchronized void move(final int source, final int target) {
		
		if (source < 0 || target < 0) return;
		if (source >= streams.size() || target >= streams.size()) return;
		
		final int current = getIndex();
		if (source == current) {
			queueIndex.set(target);
		}
		else if (source < current && target >= current) {
			queueIndex.decrementAndGet();
		}
		else if (source > current && target <= current) {
			queueIndex.incrementAndGet();
		}
		
		PlayQueueItem playQueueItem = streams.remove(source);
		playQueueItem.setAutoQueued(false);
		streams.add(target, playQueueItem);
		broadcast(new MoveEvent(source, target));
	}
	
	/**
	 * Sets the recovery record of the item at the index.
	 * <p>
	 * Broadcasts a recovery event.
	 */
	public synchronized void setRecovery(final int index, final long position) {
		
		if (index < 0 || index >= streams.size()) return;
		
		streams.get(index).setRecoveryPosition(position);
		broadcast(new RecoveryEvent(index, position));
	}
	
	/**
	 * Revoke the recovery record of the item at the index.
	 * <p>
	 * Broadcasts a recovery event.
	 */
	public synchronized void unsetRecovery(final int index) {
		setRecovery(index, PlayQueueItem.RECOVERY_UNSET);
	}
	
	/**
	 * Shuffles the current play queue.
	 * <p>
	 * This method first backs up the existing play queue and item being played.
	 * Then a newly shuffled play queue will be generated along with currently
	 * playing item placed at the beginning of the queue.
	 * <p>
	 * Will emit a {@link ReorderEvent} in any context.
	 */
	public synchronized void shuffle() {
		
		if (backup == null) {
			backup = new ArrayList<>(streams);
		}
		final int originIndex = getIndex();
		final PlayQueueItem current = getItem();
		Collections.shuffle(streams);
		
		final int newIndex = streams.indexOf(current);
		if (newIndex != -1) {
			streams.add(0, streams.remove(newIndex));
		}
		queueIndex.set(0);
		if (streams.size() > 0) {
			history.add(streams.get(0));
		}
		
		broadcast(new ReorderEvent(originIndex, queueIndex.get()));
	}
	
	/**
	 * Unshuffles the current play queue if a backup play queue exists.
	 * <p>
	 * This method undoes shuffling and index will be set to the previously playing item if found,
	 * otherwise, the index will reset to 0.
	 * <p>
	 * Will emit a {@link ReorderEvent} if a backup exists.
	 */
	public synchronized void unshuffle() {
		
		if (backup == null) return;
		final int originIndex = getIndex();
		final PlayQueueItem current = getItem();
		
		streams.clear();
		streams = backup;
		backup = null;
		
		final int newIndex = streams.indexOf(current);
		if (newIndex != -1) {
			queueIndex.set(newIndex);
		}
		else {
			queueIndex.set(0);
		}
		if (streams.size() > queueIndex.get()) {
			history.add(streams.get(queueIndex.get()));
		}
		
		broadcast(new ReorderEvent(originIndex, queueIndex.get()));
	}

	/**
	 * Selects previous played item.
	 *
	 * This method removes currently playing item from history and
	 * starts playing the last item from history if it exists
	 *
	 * @return true if history is not empty and the item can be played
	 * */
	public synchronized boolean previous() {
		if (history.size() <= 1) {
			return false;
		}

		history.remove(history.size() - 1);

		final PlayQueueItem last = history.remove(history.size() - 1);
		setIndex(indexOf(last));

		return true;
	}

	/*
	 * Compares two PlayQueues. Useful when a user switches players but queue is the same so
	 * we don't have to do anything with new queue.
	 * This method also gives a chance to track history of items in a queue in
	 * VideoDetailFragment without duplicating items from two identical queues
	 * */
	@Override
	public boolean equals(@Nullable final Object obj) {
		if (!(obj instanceof PlayQueue)
				|| getStreams().size() != ((PlayQueue) obj).getStreams().size()) {
			return false;
		}

		final PlayQueue other = (PlayQueue) obj;
		if (size() != other.size()) {
			return false;
		}
		for (int i = 0; i < size(); i++) {
			final PlayQueueItem stream = streams.get(i);
			final PlayQueueItem otherStream = other.streams.get(i);
			// Check is based on serviceId and URL
			if (stream.getServiceId() != otherStream.getServiceId() || !stream.getUrl().equals(otherStream.getUrl())) {
				return false;
			}
		}
		return true;
	}

	public boolean isDisposed() {
		return disposed;
	}

	// Rx Broadcast
	private void broadcast(@NonNull final PlayQueueEvent event) {
		
		if (eventBroadcast != null) {
			eventBroadcast.onNext(event);
		}
	}

	private Subscriber<PlayQueueEvent> getSelfReporter() {
		return new Subscriber<PlayQueueEvent>() {

			@Override
			public void onSubscribe(final Subscription s) {
				if (reportingReactor != null) {
					reportingReactor.cancel();
				}
				reportingReactor = s;
				reportingReactor.request(1);
			}

			@Override
			public void onNext(final PlayQueueEvent event) {
				reportingReactor.request(1);
			}

			@Override
			public void onError(final Throwable t) {
			}

			@Override
			public void onComplete() {
			}
		};
	}
}

