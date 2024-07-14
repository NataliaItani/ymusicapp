package io.ymusic.app.local_player.helper;

import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import io.ymusic.app.R;
import io.ymusic.app.fragments.songs.SongsFragment.SONG_TYPE;
import io.ymusic.app.local_player.model.Song;

public class SongMenuHelper {
    public static final int MENU_RES = R.menu.menu_item_song;

    public static boolean handleMenuClick(@NonNull FragmentActivity activity, @NonNull Song song, int menuItemId) {
        switch (menuItemId) {
            case R.id.action_add_to_current_playing:
                MusicPlayerRemote.enqueue(song);
                return true;

            case R.id.action_delete_from_device:
                DeleteSongsDialog.create(song).show(activity.getSupportFragmentManager(), "DELETE_SONGS");
                return true;
        }
        return false;
    }

    public static abstract class OnClickSongMenu implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {

        private final AppCompatActivity activity;

        public OnClickSongMenu(@NonNull AppCompatActivity activity) {
            this.activity = activity;
        }

        public int getMenuRes() {
            return MENU_RES;
        }

        @Override
        public void onClick(View v) {
            PopupMenu popupMenu = new PopupMenu(activity, v);
            popupMenu.inflate(getMenuRes());
            popupMenu.setOnMenuItemClickListener(this);

            // This is solely for the playlist
            // Dynamically change the title (Add to Playlist or Remove from Playlist)
            if (getType() == SONG_TYPE.PLAYLIST) {
                // 1 is the index of add playlist
                popupMenu.getMenu().getItem(1).setTitle("Remove from playlist");
            }
            popupMenu.show();
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            return handleMenuClick(activity, getSong(), item.getItemId());
        }

        public abstract Song getSong();

        public abstract SONG_TYPE getType();
    }
}
