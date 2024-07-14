package io.ymusic.app.updates;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.webkit.URLUtil;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import io.ymusic.app.App;
import io.ymusic.app.R;

public class UpdateApp extends AsyncTask<String, Integer, String> {

    private ProgressDialog mPDialog;
    @SuppressLint("StaticFieldLeak")
    private Context mContext;
    private static final String mMimeType = "application/vnd.android.package-archive";

    public void setContext(Activity context) {
        mContext = context;
        context.runOnUiThread(() -> {
            mPDialog = new ProgressDialog(mContext);
            mPDialog.setMessage(context.getString(R.string.downloading_the_new_version));
            mPDialog.setIndeterminate(true);
            mPDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mPDialog.setCancelable(false);
            mPDialog.show();
        });
    }

    @Override
    protected String doInBackground(String... arg0) {
        try {

            URL url = new URL(arg0[0]);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setDoOutput(true);
            httpURLConnection.connect();
            int lengthOfFile = httpURLConnection.getContentLength();

            String PATH = mContext.getExternalFilesDir(null).getAbsolutePath();
            File file = new File(PATH);
            file.mkdirs();
            String fileName = URLUtil.guessFileName(String.valueOf(url), null, mMimeType);
            File outputFile = new File(file, fileName);
            if (outputFile.exists()) {
                outputFile.delete();
            }

            FileOutputStream fos = new FileOutputStream(outputFile);
            InputStream is = httpURLConnection.getInputStream();

            byte[] buffer = new byte[1024];
            int length;
            long total = 0;
            while ((length = is.read(buffer)) != -1) {
                total += length;
                fos.write(buffer, 0, length);
                publishProgress((int) ((total * 100) / lengthOfFile));
            }
            fos.close();
            is.close();
            if (mPDialog != null) {
                mPDialog.dismiss();
            }
            installApk(fileName);
        } catch (Exception e) {
            Log.e("UpdateApp", "Update error! " + e.getMessage());
        }
        return null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (mPDialog != null) {
            mPDialog.show();
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        if (mPDialog != null) {
            mPDialog.setIndeterminate(false);
            mPDialog.setMax(100);
            mPDialog.setProgress(values[0]);
        }
    }

    @Override
    protected void onPostExecute(String result) {
        if (mPDialog != null) {
            mPDialog.dismiss();
        }
    }

    private void installApk(String fileName) {
        try {
            String PATH = mContext.getExternalFilesDir(null).getAbsolutePath();
            File file = new File(PATH + File.separator + fileName);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri downloaded_apk = FileProvider.getUriForFile(mContext,
                        mContext.getApplicationContext().getPackageName() + ".provider", file);
                intent.setDataAndType(downloaded_apk, mMimeType);
                List<ResolveInfo> resInfoList = mContext.getPackageManager().queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    mContext.grantUriPermission(mContext.getApplicationContext().getPackageName() + ".provider", downloaded_apk,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                    | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                App.getAppContext().startActivity(intent);
            } else {
                intent.setAction(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
                intent.setDataAndType(Uri.fromFile(file), mMimeType);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            App.getAppContext().startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}