package io.ymusic.app.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Room;

public final class SDMusicDatabase {

    private static volatile AppDatabase databaseInstance;

    private SDMusicDatabase() {
    }

    private static AppDatabase getDatabase(Context context) {
        
        return Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, AppDatabase.DATABASE_NAME)
                .addMigrations(Migrations.MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build();
    }

    @NonNull
    public static AppDatabase getInstance(@NonNull Context context) {
      
        AppDatabase result = databaseInstance;
       
        if (result == null) {
      
            synchronized (SDMusicDatabase.class) {
      
                result = databaseInstance;
                if (result == null) {
                    databaseInstance = (result = getDatabase(context));
                }
            }
        }

        return result;
    }
}
