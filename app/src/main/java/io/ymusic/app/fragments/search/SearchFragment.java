package io.ymusic.app.fragments.search;

import static java.util.Arrays.asList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import org.jetbrains.annotations.NotNull;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.search.SearchInfo;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import icepick.State;
import io.ymusic.app.R;
import io.ymusic.app.activities.ReCaptchaActivity;
import io.ymusic.app.ads.AppInterstitialAd;
import io.ymusic.app.ads.nativead.AppNativeAdView;
import io.ymusic.app.ads.nativead.NativeAdStyle;
import io.ymusic.app.database.history.model.SearchHistoryEntry;
import io.ymusic.app.fragments.BackPressable;
import io.ymusic.app.fragments.BaseListFragment;
import io.ymusic.app.local.history.HistoryRecordManager;
import io.ymusic.app.util.AnimationUtils;
import io.ymusic.app.util.AppUtils;
import io.ymusic.app.util.ExtractorHelper;
import io.ymusic.app.util.LayoutManagerSmoothScroller;
import io.ymusic.app.util.NavigationHelper;
import io.ymusic.app.util.views.LockableTabLayout;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class SearchFragment extends BaseListFragment<SearchInfo, ListExtractor.InfoItemsPage> implements BackPressable {

    /**
     * The suggestions will only be fetched from network if the query meet this threshold (>=).
     * (local ones will be fetched regardless of the length)
     */
    private static final int THRESHOLD_NETWORK_SUGGESTION = 1;

    /**
     * How much time have to pass without emitting a item (i.e. the user stop typing) to fetch/show the suggestions, in milliseconds.
     */
    private static final int SUGGESTIONS_DEBOUNCE = 120;

    @State
    protected int serviceId = ServiceList.YouTube.getServiceId();

    private static final int YOUTUBE = 0;
    private static final int SOUNDCLOUD = 1;

    @State
    protected String searchString;
    @State
    protected String[] contentFilter = new String[]{"videos"};
    @State
    protected String sortFilter;

    @State
    protected String lastSearchedString;

    @State
    protected boolean wasSearchFocused = false;

    private Page nextPage;
    private boolean isSuggestionsEnabled = true;

    private final PublishSubject<String> suggestionPublisher = PublishSubject.create();
    private Disposable searchDisposable;
    private Disposable suggestionDisposable;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private SuggestionListAdapter suggestionListAdapter;
    private HistoryRecordManager historyRecordManager;

    // Views
    private TextInputEditText searchEditText;
    private View searchClear;

    private Toolbar mToolbar;
    private View suggestionsPanel;
    private RecyclerView suggestionsRecyclerView;
    private AppNativeAdView nativeAdView;
    private View headerRootLayout;
    private LockableTabLayout tabLayout;

    public static SearchFragment getInstance(int serviceId, String searchString) {
        SearchFragment searchFragment = new SearchFragment();
        searchFragment.setQuery(ServiceList.YouTube.getServiceId(), searchString, new String[]{"videos"}, "");

        if (!TextUtils.isEmpty(searchString)) {
            searchFragment.setSearchOnResume();
        }
        return searchFragment;
    }

    /**
     * Set wasLoading to true so when the fragment onResume is called, the initial search is done.
     */
    private void setSearchOnResume() {
        wasLoading.set(true);
    }

    // Fragment's LifeCycle
    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        suggestionListAdapter = new SuggestionListAdapter(activity);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean isSearchHistoryEnabled = preferences.getBoolean(getString(R.string.enable_search_history_key), true);
        suggestionListAdapter.setShowSuggestionHistory(isSearchHistoryEnabled);

        historyRecordManager = new HistoryRecordManager(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        isSuggestionsEnabled = preferences.getBoolean(getString(R.string.show_search_suggestions_key), true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View rootView, Bundle savedInstanceState) {
        super.onViewCreated(rootView, savedInstanceState);
        showSearchOnStart();
        initSearchListeners();
        mToolbar.setNavigationOnClickListener(view -> onPopBackStack());
    }

    // Init
    @Override
    protected void initViews(View rootView, Bundle savedInstanceState) {
        super.initViews(rootView, savedInstanceState);

        View statusBarView = rootView.findViewById(R.id.status_bar);
        AppUtils.setStatusBarHeight(activity, statusBarView);

        mToolbar = rootView.findViewById(R.id.toolbar);
        activity.getDelegate().setSupportActionBar(mToolbar);

        suggestionsPanel = rootView.findViewById(R.id.suggestions_panel);
        suggestionsRecyclerView = rootView.findViewById(R.id.suggestions_list);
        suggestionsRecyclerView.setAdapter(suggestionListAdapter);
        suggestionsRecyclerView.setLayoutManager(new LayoutManagerSmoothScroller(activity));

        searchEditText = rootView.findViewById(R.id.search_edit_text);
        searchClear = rootView.findViewById(R.id.toolbar_search_clear);

        tabLayout = rootView.findViewById(R.id.tabs);

        infoListAdapter.useMiniItemVariants(true);

        headerRootLayout = activity.getLayoutInflater().inflate(R.layout.native_ad_list_header, itemsList, false);
        nativeAdView = headerRootLayout.findViewById(R.id.template_view);
//        infoListAdapter.setHeader(headerRootLayout);

        showNativeAd();
    }

    @Override
    protected void initListeners() {
        super.initListeners();
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == YOUTUBE) {
                    serviceId = ServiceList.YouTube.getServiceId();
                    setQuery(serviceId, searchString, new String[]{"videos"}, "");
                } else {
                    serviceId = ServiceList.SoundCloud.getServiceId();
                    setQuery(serviceId, searchString, new String[]{"tracks"}, "");
                }
                search(searchString, contentFilter, sortFilter);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // unimplemented
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // unimplemented
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        wasSearchFocused = searchEditText.hasFocus();

        if (searchDisposable != null) searchDisposable.dispose();
        if (suggestionDisposable != null) suggestionDisposable.dispose();
        disposables.clear();
        hideKeyboardSearch();
    }

    @Override
    public void onResume() {
        super.onResume();
        // search by keyword
        if (!TextUtils.isEmpty(searchString)) {
            if (wasLoading.getAndSet(false)) {
                search(searchString, contentFilter, sortFilter);
            } else if (infoListAdapter.getItemsList().size() == 0) {
                search(searchString, contentFilter, sortFilter);
            }
        }

        if (suggestionDisposable == null || suggestionDisposable.isDisposed())
            initSuggestionObserver();

        if (TextUtils.isEmpty(searchString) || wasSearchFocused) {
            showKeyboardSearch();
            //showSuggestionsPanel();
        } else {
            hideKeyboardSearch();
            hideSuggestionsPanel();
        }
        wasSearchFocused = false;
    }

    @Override
    public void onDestroyView() {
        unsetSearchListeners();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (nativeAdView != null) {
            nativeAdView.destroyNativeAd();
        }
        super.onDestroy();
        if (searchDisposable != null) searchDisposable.dispose();
        if (suggestionDisposable != null) suggestionDisposable.dispose();
        disposables.clear();
    }

    // State Saving
    @Override
    public void writeTo(Queue<Object> objectsToSave) {
        super.writeTo(objectsToSave);
        objectsToSave.add(nextPage);
    }

    @Override
    public void readFrom(@NonNull Queue<Object> savedObjects) throws Exception {
        super.readFrom(savedObjects);
        nextPage = (Page) savedObjects.poll();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle bundle) {
        searchString = searchEditText != null ? searchEditText.getText().toString() : searchString;
        super.onSaveInstanceState(bundle);
    }

    @Override
    public void reloadContent() {
        if (!TextUtils.isEmpty(searchString) || (searchEditText != null && !TextUtils.isEmpty(searchEditText.getText()))) {
            search(!TextUtils.isEmpty(searchString) ? searchString : searchEditText.getText().toString(), this.contentFilter, "");
        } else {
            if (searchEditText != null) {
                searchEditText.setText("");
                //showKeyboardSearch();
            }
            AnimationUtils.animateView(errorPanel, false, 200);
        }
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

    // Search
    private TextWatcher textWatcher;

    private void showSearchOnStart() {
        searchEditText.setText(searchString);
    }

    private void initSearchListeners() {
        searchClear.setOnClickListener(v -> {
            if (TextUtils.isEmpty(searchEditText.getText())) {
                onPopBackStack();
                return;
            }
            searchEditText.setText("");
            suggestionListAdapter.setItems(new ArrayList<>());
            showKeyboardSearch();
        });

        searchEditText.setOnClickListener(v -> {
            if (isSuggestionsEnabled && errorPanel.getVisibility() != View.VISIBLE) {
                //showSuggestionsPanel();
            }
        });

        searchEditText.setOnFocusChangeListener((View v, boolean hasFocus) -> {
            if (isSuggestionsEnabled && hasFocus && errorPanel.getVisibility() != View.VISIBLE) {
                //showSuggestionsPanel();
            }
        });

        suggestionListAdapter.setListener(new SuggestionListAdapter.OnSuggestionItemSelected() {

            @Override
            public void onSuggestionItemSelected(SuggestionItem item) {
                search(item.query, new String[0], "");
                searchEditText.setText(item.query);
                hideKeyboardSearch();
                hideSuggestionsPanel();
            }

            @Override
            public void onSuggestionItemInserted(SuggestionItem item) {
                searchEditText.setText(item.query);
                searchEditText.setSelection(searchEditText.getText().length());
            }

            @Override
            public void onSuggestionItemLongClick(SuggestionItem item) {
                if (item.fromHistory) showDeleteSuggestionDialog(item);
            }
        });

        if (textWatcher != null) searchEditText.removeTextChangedListener(textWatcher);

        textWatcher = new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String newText = searchEditText.getText().toString();
                suggestionPublisher.onNext(newText);
            }
        };
        searchEditText.addTextChangedListener(textWatcher);
        searchEditText.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {

            if (event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER || event.getAction() == EditorInfo.IME_ACTION_SEARCH)) {
                search(searchEditText.getText().toString(), new String[0], "");
                hideKeyboardSearch();
                hideSuggestionsPanel();
                return true;
            }
            return false;
        });

        if (suggestionDisposable == null || suggestionDisposable.isDisposed())
            initSuggestionObserver();
    }

    private void unsetSearchListeners() {
        searchClear.setOnClickListener(null);
        searchEditText.setOnClickListener(null);
        searchEditText.setOnFocusChangeListener(null);
        searchEditText.setOnEditorActionListener(null);

        if (textWatcher != null) searchEditText.removeTextChangedListener(textWatcher);
        textWatcher = null;
    }

    private void showSuggestionsPanel() {
        AnimationUtils.animateView(suggestionsPanel, AnimationUtils.Type.LIGHT_SLIDE_AND_ALPHA, true, 200);
    }

    private void hideSuggestionsPanel() {
        AnimationUtils.animateView(suggestionsPanel, AnimationUtils.Type.LIGHT_SLIDE_AND_ALPHA, false, 200);
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

    private void showDeleteSuggestionDialog(final SuggestionItem item) {
        if (activity == null || historyRecordManager == null || searchEditText == null) return;
        final String query = item.query;
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.dialog_warning_title)
                .setMessage(R.string.delete_item_search_history)
                .setCancelable(true)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    final Disposable onDelete = historyRecordManager.deleteSearchHistory(query)
                            .observeOn(AndroidSchedulers.mainThread()).subscribe(
                                    // onNext
                                    howManyDeleted -> suggestionPublisher.onNext(searchEditText.getText().toString()),
                                    // onError
                                    throwable -> {/*showSnackBarError(throwable, UserAction.DELETE_FROM_HISTORY, "none", "Deleting item failed", R.string.general_error);*/});
                    disposables.add(onDelete);
                })
                .show();
    }

    @Override
    public boolean onBackPressed() {

        if (suggestionsPanel.getVisibility() == View.VISIBLE && infoListAdapter.getItemsList().size() > 0 && !isLoading.get()) {
            hideSuggestionsPanel();
            hideKeyboardSearch();
            onPopBackStack();
            return true;
        }
        return false;
    }

    private void initSuggestionObserver() {
        if (suggestionDisposable != null) suggestionDisposable.dispose();

        final Observable<String> observable = suggestionPublisher
                .debounce(SUGGESTIONS_DEBOUNCE, TimeUnit.MILLISECONDS)
                .startWith(searchString != null ? searchString : "")
                .filter(searchString -> isSuggestionsEnabled);

        suggestionDisposable = observable.switchMap(query -> {

            final Flowable<List<SearchHistoryEntry>> flowable = historyRecordManager.getRelatedSearches(query, 3, 25);

            final Observable<List<SuggestionItem>> local = flowable.toObservable().map(searchHistoryEntries -> {
                List<SuggestionItem> result = new ArrayList<>();
                for (SearchHistoryEntry entry : searchHistoryEntries)
                    result.add(new SuggestionItem(true, entry.getSearch()));
                return result;
            });

            if (query.length() < THRESHOLD_NETWORK_SUGGESTION) {
                // Only pass through if the query length is equal or greater than THRESHOLD_NETWORK_SUGGESTION
                return local.materialize();
            }

            final Observable<List<SuggestionItem>> network = ExtractorHelper
                    .suggestionsFor(serviceId, query)
                    .toObservable()
                    .map(strings -> {
                        List<SuggestionItem> result = new ArrayList<>();
                        for (String entry : strings) {
                            result.add(new SuggestionItem(false, entry));
                        }
                        return result;
                    });

            return Observable.zip(local, network, (localResult, networkResult) -> {

                List<SuggestionItem> result = new ArrayList<>();
                if (localResult.size() > 0) result.addAll(localResult);

                // Remove duplicates
                final Iterator<SuggestionItem> iterator = networkResult.iterator();
                while (iterator.hasNext() && localResult.size() > 0) {

                    final SuggestionItem next = iterator.next();
                    for (SuggestionItem item : localResult) {

                        if (item.query.equals(next.query)) {
                            iterator.remove();
                            break;
                        }
                    }
                }

                if (networkResult.size() > 0) result.addAll(networkResult);
                return result;
            }).materialize();
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(listNotification -> {
                    if (listNotification.isOnNext()) {
                        handleSuggestions(listNotification.getValue());
                    } else if (listNotification.isOnError()) {
                        Throwable error = listNotification.getError();
                        if (!ExtractorHelper.hasAssignableCauseThrowable(error,
                                IOException.class, SocketException.class,
                                InterruptedException.class, InterruptedIOException.class)) {
                            onSuggestionError(error);
                        }
                    }
                });
    }

    @Override
    protected void doInitialLoadLogic() {
        // no-op
    }

    @SuppressLint("CheckResult")
    private void search(final String searchString, String[] contentFilter, String sortFilter) {
        if (searchString.isEmpty()) return;

        try {
            final StreamingService service = NewPipe.getServiceByUrl(searchString);
            if (service != null) {
                showLoading();
                disposables.add(Observable.fromCallable(() -> NavigationHelper.getIntentByLink(activity, service, searchString))
                        .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(
                                // onNext
                                intent -> {
                                    getFM().popBackStackImmediate();
                                    activity.startActivity(intent);
                                },
                                // onError
                                throwable -> showError(getString(R.string.url_not_supported_toast), false)));
                return;
            }
        } catch (Exception e) {
            // Exception occurred, it's not a url
        }

        lastSearchedString = this.searchString;
        this.searchString = searchString;
        infoListAdapter.clearStreamItemList();
        hideSuggestionsPanel();
        hideKeyboardSearch();

        historyRecordManager.onSearched(serviceId, searchString).observeOn(AndroidSchedulers.mainThread()).subscribe();
        suggestionPublisher.onNext(searchString);
        startLoading(false);
    }

    @Override
    public void startLoading(boolean forceLoad) {

        super.startLoading(forceLoad);

        disposables.clear();
        if (searchDisposable != null) searchDisposable.dispose();
        searchDisposable = ExtractorHelper.searchFor(serviceId, searchString, Arrays.asList(contentFilter), sortFilter)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnEvent((searchResult, throwable) -> isLoading.set(false))
                .subscribe(this::handleResult, this::onError);

    }

    @Override
    protected void loadMoreItems() {
        if (!Page.isValid(nextPage)) return;
        isLoading.set(true);
        showListFooter(true);
        if (searchDisposable != null) searchDisposable.dispose();
        searchDisposable = ExtractorHelper.getMoreSearchItems(serviceId, searchString, asList(contentFilter), sortFilter, nextPage)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnEvent((nextItemsResult, throwable) -> isLoading.set(false))
                .subscribe(this::handleNextItems, this::onError);
    }

    @Override
    protected boolean hasMoreItems() {
        return true;
    }

    @Override
    protected void onItemSelected(InfoItem selectedItem) {
        super.onItemSelected(selectedItem);
        hideKeyboardSearch();
    }

    // Utils
    private void setQuery(int serviceId, String searchString, String[] contentFilter, String sortFilter) {
        this.serviceId = serviceId;
        this.searchString = searchString;
        this.contentFilter = contentFilter;
        this.sortFilter = sortFilter;
    }

    // Suggestion Results
    public void handleSuggestions(@NonNull final List<SuggestionItem> suggestions) {
        suggestionsRecyclerView.smoothScrollToPosition(0);
        suggestionsRecyclerView.post(() -> suggestionListAdapter.setItems(suggestions));

        if (errorPanel.getVisibility() == View.VISIBLE) {
            hideLoading();
        }
    }

    public void onSuggestionError(Throwable exception) {
        if (super.onError(exception)) return;
        int errorId = exception instanceof ParsingException ? R.string.parsing_error : R.string.general_error;
        //onUnrecoverableError(exception, UserAction.GET_SUGGESTIONS, NewPipe.getNameOfService(serviceId), searchString, errorId);
    }

    // Contract
    @Override
    public void hideLoading() {
        super.hideLoading();
        showListFooter(false);
    }

    @Override
    public void showError(String message, boolean showRetryButton) {
        super.showError(message, showRetryButton);
        hideSuggestionsPanel();
        hideKeyboardSearch();
    }

    // Search Results
    @Override
    public void handleResult(@NonNull SearchInfo result) {
        lastSearchedString = searchString;
        nextPage = result.getNextPage();

        if (infoListAdapter.getItemsList().size() == 0) {
            if (!result.getRelatedItems().isEmpty()) {
                infoListAdapter.addInfoItemList(result.getRelatedItems());
            } else {
                infoListAdapter.clearStreamItemList();
                showEmptyState();
                return;
            }
        }
        super.handleResult(result);
    }

    @Override
    public void handleNextItems(ListExtractor.InfoItemsPage result) {
        showListFooter(false);
        infoListAdapter.addInfoItemList(result.getItems());
        nextPage = result.getNextPage();

//        if (!result.getErrors().isEmpty()) {
//            showSnackBarError(result.getErrors(), UserAction.SEARCHED, NewPipe.getNameOfService(serviceId),
//                    "\"" + searchString + "\" → pageUrl: " + nextPage.getUrl() + ", "
//                            + "pageIds: " + nextPage.getIds() + ", "
//                            + "pageCookies: " + nextPage.getCookies(), 0);
//        }
        super.handleNextItems(result);
    }

    @Override
    protected boolean onError(Throwable exception) {
        if (super.onError(exception)) return true;
        infoListAdapter.clearStreamItemList();
        showEmptyState();
        return true;
    }

    private void onPopBackStack() {
        if (getFragmentManager() != null) {
            getFragmentManager().popBackStack();
            new Handler(Looper.getMainLooper()).postDelayed(() -> AppInterstitialAd.getInstance().showInterstitialAd(activity, () -> {}), 500);
        }
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
                        infoListAdapter.setHeader(null);
                    }

                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        // visible
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ReCaptchaActivity.RECAPTCHA_REQUEST && resultCode == Activity.RESULT_OK && !TextUtils.isEmpty(searchString)) {
            search(searchString, contentFilter, sortFilter);
        }
    }
}