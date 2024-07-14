package io.ymusic.app.fragments.trending;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.nativead.NativeAdOptions;

import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.kiosk.KioskInfo;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.localization.ContentCountry;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import butterknife.ButterKnife;
import icepick.State;
import io.ymusic.app.R;
import io.ymusic.app.ads.AppInterstitialAd;
import io.ymusic.app.ads.nativead.AppNativeAdView;
import io.ymusic.app.ads.nativead.NativeAdStyle;
import io.ymusic.app.fragments.BaseListInfoFragment;
import io.ymusic.app.util.ExtractorHelper;
import io.ymusic.app.util.Localization;
import io.ymusic.app.util.dialog.DialogUtils;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class Top50Fragment extends BaseListInfoFragment<KioskInfo> {

    @State
    String kioskId = "";
    String kioskTranslatedName;
    @State
    ContentCountry contentCountry;

    // NativeAd
    private AppNativeAdView nativeAdView;
    private View headerRootLayout;

    public static Top50Fragment getInstance(final int serviceId) throws ExtractionException {
        return getInstance(serviceId, NewPipe.getService(serviceId).getKioskList().getDefaultKioskId());
    }

    public static Top50Fragment getInstance(final int serviceId, final String kioskId) throws ExtractionException {
        final Top50Fragment instance = new Top50Fragment();
        final StreamingService service = NewPipe.getService(serviceId);
        final ListLinkHandlerFactory kioskLinkHandlerFactory = service.getKioskList().getListLinkHandlerFactoryByType(kioskId);
        instance.setInitialData(serviceId, kioskLinkHandlerFactory.fromId(kioskId).getUrl(), kioskId);
        instance.kioskId = kioskId;
        instance.kioskTranslatedName = "Top 50";
        return instance;
    }

    public Top50Fragment() {
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        kioskTranslatedName = "Top 50";
        name = kioskTranslatedName;
        contentCountry = Localization.getAppCountry(requireContext());
        infoListAdapter.useMiniItemVariants(true);

        AppInterstitialAd.getInstance().init(activity);
    }

    @Override
    protected void initViews(View rootView, Bundle savedInstanceState) {
        super.initViews(rootView, savedInstanceState);

        headerRootLayout = activity.getLayoutInflater().inflate(R.layout.native_ad_list_header, itemsList, false);
        nativeAdView = headerRootLayout.findViewById(R.id.template_view);

        // show ad
        showNativeAd();
    }

    @Override
    protected void initListeners() {
        super.initListeners();
        /*infoListAdapter.setOnStreamSelectedListener(new OnClickGesture<>() {

            @Override
            public void selected(StreamInfoItem selectedItem) {
                AppInterstitialAd.getInstance().showInterstitialAd(activity, () -> {
                    NavigationHelper.openVideoDetailFragment(getFM(), selectedItem.getServiceId(), selectedItem.getUrl(), selectedItem.getName());
                });
            }

            @Override
            public void download(StreamInfoItem selectedItem) {
                if (!PermissionHelper.checkStoragePermissions(getActivity(), PermissionHelper.DOWNLOADS_REQUEST_CODE)) {
                    return;
                }
                // download
                onStreamDownload(selectedItem);
            }
        });*/
    }

    @SuppressLint("CheckResult")
    private void onStreamDownload(StreamInfoItem selectedItem) {
        // create getting link dialog
        AlertDialog gettingLinkDialog = DialogUtils.gettingLinkDialog(activity);
        gettingLinkDialog.show();

        // extractor from video URL
        ExtractorHelper.getStreamInfo(ServiceList.SoundCloud.getServiceId(), selectedItem.getUrl(), true)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(streamInfo -> {
                    try {
                        // dismiss getting dialog
                        gettingLinkDialog.dismiss();
                        // download
                        if (streamInfo.getAudioStreams().size() > 0) {
                            AppInterstitialAd.getInstance().showInterstitialAd(activity, () -> download(streamInfo));
                        } else {
                            // show dialog URL not supported
                            DialogUtils.showDialogURLNotSupported(activity);
                        }
                    } catch (Exception e) {
                        // dismiss getting dialog
                        gettingLinkDialog.dismiss();
                        // show dialog URL not supported
                        DialogUtils.showDialogURLNotSupported(activity);
                    }
                }, throwable -> {
                    // dismiss getting dialog
                    gettingLinkDialog.dismiss();
                    // show dialog URL not supported
                    DialogUtils.showDialogURLNotSupported(activity);
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!Localization.getAppCountry(requireContext()).equals(contentCountry)) {
            reloadContent();
        }
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_top50, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public Single<KioskInfo> loadResult(final boolean forceReload) {
        contentCountry = Localization.getAppCountry(requireContext());
        return ExtractorHelper.getKioskInfo(serviceId, url, forceReload);
    }

    @Override
    public Single<ListExtractor.InfoItemsPage> loadMoreItemsLogic() {
        return ExtractorHelper.getMoreKioskItems(serviceId, url, currentNextPage);
    }

    @Override
    public void handleResult(@NonNull final KioskInfo result) {
        super.handleResult(result);
        name = kioskTranslatedName;
    }

    @Override
    public void onDestroy() {
        // destroy native
        if (nativeAdView != null) {
            nativeAdView.destroyNativeAd();
        }
        super.onDestroy();
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