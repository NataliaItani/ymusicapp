package io.ymusic.app.fragments.search;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.material.textfield.TextInputEditText;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.ymusic.app.R;
import io.ymusic.app.ads.nativead.AppNativeAdView;
import io.ymusic.app.ads.nativead.NativeAdStyle;
import io.ymusic.app.base.BaseFragment;
import io.ymusic.app.fragments.BackPressable;
import io.ymusic.app.fragments.songs.SongsFragment.SONG_TYPE;
import io.ymusic.app.fragments.songs.adapters.SongAdapter;
import io.ymusic.app.fragments.songs.fragments.SongFragment;
import io.ymusic.app.local_player.model.Song;
import io.ymusic.app.util.AppUtils;
import io.ymusic.app.util.NavigationHelper;

public class LocalSearchFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<List<Song>>, BackPressable {

    @BindView(R.id.search_edit_text)
    TextInputEditText searchEditText;
    @BindView(R.id.toolbar_search_clear)
    View searchClear;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @BindView(R.id.template_view)
    AppNativeAdView nativeAdView;

    @BindView(R.id.items_list)
    RecyclerView recyclerView;

    private SongAdapter songAdapter;

    public static LocalSearchFragment getInstance() {
        return new LocalSearchFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LoaderManager.getInstance(this).initLoader(0, null, this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_local_search, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    // Init
    @Override
    protected void initViews(View rootView, Bundle savedInstanceState) {
        super.initViews(rootView, savedInstanceState);

        View statusBarView = rootView.findViewById(R.id.status_bar);
        AppUtils.setStatusBarHeight(activity, statusBarView);

        activity.getDelegate().setSupportActionBar(mToolbar);
        mToolbar.setNavigationOnClickListener(view -> NavigationHelper.goBack(getFM()));

        initRecyclerView();

        showNativeAd();

        showKeyboardSearch();
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                songAdapter.getFilter().filter(editable);
            }
        });

        searchEditText.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER || event.getAction() == EditorInfo.IME_ACTION_SEARCH)) {
                songAdapter.getFilter().filter(searchEditText.getText());
                hideKeyboardSearch();
                return true;
            }
            return false;
        });

        searchClear.setOnClickListener(view -> {
            if (!TextUtils.isEmpty(searchEditText.getText())) {
                searchEditText.setText("");
            }
        });
    }

    private void initRecyclerView() {
        songAdapter = new SongAdapter(activity, new ArrayList<>(), R.layout.list_song_item, null);
        songAdapter.setType(SONG_TYPE.SEARCH);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(songAdapter);
    }

    @Override
    public void onPause() {
        super.onPause();
        hideKeyboardSearch();
    }

    @Override
    public void onDestroy() {
        if (nativeAdView != null) {
            nativeAdView.destroyNativeAd();
        }
        super.onDestroy();
    }

    // Menu
    @Override
    public void onCreateOptionsMenu(@NotNull Menu menu, @NotNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        ActionBar actionBar = activity.getDelegate().getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }
    }

    private void showKeyboardSearch() {
        if (searchEditText == null) return;

        if (searchEditText.requestFocus()) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }

    private void hideKeyboardSearch() {
        if (searchEditText == null) return;
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
        searchEditText.clearFocus();
    }

    @Override
    public boolean onBackPressed() {
        NavigationHelper.goBack(getFM());
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
                    }

                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        // visible
                        nativeAdView.setVisibility(View.VISIBLE);
                    }
                })
                .withNativeAdOptions(adOptions)
                .build();

        // loadAd
        AdRequest.Builder builder = new AdRequest.Builder();
        adLoader.loadAd(builder.build());
    }

    @NonNull
    @Override
    public Loader<List<Song>> onCreateLoader(int id, @Nullable Bundle args) {
        return new SongFragment.AsyncSongLoader(getActivity());
    }

    @Override
    public void onLoadFinished(@NonNull Loader<List<Song>> loader, List<Song> data) {
        songAdapter.swapDataSet(data);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<List<Song>> loader) {
        songAdapter.swapDataSet(new ArrayList<>());
    }
}