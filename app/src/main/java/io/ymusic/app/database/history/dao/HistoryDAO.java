package io.ymusic.app.database.history.dao;

import io.ymusic.app.database.BasicDAO;

public interface HistoryDAO<T> extends BasicDAO<T> {
    T getLatestEntry();
}
