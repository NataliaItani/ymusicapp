package io.ymusic.app.fragments.download;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import io.ymusic.app.R;
import io.ymusic.app.activities.MainActivity;

public class DownloadNotification {
	
	private static final String TAG = DownloadNotification.class.getName();
	
	public static void notification(Context context, String title, String text) {
		
		String channelId = context.getString(R.string.download_channel_name);
		
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		
		// cancel the previous Notification, if any
		notificationManager.cancel(TAG, 1001);
		
		// builder for Notification
		NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId);
		
		// icon
		builder.setSmallIcon(R.drawable.ic_done_white_24dp);
		// title
		builder.setContentTitle(title);
		// title as status bar text
		builder.setTicker(title);
		// second line text
		builder.setContentText(text);
		// set Intent to use when click notification
		builder.setContentIntent(getPendingIntent(context));
		// clear notification when clicked
		builder.setAutoCancel(true);
		// set notification to channel
		builder.setChannelId(channelId);
		
		// notify
		notificationManager.notify(TAG, 1001, builder.build());
	}
	
	private static PendingIntent getPendingIntent(Context context) {
		TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context);
		Intent intent = new Intent(context, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		taskStackBuilder.addNextIntent(intent);
		
		// get the PendingIntent for taskStackBuilder
		return taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE);
	}
}
