package io.ymusic.app.player.playqueue.events;

import java.io.Serializable;

public interface PlayQueueEvent extends Serializable {
    PlayQueueEventType type();
}
