package io.ymusic.app.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.nostra13.universalimageloader.core.ImageLoader;

import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;

import io.ymusic.app.R;
import io.ymusic.app.activities.MainActivity;
import io.ymusic.app.fragments.search.LocalSearchFragment;
import io.ymusic.app.fragments.songs.fragments.Song2Fragment;
import io.ymusic.app.fragments.channel.ChannelFragment;
import io.ymusic.app.fragments.detail.VideoDetailFragment;
import io.ymusic.app.fragments.download.DownloadFragment;
import io.ymusic.app.fragments.library.LibraryFragment;
import io.ymusic.app.fragments.search.SearchFragment;
import io.ymusic.app.fragments.search.SearchFragmentMain;
import io.ymusic.app.fragments.songs.SongsFragment;
import io.ymusic.app.fragments.trending.NewAndHotFragment;
import io.ymusic.app.fragments.trending.Top50Fragment;
import io.ymusic.app.fragments.trending.TrendingFragment;
import io.ymusic.app.local.subscription.SubscriptionFragment;
import io.ymusic.app.player.BasePlayer;
import io.ymusic.app.player.MainPlayer;
import io.ymusic.app.player.VideoPlayer;
import io.ymusic.app.player.playqueue.PlayQueue;
import io.ymusic.app.player.playqueue.PlayQueueItem;
import io.ymusic.app.settings.SettingsActivity;

public class NavigationHelper {

    // Players
    @NonNull
    public static <T> Intent getPlayerIntent(@NonNull final Context context,
                                             @NonNull final Class<T> targetClazz,
                                             @Nullable final PlayQueue playQueue,
                                             final boolean resumePlayback) {

        Intent intent = new Intent(context, targetClazz);

        if (playQueue != null) {
            final String cacheKey = SerializedCache.getInstance().put(playQueue, PlayQueue.class);
            if (cacheKey != null) {
                intent.putExtra(VideoPlayer.PLAY_QUEUE_KEY, cacheKey);
            }
        }

        intent.putExtra(VideoPlayer.RESUME_PLAYBACK, resumePlayback);
        intent.putExtra(VideoPlayer.PLAYER_TYPE, VideoPlayer.PLAYER_TYPE_VIDEO);

        return intent;
    }

    @NonNull
    public static <T> Intent getPlayerIntent(@NonNull final Context context,
                                             @NonNull final Class<T> targetClazz,
                                             @Nullable final PlayQueue playQueue,
                                             final boolean resumePlayback,
                                             final boolean playWhenReady) {
        return getPlayerIntent(context, targetClazz, playQueue, resumePlayback)
                .putExtra(BasePlayer.PLAY_WHEN_READY, playWhenReady);
    }

    public static void playOnMainPlayer(final FragmentManager fragmentManager, final PlayQueue queue, final boolean autoPlay) {
        final PlayQueueItem currentStream = queue.getItem();
        openVideoDetailFragment(fragmentManager, currentStream.getServiceId(), currentStream.getUrl(), currentStream.getTitle(), autoPlay, queue);
    }

    public static void startService(@NonNull final Context context, @NonNull final Intent intent) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    @SuppressLint("CommitTransaction")
    private static FragmentTransaction defaultTransaction(FragmentManager fragmentManager) {
        return fragmentManager.beginTransaction().setCustomAnimations(R.animator.custom_fade_in, R.animator.custom_fade_out, R.animator.custom_fade_in, R.animator.custom_fade_out);
    }

