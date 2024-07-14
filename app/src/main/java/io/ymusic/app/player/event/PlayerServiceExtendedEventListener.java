package io.ymusic.app.player.event;

import io.ymusic.app.player.MainPlayer;
import io.ymusic.app.player.VideoPlayerImpl;

public interface PlayerServiceExtendedEventListener extends PlayerServiceEventListener {

    void onServiceConnected(VideoPlayerImpl player, MainPlayer playerService, boolean playAfterConnect);

    void onServiceDisconnected();
}
