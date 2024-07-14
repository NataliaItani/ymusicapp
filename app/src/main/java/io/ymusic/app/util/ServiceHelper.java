package io.ymusic.app.util;

import android.content.Context;

import org.schabi.newpipe.extractor.ServiceList;

import java.util.concurrent.TimeUnit;

public class ServiceHelper {

    public static int getSelectedServiceId(Context context) {
        return ServiceList.YouTube.getServiceId();
    }

    public static long getCacheExpirationMillis() {
        
        return TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS);
    }
}
