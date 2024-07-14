package io.ymusic.app.fragments.songs;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import io.ymusic.app.R;
import io.ymusic.app.fragments.songs.fragments.AlbumFragment;
import io.ymusic.app.fragments.songs.fragments.ArtistFragment;
import io.ymusic.app.fragments.songs.fragments.PlaylistFragment;
import io.ymusic.app.fragments.songs.fragments.SongFragment;

public class SongsPagerAdapter extends FragmentStateAdapter {

    public SongsPagerAdapter(FragmentManager fm, Lifecycle lifecycle) {
        super(fm, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return SongFragment.getInstance();
            case 1:
                return ArtistFragment.getInstance();
            case 2:
                return AlbumFragment.getInstance();
            case 3:
                return PlaylistFragment.getInstance();
            default:
                return new Fragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
