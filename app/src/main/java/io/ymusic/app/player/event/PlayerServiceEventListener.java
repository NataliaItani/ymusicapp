package io.ymusic.app.player.event;

import com.google.android.exoplayer2.ExoPlaybackException;

public interface PlayerServiceEventListener extends PlayerEventListener {
    void onPlayerError(ExoPlaybackException error);
}
