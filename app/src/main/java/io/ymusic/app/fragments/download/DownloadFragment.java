package io.ymusic.app.fragments.download;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.annimon.stream.Stream;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.nativead.NativeAdOptions;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.ButterKnife;
import io.ymusic.app.App;
import io.ymusic.app.R;
import io.ymusic.app.ads.AppInterstitialAd;
import io.ymusic.app.ads.nativead.AppNativeAdView;
import io.ymusic.app.ads.nativead.NativeAdStyle;
import io.ymusic.app.database.LocalItem;
import io.ymusic.app.database.stream.StreamStatisticsEntry;
import io.ymusic.app.local.BaseLocalListFragment;
import io.ymusic.app.local.history.HistoryRecordManager;
import io.ymusic.app.local_player.helper.MusicPlayerRemote;
import io.ymusic.app.player.helper.PlayerHolder;
import io.ymusic.app.player.playqueue.PlayQueue;
import io.ymusic.app.player.playqueue.SinglePlayQueue;
import io.ymusic.app.util.NavigationHelper;
import io.ymusic.app.util.OnClickGesture;
import io.ymusic.app.util.dialog.DialogUtils;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class DownloadFragment extends BaseLocalListFragment<List<StreamStatisticsEntry>, Void> {

    private AppNativeAdView nativeAdView;
    private View headerRootLayout;

    // Used for independent events
    private Subscription databaseSubscription;
    private HistoryRecordManager recordManager;

    @NonNull
    public static DownloadFragment getInstance() {
        return new DownloadFragment();
    }

    // LifeCycle
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recordManager = new HistoryRecordManager(activity);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_download, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    protected void initViews(View rootView, Bundle savedInstanceState) {
        super.initViews(rootView, savedInstanceState);

        headerRootLayout = activity.getLayoutInflater().inflate(R.layout.native_ad_list_header, itemsList, false);
        nativeAdView = headerRootLayout.findViewById(R.id.template_view);
//        itemListAdapter.setHeader(headerRootLayout);

        showNativeAd();
    }

    @Override
    protected void initListeners() {
        super.initListeners();
        itemListAdapter.setSelectedListener(new OnClickGesture<LocalItem>() {

            @Override
            public void selected(LocalItem selectedItem) {
                if (selectedItem instanceof StreamStatisticsEntry) {
                    AppInterstitialAd.getInstance().showInterstitialAd(activity, () -> {
                        // remove local player first
                        MusicPlayerRemote.clearQueue();

                        // start remote player
                        StreamInfoItem infoItem = ((StreamStatisticsEntry) selectedItem).toStreamInfoItem();
                        int selectedIndex = itemListAdapter.getItemsList().indexOf(selectedItem);
                        PlayerHolder.stopService(App.getAppContext());
                        NavigationHelper.openVideoDetailFragment(getFM(), infoItem.getServiceId(), infoItem.getUrl(), infoItem.getName(), true, getPlayQueue(selectedIndex));
                    });
                }
            }

            @Override
            public void delete(LocalItem selectedItem) {
                if (selectedItem instanceof StreamStatisticsEntry) {
                    //StreamInfoItem infoItem = ((StreamStatisticsEntry) selectedItem).toStreamInfoItem();
                    final int deleteIndex = Math.max(itemListAdapter.getItemsList().indexOf(selectedItem), 0);
                    DialogUtils.showDeleteDialog(activity, ((StreamStatisticsEntry) selectedItem).title, (dialogInterface, which) -> {
                        deleteEntry(deleteIndex);
                    });
                }
            }
        });
    }

    private PlayQueue getPlayQueue(final int index) {
        if (itemListAdapter == null) {
            return new SinglePlayQueue(Collections.emptyList(), 0);
        }

        final List<Object> infoItems = itemListAdapter.getItemsList();
        List<StreamInfoItem> streamInfoItems = new ArrayList<>(infoItems.size());
        for (final Object item : infoItems) {
            if (item instanceof StreamStatisticsEntry) {
                streamInfoItems.add(((StreamStatisticsEntry) item).toStreamInfoItem());
            }
        }
        return new SinglePlayQueue(streamInfoItems, index);
    }

    // Fragment LifeCycle - Loading
    @Override
    public void startLoading(boolean forceLoad) {
        super.startLoading(forceLoad);
        recordManager.getStreamStatistics().observeOn(AndroidSchedulers.mainThread())
                .subscribe(getHistoryObserver());
    }

    // Statistics Loader
    private Subscriber<List<StreamStatisticsEntry>> getHistoryObserver() {

        return new Subscriber<List<StreamStatisticsEntry>>() {

            @Override
            public void onSubscribe(Subscription s) {
                if (databaseSubscription != null) {
                    databaseSubscription.cancel();
                }
                databaseSubscription = s;
                databaseSubscription.request(1);
            }

            @Override
            public void onNext(List<StreamStatisticsEntry> streams) {
                handleResult(streams);
                if (databaseSubscription != null) {
                    databaseSubscription.request(1);
                }
            }

            @Override
            public void onError(Throwable exception) {
                DownloadFragment.this.onError(exception);
            }

            @Override
            public void onComplete() {
            }
        };
    }

    @Override
    public void handleResult(@NonNull List<StreamStatisticsEntry> results) {
        super.handleResult(results);
        if (itemListAdapter == null) return;

        itemListAdapter.clearStreamItemList();

        // if results empty don't need to do anything
        if (results.isEmpty()) {
            showEmptyState();
            return;
        }

        List<StreamStatisticsEntry> statisticsEntryList = Stream.of(results)
                //.sorted((left, right) -> right.title.compareTo(left.title))
                .toList();
        itemListAdapter.addItems(statisticsEntryList);
    }

    @SuppressLint("CheckResult")
    private void deleteEntry(final int index) {
        final LocalItem infoItem = (LocalItem) itemListAdapter.getItemsList().get(index);
        if (infoItem instanceof StreamStatisticsEntry) {

            final StreamStatisticsEntry entry = (StreamStatisticsEntry) infoItem;
            recordManager.deleteStreamHistory(entry.streamId)
                    .observeOn(AndroidSchedulers.mainThread()).subscribe(
                    // onNext
                    ignored -> {
                        /*Toast.makeText(activity, R.string.msg_delete_successfully, Toast.LENGTH_SHORT).show();*/
                    },
                    // onError
                    throwable -> Toast.makeText(activity, R.string.error_snackbar_message, Toast.LENGTH_SHORT).show());

        }
    }

    // Fragment Error Handling
    @Override
    protected void resetFragment() {
        super.resetFragment();
        if (databaseSubscription != null) databaseSubscription.cancel();
    }

    @Override
    protected boolean onError(Throwable exception) {
        if (super.onError(exception)) return true;
        DialogUtils.showErrorDialog(activity, R.string.error_snackbar_message);
        return true;
    }

    // Fragment LifeCycle - Destruction
    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (itemListAdapter != null) itemListAdapter.unsetSelectedListener();
        if (databaseSubscription != null) databaseSubscription.cancel();
        databaseSubscription = null;
    }

    @Override
    public void onDestroy() {
        if (nativeAdView != null) {
            nativeAdView.destroyNativeAd();
        }
        super.onDestroy();
        recordManager = null;
    }

    private void showNativeAd() {
        // ad options
        VideoOptions videoOptions = new VideoOptions.Builder()
                .setStartMuted(true)
                .build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder()
                .setVideoOptions(videoOptions)
                .build();

        AdLoader adLoader = new AdLoader.Builder(activity, getString(R.string.native_ad))
                .forNativeAd(nativeAd -> {

                    // show the ad
                    NativeAdStyle styles = new NativeAdStyle.Builder().build();
                    nativeAdView.setStyles(styles);
                    nativeAdView.setNativeAd(nativeAd);
                })
                .withAdListener(new AdListener() {

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        // gone
                        nativeAdView.setVisibility(View.GONE);
                        itemListAdapter.setHeader(null);
                    }

                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        // visible
                        nativeAdView.setVisibility(View.VISIBLE);
                        itemListAdapter.setHeader(headerRootLayout);
                    }
                })
                .withNativeAdOptions(adOptions)
                .build();

        // loadAd
        AdRequest.Builder builder = new AdRequest.Builder();
        adLoader.loadAd(builder.build());
    }
}
