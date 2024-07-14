package io.ymusic.app.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import io.ymusic.app.R;

public class NotificationOreo {
	
	public static void init(Context context) {
		
		// create the NotificationChannel, but only on API 26+ because the NotificationChannel class is new and not in the support library
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			
			NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			
			// channel
			NotificationChannel generalChannel = new NotificationChannel(context.getString(R.string.general_channel_name), context.getString(R.string.general_channel_name), NotificationManager.IMPORTANCE_HIGH);
			NotificationChannel downloadChannel = new NotificationChannel(context.getString(R.string.download_channel_name), context.getString(R.string.download_channel_name), NotificationManager.IMPORTANCE_DEFAULT);
			
			// submit the notification channel object to notification manager
			if (notificationManager != null) {
				notificationManager.createNotificationChannel(generalChannel);
				notificationManager.createNotificationChannel(downloadChannel);
			}
		}
	}
}
