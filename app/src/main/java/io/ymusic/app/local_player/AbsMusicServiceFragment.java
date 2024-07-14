package io.ymusic.app.local_player;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;

import io.ymusic.app.base.BaseFragment;
import io.ymusic.app.local_player.base.AbsMusicServiceActivity;
import io.ymusic.app.local_player.interfaces.MusicServiceEventListener;

public class AbsMusicServiceFragment extends BaseFragment implements MusicServiceEventListener {

    public AbsMusicServiceActivity activity;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            activity = (AbsMusicServiceActivity) context;
        } catch (ClassCastException e) {
            throw new RuntimeException(context.getClass().getSimpleName() + " must be an instance of " + AbsMusicServiceActivity.class.getSimpleName());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        activity = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activity.addMusicServiceEventListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        activity.removeMusicServiceEventListener(this);
    }

    @Override
    public void onPlayingMetaChanged() {

    }

    @Override
    public void onServiceConnected() {

    }

    @Override
    public void onServiceDisconnected() {

    }

    @Override
    public void onQueueChanged() {

    }

    @Override
    public void onPlayStateChanged() {

    }

    @Override
    public void onRepeatModeChanged() {

    }

    @Override
    public void onShuffleModeChanged() {

    }

    @Override
    public void onMediaStoreChanged() {

    }
}
