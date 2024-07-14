package io.ymusic.app.fragments.download;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import org.schabi.newpipe.extractor.stream.StreamInfo;

import java.io.File;

import io.ymusic.app.App;
import io.ymusic.app.R;
import io.ymusic.app.local.history.HistoryRecordManager;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class DownloadMusicService extends JobIntentService {

    public enum Extra {STREAM_INFO}

    private static final int JOB_ID = 1;
    private static final Handler mHandler = new Handler(Looper.getMainLooper());
    private static DownloadManager downloadManager;
    private DownloadBroadcastReceiver downloadReceiver;
    /*private NotificationClickedBroadcastReceiver notificationClickedBroadcastReceiver;*/

    private static HistoryRecordManager recordManager;
    private static StreamInfo streamInfo;
    private static final CompositeDisposable disposable = new CompositeDisposable();

    @Override
    public void onCreate() {
        super.onCreate();
        showToast(getString(R.string.download_start));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposable.clear();
        if (downloadReceiver != null) {
            unregisterReceiver(downloadReceiver);
        }
        /*if (notificationClickedBroadcastReceiver != null) {
            unregisterReceiver(notificationClickedBroadcastReceiver);
        }*/
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        recordManager = new HistoryRecordManager(this);
        streamInfo = (StreamInfo) intent.getSerializableExtra(Extra.STREAM_INFO.name());
        assert streamInfo != null;
        String songUrl = streamInfo.getAudioStreams().get(0).getUrl();
        String songTitle = streamInfo.getName();
        String fileName = streamInfo.getName() + ".mp3";

        Uri uri = Uri.parse(songUrl);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle(songTitle);

        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, getString(R.string.app_name) + File.separator + fileName);
        request.allowScanningByMediaScanner();

        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager != null) {
            downloadManager.enqueue(request);
        }

        // register download receiver
        downloadReceiver = new DownloadBroadcastReceiver();
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(downloadReceiver, filter);

        /*// register notification clicked receiver
        notificationClickedBroadcastReceiver = new NotificationClickedBroadcastReceiver();
        IntentFilter filterClicked = new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED);
        registerReceiver(notificationClickedBroadcastReceiver, filterClicked);*/
    }

    public static class DownloadBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (downloadId == -1) return;

            // query download status
            Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(downloadId));
            if (cursor.moveToFirst()) {
                @SuppressLint("Range") int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    @SuppressLint("Range") String title = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE));
                    // show notification download complete
                    DownloadNotification.notification(context, title, context.getString(R.string.download_successfully));
                    // show toast
                    CharSequence content = Html.fromHtml(context.getString(R.string.download_song_x, title));
                    showToast(String.valueOf(content));

                    // add to download history
                    final Disposable disposableDownloaded = recordManager.onDownloaded(streamInfo).onErrorComplete().subscribe();
                    disposable.add(disposableDownloaded);
                } else {
                    showToast(context.getString(R.string.download_cancelled));
                }
            } else {
                showToast(context.getString(R.string.download_cancelled));
            }
        }
    }

    /*public static class NotificationClickedBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            *//*Intent mIntent = new Intent(context, MainActivity.class);
            mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(mIntent);*//*
        }
    }*/

    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, DownloadMusicService.class, JOB_ID, intent);
    }

    private static void showToast(final String toastMessage) {
        mHandler.post(() -> Toast.makeText(App.applicationContext, toastMessage, Toast.LENGTH_SHORT).show());
    }
}
