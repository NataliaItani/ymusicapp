package io.ymusic.app.util;

import android.content.Context;
import android.content.Intent;

import io.ymusic.app.R;

public class SharedUtils {

    public static void shareUrl(Context context) {

        String sharedText = "https://play.google.com/store/apps/details?id=" + context.getPackageName();

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name));
        intent.putExtra(Intent.EXTRA_TEXT, sharedText);
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_dialog_title)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
}