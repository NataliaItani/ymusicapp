package io.ymusic.app.local.subscription;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.nativead.NativeAdOptions;

import org.jetbrains.annotations.NotNull;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.channel.ChannelInfoItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import icepick.State;
import io.ymusic.app.R;
import io.ymusic.app.ads.nativead.AppNativeAdView;
import io.ymusic.app.ads.nativead.NativeAdStyle;
import io.ymusic.app.database.subscription.SubscriptionEntity;
import io.ymusic.app.fragments.BaseStateFragment;
import io.ymusic.app.info_list.InfoListAdapter;
import io.ymusic.app.report.ErrorActivity;
import io.ymusic.app.report.UserAction;
import io.ymusic.app.util.AnimationUtils;
import io.ymusic.app.util.NavigationHelper;
import io.ymusic.app.util.OnClickGesture;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class SubscriptionFragment extends BaseStateFragment<List<SubscriptionEntity>> {

    @BindView(R.id.empty_message)
    TextView emptyMessage;
    // NativeAd
    private AppNativeAdView nativeAdView;

    private RecyclerView itemsList;
    @State
    protected Parcelable itemsListState;
    private InfoListAdapter infoListAdapter;

    private CompositeDisposable disposables = new CompositeDisposable();
    private SubscriptionService subscriptionService;

    private View headerRootLayout;

    public static SubscriptionFragment getInstance() {
        return new SubscriptionFragment();
    }

    // Fragment LifeCycle
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onAttach(@NotNull Context context) {

        super.onAttach(context);

        infoListAdapter = new InfoListAdapter(activity, true);
        subscriptionService = SubscriptionService.getInstance(activity);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_subscription, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // show ad
        showNativeAd();
    }

    @Override
    public void onPause() {

        super.onPause();

        if (itemsList.getLayoutManager() != null) {
            itemsListState = itemsList.getLayoutManager().onSaveInstanceState();
        }
    }

    @Override
    public void onDestroyView() {

        if (disposables != null) disposables.clear();

        super.onDestroyView();
    }

    @Override
    public void onDestroy() {

        // destroy ad
        if (nativeAdView != null) {
            nativeAdView.destroyNativeAd();
        }

        if (disposables != null) disposables.dispose();
        disposables = null;
        subscriptionService = null;

        super.onDestroy();
    }

    // Fragment Views
    @Override
    protected void initViews(View rootView, Bundle savedInstanceState) {

        super.initViews(rootView, savedInstanceState);

        itemsList = rootView.findViewById(R.id.items_list);
        itemsList.setLayoutManager(new LinearLayoutManager(activity));

        infoListAdapter.useMiniItemVariants(true);
        itemsList.setAdapter(infoListAdapter);

        // for empty message
        emptyMessage.setText(R.string.no_results);

        headerRootLayout = activity.getLayoutInflater().inflate(R.layout.native_ad_list_header, itemsList, false);
        nativeAdView = headerRootLayout.findViewById(R.id.template_view);
    }

    @Override
    protected void initListeners() {

        super.initListeners();

        // onClick listener
        infoListAdapter.setOnChannelSelectedListener(new OnClickGesture<>() {

            @Override
            public void selected(ChannelInfoItem selectedItem) {
                try {
                    // Requires the parent fragment to find holder for fragment replacement
                    NavigationHelper.openChannelFragment(activity.getSupportFragmentManager(),
                            selectedItem.getServiceId(),
                            selectedItem.getUrl(),
                            selectedItem.getName());
                } catch (Exception e) {
                    ErrorActivity.reportUiError(activity, e);
                }
            }

            @Override
            public void swipe(ChannelInfoItem selectedItem) {
                // unsubscribe channel
                unsubscribeChannel(selectedItem);
            }
        });
    }

    private void resetFragment() {

        if (disposables != null) disposables.clear();
        if (infoListAdapter != null) infoListAdapter.clearStreamItemList();
    }

    // Subscriptions Loader
    @Override
    public void startLoading(boolean forceLoad) {

        super.startLoading(forceLoad);

        resetFragment();

        subscriptionService.getSubscription().toObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(getSubscriptionObserver());
    }

    private Observer<List<SubscriptionEntity>> getSubscriptionObserver() {

        return new Observer<>() {

            @Override
            public void onSubscribe(Disposable d) {

                showLoading();
                disposables.add(d);
            }

            @Override
            public void onNext(List<SubscriptionEntity> subscriptions) {
                handleResult(subscriptions);
            }

            @Override
            public void onError(Throwable exception) {
                SubscriptionFragment.this.onError(exception);
            }

            @Override
            public void onComplete() {
            }
        };
    }

    @Override
    public void handleResult(@NonNull List<SubscriptionEntity> result) {

        super.handleResult(result);

        infoListAdapter.clearStreamItemList();

        if (result.isEmpty()) {
            showEmptyState();
            return;
        }

        infoListAdapter.addInfoItemList(getSubscriptionItems(result));
        if (itemsListState != null && itemsList.getLayoutManager() != null) {
            itemsList.getLayoutManager().onRestoreInstanceState(itemsListState);
            itemsListState = null;
        }

        hideLoading();
    }

    private List<InfoItem> getSubscriptionItems(List<SubscriptionEntity> subscriptions) {

        List<InfoItem> items = new ArrayList<>();
        for (final SubscriptionEntity subscription : subscriptions) {
            items.add(subscription.toChannelInfoItem());
        }

        Collections.sort(items, (InfoItem o1, InfoItem o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
        return items;
    }

    private void unsubscribeChannel(ChannelInfoItem channelInfoItem) {

        subscriptionService.subscriptionTable()
                .getSubscription(channelInfoItem.getServiceId(), channelInfoItem.getUrl())
                .toObservable()
                .observeOn(Schedulers.io())
                .subscribe(getDeleteObserver());
    }

    private Observer<List<SubscriptionEntity>> getDeleteObserver() {

        return new Observer<>() {

            @Override
            public void onSubscribe(Disposable disposable) {
                disposables.add(disposable);
            }

            @Override
            public void onNext(List<SubscriptionEntity> subscriptionEntities) {
                subscriptionService.subscriptionTable().delete(subscriptionEntities);
            }

            @Override
            public void onError(Throwable exception) {
                SubscriptionFragment.this.onError(exception);
            }

            @Override
            public void onComplete() {
            }
        };
    }

    // Contract
    @Override
    public void showLoading() {

        super.showLoading();

        AnimationUtils.animateView(itemsList, false, 100);
    }

    @Override
    public void hideLoading() {

        super.hideLoading();

        AnimationUtils.animateView(itemsList, true, 200);
    }

    // Fragment Error Handling
    @Override
    protected boolean onError(Throwable exception) {

        resetFragment();
        if (super.onError(exception)) return true;

        onUnrecoverableError(exception, UserAction.SOMETHING_ELSE, "none", "Subscriptions", R.string.general_error);
        return true;
    }

    private void showNativeAd() {

        // ad options
        VideoOptions videoOptions = new VideoOptions.Builder()
                .setStartMuted(true)
                .build();

        NativeAdOptions adOptions = new NativeAdOptions.Builder()
                .setVideoOptions(videoOptions)
                .build();

        AdLoader adLoader = new AdLoader.Builder(activity, activity.getString(R.string.native_ad))
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
                        nativeAdView.setVisibility(View.GONE);
                        infoListAdapter.setHeader(null);
                    }

                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        nativeAdView.setVisibility(View.VISIBLE);
                        infoListAdapter.setHeader(headerRootLayout);
                    }
                })
                .withNativeAdOptions(adOptions)
                .build();

        // loadAd
        AdRequest.Builder builder = new AdRequest.Builder();
        adLoader.loadAd(builder.build());
    }
}