    public static void openMainFragment(FragmentManager fragmentManager) {
        InfoCache.getInstance().trimCache();
        ImageLoader.getInstance().clearMemoryCache();
        //fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        SearchFragmentMain searchFragmentMain = SearchFragmentMain.getInstance();
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, searchFragmentMain)
                .addToBackStack(null)
                .commit();
    }

    public static void openSearchFragment(FragmentManager fragmentManager, int serviceId, String searchString) {
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, SearchFragment.getInstance(serviceId, searchString))
                .addToBackStack(null)
                .commit();
    }

    public static void openLocalSearchFragment(FragmentManager fragmentManager) {
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, LocalSearchFragment.getInstance())
                .addToBackStack(null)
                .commit();
    }

    public static void openVideoDetailFragment(FragmentManager fragmentManager, int serviceId, String url, String title) {
        openVideoDetailFragment(fragmentManager, serviceId, url, title, true, null);
    }

    public static void openVideoDetailFragment(FragmentManager fragmentManager, int serviceId, String url, String title, final boolean autoPlay, final PlayQueue playQueue) {
        final Fragment fragment = fragmentManager.findFragmentById(R.id.fragment_player_holder);
        if (fragment instanceof VideoDetailFragment && fragment.isVisible()) {
            expandMainPlayer(fragment.requireActivity());
            final VideoDetailFragment detailFragment = (VideoDetailFragment) fragment;
            detailFragment.setAutoplay(autoPlay);
            detailFragment.selectAndLoadVideo(serviceId, url, title == null ? "" : title, playQueue);
        } else {
            final VideoDetailFragment instance = VideoDetailFragment.getInstance(serviceId, url, title == null ? "" : title, playQueue);
            instance.setAutoplay(autoPlay);

            defaultTransaction(fragmentManager)
                    .replace(R.id.fragment_player_holder, instance)
                    .runOnCommit(() -> expandMainPlayer(instance.requireActivity()))
                    .commit();
        }
    }

    public static void expandMainPlayer(final Context context) {
        context.sendBroadcast(new Intent(VideoDetailFragment.ACTION_SHOW_MAIN_PLAYER));
    }

    public static void sendPlayerStartedEvent(final Context context) {
        context.sendBroadcast(new Intent(VideoDetailFragment.ACTION_PLAYER_STARTED));
    }

    public static void showMiniPlayer(final FragmentManager fragmentManager) {
        final VideoDetailFragment instance = VideoDetailFragment.getInstanceInCollapsedState();
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_player_holder, instance)
                .runOnCommit(() -> sendPlayerStartedEvent(instance.requireActivity()))
                .commitAllowingStateLoss();
    }

    public static void openSongsFragment(Activity activity, FragmentManager fragmentManager) {
        if (!PermissionHelper.checkStoragePermissions(activity, PermissionHelper.DOWNLOADS_REQUEST_CODE)) {
            return;
        }
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, SongsFragment.getInstance())
                .addToBackStack(null)
                .commit();
    }

    public static void playOnBackgroundPlayer(final Context context, final PlayQueue queue, final boolean resumePlayback) {
        final Intent intent = getPlayerIntent(context, MainPlayer.class, queue, resumePlayback);
        intent.putExtra(VideoPlayer.PLAYER_TYPE, VideoPlayer.PLAYER_TYPE_AUDIO);
        ContextCompat.startForegroundService(context, intent);
    }

    // Link handling
    private static Intent getOpenIntent(Context context, String url, int serviceId, StreamingService.LinkType type) {
        Intent mIntent = new Intent(context, MainActivity.class);
        mIntent.putExtra(Constants.KEY_SERVICE_ID, serviceId);
        mIntent.putExtra(Constants.KEY_URL, url);
        mIntent.putExtra(Constants.KEY_LINK_TYPE, type);
        return mIntent;
    }

    public static Intent getIntentByLink(Context context, StreamingService service, String url) throws ExtractionException {
        StreamingService.LinkType linkType = service.getLinkTypeByUrl(url);
        if (linkType == StreamingService.LinkType.NONE) {
            throw new ExtractionException("Url not known to service. service=" + service + " url=" + url);
        }
        return getOpenIntent(context, url, service.getServiceId(), linkType);
    }

    public static void openDownloads(Activity activity, FragmentManager fragmentManager) {
        if (!PermissionHelper.checkStoragePermissions(activity, PermissionHelper.DOWNLOADS_REQUEST_CODE)) {
            return;
        }
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, DownloadFragment.getInstance())
                .addToBackStack(null)
                .commit();
    }

    public static void openTop50Fragment(FragmentManager fragmentManager, final int serviceId) throws ExtractionException {
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, Top50Fragment.getInstance(serviceId))
                .addToBackStack(null)
                .commit();
    }

    public static void openNewAndHotFragment(FragmentManager fragmentManager, final int serviceId) throws ExtractionException {
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, NewAndHotFragment.getInstance(serviceId))
                .addToBackStack(null)
                .commit();
    }

    public static void openTrendingFragment(FragmentManager fragmentManager) {
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, TrendingFragment.getInstance())
                .addToBackStack(null)
                .commit();
    }

    public static void openChannelFragment(FragmentManager fragmentManager, int serviceId, String url, String name) {
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, ChannelFragment.getInstance(serviceId, url, name == null ? "" : name))
                .addToBackStack(null)
                .commit();
    }

    public static void openSubscriptionFragment(final FragmentManager fragmentManager) {
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, new SubscriptionFragment())
                .addToBackStack(null)
                .commit();
    }

    public static void openLibraryFragment(final FragmentManager fragmentManager) {
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, new LibraryFragment())
                .addToBackStack(null)
                .commit();
    }

    public static void openSettings(Activity activity) {
        Intent intent = new Intent(activity, SettingsActivity.class);
        activity.startActivity(intent);
    }

    public static void openSongs(FragmentManager fragmentManager, SongsFragment.SONG_TYPE type, long id, String title) {
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, Song2Fragment.getInstance(type, id, title))
                .addToBackStack(null)
                .commit();

    }

    public static void goBack(FragmentManager fragmentManager) {
        fragmentManager.popBackStackImmediate();
    }

    public static void composeEmail(Context context, String subject) {

        //String model = String.format("Model [%s]", Build.MODEL);
        //String os = String.format("OS [%s]", "Android");
        //String os_version = String.format("OS Version [%s]", Build.VERSION.RELEASE);
        //String emailBody = String.format("About Device:\n%s\n%s\n%s", model, os, os_version);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"info@passiatech.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.feedback_message));

        // open mail apps
        try {
            context.startActivity(Intent.createChooser(intent, null));
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(context, R.string.msg_no_apps, Toast.LENGTH_SHORT).show();
        }
    }

    public static void openGooglePlayStore(Context context, String packageName) {
        try {
            // Try market:// scheme
            context.startActivity(new Intent(Intent.ACTION_VIEW, openMarket(packageName)));
        } catch (ActivityNotFoundException e) {
            // Fall back to google play URL (don't worry F-Droid can handle it :)
            context.startActivity(new Intent(Intent.ACTION_VIEW, getGooglePlay(packageName)));
        }
    }

    private static Uri openMarket(String packageName) {
        return Uri.parse("market://details").buildUpon()
                .appendQueryParameter("id", packageName)
                .build();
    }

    private static Uri getGooglePlay(String packageName) {
        return Uri.parse("https://play.google.com/store/apps/details").buildUpon()
                .appendQueryParameter("id", packageName)
                .build();
    }
}