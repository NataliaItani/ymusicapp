package io.ymusic.app.fragments.library;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import org.jetbrains.annotations.NotNull;

import io.ymusic.app.R;
import io.ymusic.app.fragments.download.DownloadFragment;
import io.ymusic.app.local.subscription.SubscriptionFragment;

public class LibraryViewPagerAdapter extends FragmentStatePagerAdapter {

    private final Context context;

    public LibraryViewPagerAdapter(FragmentManager fm, Context context) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        this.context = context;
    }

    @NotNull
    @Override
    public Fragment getItem(int position) {

        switch (position) {

            case 0:
                return SubscriptionFragment.getInstance();

            case 1:
                return DownloadFragment.getInstance();

            default:
                return new Fragment();
        }
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return context.getString(R.string.subscriptions);

            case 1:
                return context.getString(R.string.history);

            default:
                return "";
        }
    }
}
