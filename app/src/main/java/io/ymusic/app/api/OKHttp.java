package io.ymusic.app.api;

import android.util.Log;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class OKHttp {

    private static OkHttpClient client = null;

    public static OkHttpClient getInstance() {
        // create OKHttp
        synchronized (OKHttp.class) {
            if (client == null) {
                client = new OkHttpClient.Builder()
                        .retryOnConnectionFailure(false)
                        .build();
            }
            return client;
        }
    }

    public static Request buildRequest(String url) {
        Log.e("TAG", "Started Request");
        return new Request.Builder().url(url).get().build();
    }


}
