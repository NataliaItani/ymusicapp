package io.ymusic.app.player.playqueue.events;

public class InitEvent implements PlayQueueEvent {
	
	@Override
	public PlayQueueEventType type() {
		return PlayQueueEventType.INIT;
	}
}
