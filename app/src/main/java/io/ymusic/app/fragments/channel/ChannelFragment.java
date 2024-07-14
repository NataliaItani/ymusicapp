package io.ymusic.app.fragments.channel;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.material.button.MaterialButton;
import com.jakewharton.rxbinding2.view.RxView;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.jetbrains.annotations.NotNull;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.channel.ChannelInfo;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;

import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.ymusic.app.App;
import io.ymusic.app.R;
import io.ymusic.app.ads.AppInterstitialAd;
import io.ymusic.app.ads.nativead.AppNativeAdView;
import io.ymusic.app.ads.nativead.NativeAdStyle;
import io.ymusic.app.database.subscription.SubscriptionEntity;
import io.ymusic.app.fragments.BackPressable;
import io.ymusic.app.fragments.BaseListInfoFragment;
import io.ymusic.app.local.subscription.SubscriptionService;
import io.ymusic.app.report.UserAction;
import io.ymusic.app.util.AnimationUtils;
import io.ymusic.app.util.AppUtils;
import io.ymusic.app.util.ExtractorHelper;
import io.ymusic.app.util.GlideUtils;
import io.ymusic.app.util.Localization;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class ChannelFragment extends BaseListInfoFragment<ChannelInfo> implements BackPressable {

    private final CompositeDisposable disposables = new CompositeDisposable();
    private Disposable subscribeButtonMonitor;
    private SubscriptionService subscriptionService;

    // Views
    private View headerRootLayout;
    private ImageView headerChannelBanner;
    private ImageView headerAvatarView;
    private TextView headerSubscribersTextView;
    private MaterialButton headerSubscribeButton;

    // NativeAd
    private AppNativeAdView nativeAdView;
    @BindView(R.id.status_bar)
    View statusBarView;

    private String toolbarTitle;

    public static ChannelFragment getInstance(int serviceId, String url, String name) {

        ChannelFragment instance = new ChannelFragment();
        instance.setInitialData(serviceId, url, name);
        return instance;
    }

    @Override
    public void onAttach(@NotNull Context context) {

        super.onAttach(context);
        subscriptionService = SubscriptionService.getInstance(activity);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_channel, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    protected void initViews(View rootView, Bundle savedInstanceState) {

        super.initViews(rootView, savedInstanceState);

        AppUtils.setStatusBarHeight(activity, statusBarView);
        headerRootLayout = activity.getLayoutInflater().inflate(R.layout.channel_header, itemsList, false);
        headerChannelBanner = rootView.findViewById(R.id.channel_banner_image);
        headerAvatarView = rootView.findViewById(R.id.channel_avatar_view);
        TextView headerTitleView = rootView.findViewById(R.id.channel_title_view);
        headerSubscribersTextView = rootView.findViewById(R.id.channel_subscriber_view);
        headerSubscribeButton = rootView.findViewById(R.id.channel_subscribe_button);

        Toolbar mToolbar = rootView.findViewById(R.id.default_toolbar);
        activity.getDelegate().setSupportActionBar(mToolbar);
        mToolbar.setTitle(TextUtils.isEmpty(toolbarTitle) ? "" : toolbarTitle);
        mToolbar.setTitleTextColor(ContextCompat.getColor(activity, R.color.white));
        headerTitleView.setText(TextUtils.isEmpty(toolbarTitle) ? "" : toolbarTitle);
        mToolbar.setNavigationOnClickListener(view -> getFM().popBackStack());

        headerRootLayout = activity.getLayoutInflater().inflate(R.layout.native_ad_list_header, itemsList, false);
        nativeAdView = headerRootLayout.findViewById(R.id.template_view);
    }

    @Override
    public void onResume() {
        super.onResume();
        AppInterstitialAd.getInstance().init(activity);
        // show ad
        showNativeAd();
    }

    @Override
    public void onDestroy() {

        // destroy ad
        if (nativeAdView != null) {
            nativeAdView.destroyNativeAd();
        }

        super.onDestroy();
        if (disposables != null) disposables.clear();
        if (subscribeButtonMonitor != null) subscribeButtonMonitor.dispose();
    }

    private void monitorSubscription(final ChannelInfo info) {

        final Consumer<Throwable> onError = throwable -> {

            AnimationUtils.animateView(headerSubscribeButton, false, 100);
            showSnackBarError(throwable, UserAction.SUBSCRIPTION, /*NewPipe.getNameOfService(currentInfo.getServiceId())*/"", "Get subscription status", 0);
        };

        final Observable<List<SubscriptionEntity>> observable = subscriptionService.subscriptionTable()
                .getSubscription(info.getServiceId(), info.getUrl())
                .toObservable();

        disposables.add(observable.observeOn(AndroidSchedulers.mainThread()).subscribe(getSubscribeUpdateMonitor(info), onError));

        disposables.add(observable
                // Some updates are very rapid (when calling the updateSubscription(info), for example)
                // so only update the UI for the latest emission ("sync" the subscribe button's state)
                .debounce(100, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(subscriptionEntities -> updateSubscribeButton(!subscriptionEntities.isEmpty()), onError));

    }

    private Function<Object, Object> mapOnSubscribe(final SubscriptionEntity subscription) {

        return object -> {
            subscriptionService.subscriptionTable().insert(subscription);
            return object;
        };
    }

    private Function<Object, Object> mapOnUnsubscribe(final SubscriptionEntity subscription) {

        return object -> {
            subscriptionService.subscriptionTable().delete(subscription);
            return object;
        };
    }

    private void updateSubscription(final ChannelInfo info) {

        final Action onComplete = () -> {
        };

        final Consumer<Throwable> onError = throwable -> {
        };

        disposables.add(subscriptionService.updateChannelInfo(info)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(onComplete, onError));
    }

    private Disposable monitorSubscribeButton(final Button subscribeButton, final Function<Object, Object> action) {

        final Consumer<Object> onNext = object -> {
        };

        final Consumer<Throwable> onError = throwable -> {
        };

        /* Emit clicks from main thread unto io thread */
        return RxView.clicks(subscribeButton)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .debounce(100, TimeUnit.MILLISECONDS) // Ignore rapid clicks
                .map(action)
                .subscribe(onNext, onError);
    }

    private Consumer<List<SubscriptionEntity>> getSubscribeUpdateMonitor(final ChannelInfo info) {

        return subscriptionEntities -> {

            if (subscribeButtonMonitor != null) subscribeButtonMonitor.dispose();

            if (subscriptionEntities.isEmpty()) {
                SubscriptionEntity channel = new SubscriptionEntity();
                channel.setServiceId(info.getServiceId());
                channel.setUrl(info.getUrl());
                channel.setData(info.getName(), info.getAvatarUrl(), info.getDescription(), info.getSubscriberCount());
                subscribeButtonMonitor = monitorSubscribeButton(headerSubscribeButton, mapOnSubscribe(channel));
            } else {
                final SubscriptionEntity subscription = subscriptionEntities.get(0);
                subscribeButtonMonitor = monitorSubscribeButton(headerSubscribeButton, mapOnUnsubscribe(subscription));
            }
        };
    }

    private void updateSubscribeButton(boolean isSubscribed) {

        boolean isButtonVisible = headerSubscribeButton.getVisibility() == View.VISIBLE;
        int backgroundDuration = isButtonVisible ? 300 : 0;
        int textDuration = isButtonVisible ? 200 : 0;

        int subscribeBackground = ContextCompat.getColor(activity, R.color.subscribe_background_color);
        int subscribeText = ContextCompat.getColor(activity, R.color.subscribe_text_color);

        if (!isSubscribed) {
            headerSubscribeButton.setText(R.string.subscribe);
            Drawable subscribe = ContextCompat.getDrawable(activity, R.drawable.ic_subs_white_24dp);
            if (subscribe != null) {
                headerSubscribeButton.setIcon(subscribe);
            }
        } else {
            headerSubscribeButton.setText(R.string.subscribed);
            Drawable unsubscribe = ContextCompat.getDrawable(activity, R.drawable.ic_subs_check_white_24dp);
            if (unsubscribe != null) {
                headerSubscribeButton.setIcon(unsubscribe);
            }
        }
        AnimationUtils.animateBackgroundColor(headerSubscribeButton, backgroundDuration, subscribeBackground, subscribeBackground);
        AnimationUtils.animateTextColor(headerSubscribeButton, textDuration, subscribeText, subscribeText);

        AnimationUtils.animateView(headerSubscribeButton, AnimationUtils.Type.LIGHT_SCALE_AND_ALPHA, true, 100);
    }

    // Load and handle
    @Override
    protected Single<ListExtractor.InfoItemsPage> loadMoreItemsLogic() {

        return ExtractorHelper.getMoreChannelItems(serviceId, url, currentNextPage);
    }

    @Override
    protected Single<ChannelInfo> loadResult(boolean forceLoad) {

        return ExtractorHelper.getChannelInfo(serviceId, url, forceLoad);
    }

    // Contract
    @Override
    public void showLoading() {

        super.showLoading();

        ImageLoader.getInstance().cancelDisplayTask(headerChannelBanner);
        ImageLoader.getInstance().cancelDisplayTask(headerAvatarView);
        AnimationUtils.animateView(headerSubscribeButton, false, 100);
    }

    @Override
    public void handleResult(@NonNull ChannelInfo result) {

        super.handleResult(result);

        headerRootLayout.setVisibility(View.VISIBLE);
        GlideUtils.loadBanner(App.getAppContext(), headerChannelBanner, result.getBannerUrl());
        GlideUtils.loadAvatar(App.getAppContext(), headerAvatarView, result.getAvatarUrl());

        if (result.getSubscriberCount() != -1) {
            headerSubscribersTextView.setText(Localization.localizeSubscribersCount(activity, result.getSubscriberCount()));
            headerSubscribersTextView.setVisibility(View.VISIBLE);
        } else headerSubscribersTextView.setVisibility(View.GONE);

        if (!result.getErrors().isEmpty()) {
//            showSnackBarError(result.getErrors(), UserAction.REQUESTED_CHANNEL, NewPipe.getNameOfService(result.getServiceId()), result.getUrl(), 0);
        }

        if (disposables != null) disposables.clear();
        if (subscribeButtonMonitor != null) subscribeButtonMonitor.dispose();
        updateSubscription(result);
        monitorSubscription(result);
    }

    @Override
    public void handleNextItems(ListExtractor.InfoItemsPage result) {

        super.handleNextItems(result);

        if (!result.getErrors().isEmpty()) {
//            showSnackBarError(result.getErrors(), UserAction.REQUESTED_CHANNEL, NewPipe.getNameOfService(serviceId), "Get next page of: " + url, R.string.general_error);
        }
    }

    // OnError
    @Override
    protected boolean onError(Throwable exception) {

        if (super.onError(exception)) return true;

        int errorId = exception instanceof ExtractionException ? R.string.parsing_error : R.string.general_error;
//        onUnrecoverableError(exception, UserAction.REQUESTED_CHANNEL, NewPipe.getNameOfService(serviceId), url, errorId);
        return true;
    }

    @Override
    public void setTitle(String title) {
        toolbarTitle = title;
    }

    @Override
    public boolean onBackPressed() {

        // pop back stack
        if (getFragmentManager() != null) {
            getFragmentManager().popBackStack();
            return true;
        }

        return false;
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