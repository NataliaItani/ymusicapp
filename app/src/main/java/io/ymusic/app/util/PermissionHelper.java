package io.ymusic.app.util;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionHelper {

    public static final int DOWNLOADS_REQUEST_CODE = 9001;

    public static boolean checkStoragePermissions(Activity activity, int requestCode) {
        if (!checkReadStoragePermissions(activity, requestCode)) return false;
        return checkWriteStoragePermissions(activity, requestCode);
    }

    public static boolean checkReadStoragePermissions(Activity activity, int requestCode) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
            return false;
        }
        return true;
    }

    public static boolean checkWriteStoragePermissions(Activity activity, int requestCode) {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            /*if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {*/

            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);

            // PERMISSION_WRITE_STORAGE is an
            // app-defined int constant. The callback method gets the
            // result of the request.
            /*}*/
            return false;
        }
        return true;
    }
}
