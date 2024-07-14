package io.ymusic.app.local;

import android.content.Context;

import io.ymusic.app.database.LocalItem;
import io.ymusic.app.util.OnClickGesture;

public class LocalItemBuilder {

    private final Context context;
    private OnClickGesture<LocalItem> onSelectedListener;

    public LocalItemBuilder(Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    public OnClickGesture<LocalItem> getOnItemSelectedListener() {
        return onSelectedListener;
    }

    public void setOnItemSelectedListener(OnClickGesture<LocalItem> listener) {
        this.onSelectedListener = listener;
    }
}
