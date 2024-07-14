package io.ymusic.app.player.helper;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;

import org.schabi.newpipe.extractor.stream.StreamInfo;

import io.ymusic.app.App;
import io.ymusic.app.player.MainPlayer;
import io.ymusic.app.player.VideoPlayerImpl;
import io.ymusic.app.player.event.PlayerServiceEventListener;
import io.ymusic.app.player.event.PlayerServiceExtendedEventListener;
import io.ymusic.app.player.playqueue.PlayQueue;

public final class PlayerHolder {

    private PlayerHolder() {
    }

    private static PlayerServiceExtendedEventListener listener;
    private static ServiceConnection serviceConnection;
    public static boolean bound;
    private static MainPlayer playerService;
    private static VideoPlayerImpl player;

    public static void setListener(final PlayerServiceExtendedEventListener newListener) {
        listener = newListener;
        // Force reload data from service
        if (player != null) {
            listener.onServiceConnected(player, playerService, false);
            startPlayerListener();
        }
    }

    public static void removeListener() {
        listener = null;
    }

    public static boolean isPlayerOpen() {
        return player != null;
    }

    public static void startService(final Context context, final boolean playAfterConnect, final PlayerServiceExtendedEventListener newListener) {
        setListener(newListener);
        if (bound) {
            return;
        }
        // startService() can be called concurrently and it will give a random crashes
        // and NullPointerExceptions inside the service because the service will be
        // bound twice. Prevent it with unbinding first
        unbind(context);
        context.startService(new Intent(context, MainPlayer.class));
        serviceConnection = getServiceConnection(context, playAfterConnect);
        bind(context);
    }

    public static void stopService(final Context context) {
        unbind(context);
        context.stopService(new Intent(context, MainPlayer.class));
    }

    private static ServiceConnection getServiceConnection(final Context context, final boolean playAfterConnect) {
        return new ServiceConnection() {
            @Override
            public void onServiceDisconnected(final ComponentName compName) {
                unbind(context);
            }

            @Override
            public void onServiceConnected(final ComponentName compName, final IBinder service) {
                final MainPlayer.LocalBinder localBinder = (MainPlayer.LocalBinder) service;

                playerService = localBinder.getService();
                player = localBinder.getPlayer();
                if (listener != null) {
                    listener.onServiceConnected(player, playerService, playAfterConnect);
                }
                startPlayerListener();
            }
        };
    }

    private static void bind(final Context context) {
        final Intent serviceIntent = new Intent(context, MainPlayer.class);
        bound = context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        if (!bound) {
            context.unbindService(serviceConnection);
        }
    }

    private static void unbind(final Context context) {
        if (bound) {
            context.unbindService(serviceConnection);
            bound = false;
            stopPlayerListener();
            playerService = null;
            player = null;
            if (listener != null) {
                listener.onServiceDisconnected();
            }
        }
    }

    private static void startPlayerListener() {
        if (player != null) {
            player.setFragmentListener(INNER_LISTENER);
        }
    }

    private static void stopPlayerListener() {
        if (player != null) {
            player.removeFragmentListener(INNER_LISTENER);
        }
    }

    private static final PlayerServiceEventListener INNER_LISTENER = new PlayerServiceEventListener() {

        @Override
        public void onPlayerError(final ExoPlaybackException error) {
            if (listener != null) {
                listener.onPlayerError(error);
            }
        }

        @Override
        public void onQueueUpdate(final PlayQueue queue) {
            if (listener != null) {
                listener.onQueueUpdate(queue);
            }
        }

        @Override
        public void onPlaybackUpdate(final int state, final int repeatMode, final boolean shuffled, final PlaybackParameters parameters) {
            if (listener != null) {
                listener.onPlaybackUpdate(state, repeatMode, shuffled, parameters);
            }
        }

        @Override
        public void onProgressUpdate(final int currentProgress, final int duration, final int bufferPercent) {
            if (listener != null) {
                listener.onProgressUpdate(currentProgress, duration, bufferPercent);
            }
        }

        @Override
        public void onMetadataUpdate(final StreamInfo info, final PlayQueue queue) {
            if (listener != null) {
                listener.onMetadataUpdate(info, queue);
            }
        }

        @Override
        public void onServiceStopped() {
            if (listener != null) {
                listener.onServiceStopped();
            }
            unbind(App.applicationContext);
        }
    };
}
