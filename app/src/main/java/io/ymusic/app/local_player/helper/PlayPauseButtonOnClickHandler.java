package io.ymusic.app.local_player.helper;

import android.view.View;

public class PlayPauseButtonOnClickHandler implements View.OnClickListener {

    @Override
    public void onClick(View v) {
        if (MusicPlayerRemote.isPlaying()) {
            MusicPlayerRemote.pauseSong();
        } else {
            MusicPlayerRemote.resumePlaying();
        }
    }
}
