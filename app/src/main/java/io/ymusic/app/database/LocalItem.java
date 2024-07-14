package io.ymusic.app.database;

public interface LocalItem {
    enum LocalItemType {
        STATISTIC_STREAM_ITEM,
    }

    LocalItemType getLocalItemType();
}
