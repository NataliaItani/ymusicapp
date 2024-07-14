package io.ymusic.app.fragments.trending;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import org.jetbrains.annotations.NotNull;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;

import io.ymusic.app.R;

public class TrendingViewPagerAdapter extends FragmentStatePagerAdapter {

    private final Context context;

    public TrendingViewPagerAdapter(FragmentManager fm, Context context) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        this.context = context;
    }

    @NotNull
    @Override
    public Fragment getItem(int position) {

        switch (position) {

            case 0:
                try {
                    return Top50Fragment.getInstance(ServiceList.SoundCloud.getServiceId(), "Top 50");
                } catch (ExtractionException e) {
                    e.printStackTrace();
                }

            case 1:
                try {
                    return NewAndHotFragment.getInstance(ServiceList.SoundCloud.getServiceId(), "New & hot");
                } catch (ExtractionException e) {
                    e.printStackTrace();
                }

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
                return context.getString(R.string.top50);

            case 1:
                return context.getString(R.string.new_and_hot);

            default:
                return "";
        }
    }
}
