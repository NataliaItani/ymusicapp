package io.ymusic.app.player.helper;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.KeyEvent;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.session.MediaButtonReceiver;

import io.ymusic.app.player.mediasession.MediaSessionCallback;
import io.ymusic.app.player.mediasession.PlayQueueNavigator;
import io.ymusic.app.player.mediasession.PlayQueuePlaybackController;

public class MediaSessionManager {
	private static final String TAG = "MediaSessionManager";

	@NonNull
	private final MediaSessionCompat mediaSession;
	@NonNull
	private final MediaSessionConnector sessionConnector;

	private int lastAlbumArtHashCode;

	public MediaSessionManager(@NonNull final Context context,
							   @NonNull final Player player,
							   @NonNull final MediaSessionCallback callback) {
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(Intent.ACTION_MEDIA_BUTTON), PendingIntent.FLAG_IMMUTABLE);
		mediaSession = new MediaSessionCompat(context, TAG, null, pendingIntent);
		mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
		mediaSession.setActive(true);

		mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
				.setState(PlaybackStateCompat.STATE_NONE, -1, 1)
				.setActions(PlaybackStateCompat.ACTION_SEEK_TO
						| PlaybackStateCompat.ACTION_PLAY
						| PlaybackStateCompat.ACTION_PAUSE // was play and pause now play/pause
						| PlaybackStateCompat.ACTION_SKIP_TO_NEXT
						| PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
						| PlaybackStateCompat.ACTION_SET_REPEAT_MODE
						| PlaybackStateCompat.ACTION_STOP)
				.build());

		sessionConnector = new MediaSessionConnector(mediaSession);
		sessionConnector.setControlDispatcher(new PlayQueuePlaybackController(callback));
		sessionConnector.setQueueNavigator(new PlayQueueNavigator(mediaSession, callback));
		sessionConnector.setPlayer(player);
	}

	@Nullable
	@SuppressWarnings("UnusedReturnValue")
	public KeyEvent handleMediaButtonIntent(final Intent intent) {
		return MediaButtonReceiver.handleIntent(mediaSession, intent);
	}

	public MediaSessionCompat.Token getSessionToken() {
		return mediaSession.getSessionToken();
	}

	public void setMetadata(final String title, final String artist, final Bitmap albumArt, final long duration) {
		if (albumArt == null || !mediaSession.isActive()) {
			return;
		}

		if (getMetadataAlbumArt() == null || getMetadataTitle() == null || getMetadataArtist() == null || getMetadataDuration() <= 1 || albumArt.hashCode() != lastAlbumArtHashCode) {
			mediaSession.setMetadata(new MediaMetadataCompat.Builder()
					.putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
					.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
					.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
					.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, albumArt)
					.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration).build());
			lastAlbumArtHashCode = albumArt.hashCode();
		}
	}

	private Bitmap getMetadataAlbumArt() {
		return mediaSession.getController().getMetadata().getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART);
	}

	private String getMetadataTitle() {
		return mediaSession.getController().getMetadata().getString(MediaMetadataCompat.METADATA_KEY_TITLE);
	}

	private String getMetadataArtist() {
		return mediaSession.getController().getMetadata().getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
	}

	private long getMetadataDuration() {
		return mediaSession.getController().getMetadata().getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
	}

	/**
	 * Should be called on player destruction to prevent leakage.
	 */
	public void dispose() {
		sessionConnector.setPlayer(null);
		sessionConnector.setQueueNavigator(null);
		mediaSession.setActive(false);
		mediaSession.release();
	}
}
