package io.ymusic.app.fragments.trending;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.ymusic.app.R;
import io.ymusic.app.base.BaseFragment;
import io.ymusic.app.util.AppUtils;
import io.ymusic.app.util.NavigationHelper;
import io.ymusic.app.util.ServiceHelper;

public class TrendingFragment extends BaseFragment {

    public static TrendingFragment getInstance() {
        return new TrendingFragment();
    }

    @BindView(R.id.tabLayout)
    TabLayout tabLayout;
    @BindView(R.id.viewPager)
    ViewPager viewPager;
    @BindView(R.id.status_bar) View statusBarView;

    private TrendingViewPagerAdapter adapter;

//    @BindView(R.id.user_avatar)
//    CircleImageView userAvatar;
//    private GoogleSignInClient mGoogleSignInClient;
//    private FirebaseAuth mAuth;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new TrendingViewPagerAdapter(getChildFragmentManager(), activity);

//        // Configure sign-in to request the user's ID, email address, and basic
//        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
//        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                .requestIdToken(getString(R.string.client_id))
//                .requestEmail()
//                .build();
//
//        // Build a GoogleSignInClient with the options specified by gso.
//        mGoogleSignInClient = GoogleSignIn.getClient(activity, gso);
//
//        // Initialize Firebase Auth
//        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
//        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(activity);
//        updateUI(account);
    }

//    public void updateUI(GoogleSignInAccount account) {
//        if (account != null) {
//            Glide.with(activity).load(account.getPhotoUrl())
//                    .diskCacheStrategy(DiskCacheStrategy.ALL)
//                    .placeholder(R.drawable.ic_account_circle_white_24dp)
//                    .error(R.drawable.ic_account_circle_white_24dp)
//                    .fallback(R.drawable.ic_account_circle_white_24dp)
//                    .into(userAvatar);
//
//            AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
//            mAuth.signInWithCredential(credential)
//                    .addOnCompleteListener(activity, task -> {
//                    });
//        } else {
//            Glide.with(activity).load(R.drawable.ic_account_circle_white_24dp)
//                    .diskCacheStrategy(DiskCacheStrategy.ALL)
//                    .placeholder(R.drawable.ic_account_circle_white_24dp)
//                    .error(R.drawable.ic_account_circle_white_24dp)
//                    .fallback(R.drawable.ic_account_circle_white_24dp)
//                    .into(userAvatar);
//        }
//    }
//
//    @OnClick(R.id.user_avatar)
//    void onUserAvatarClicked() {
//        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(activity);
//        if (account != null) {
//            AccountInfoDialog accountInfoDialog = AccountInfoDialog.getInstance(account, mGoogleSignInClient, () -> {
//                updateUI(null);
//                SharedPrefsHelper.removePrefs(activity, SharedPrefsHelper.Key.USER_LOGIN_EMAIL.name());
//            });
//            accountInfoDialog.show(getFM(), AccountInfoDialog.class.getSimpleName());
//        } else {
//            boolean showedLoginDialog = SharedPrefsHelper.getBooleanPrefs(activity, SharedPrefsHelper.Key.SHOWED_LOGIN_DIALOG.name());
//            if (showedLoginDialog) {
//                signIn();
//            } else {
//                DialogUtils.showLoginDialog(activity, (dialogInterface, i) -> signIn(), (dialogInterface, i) -> dialogInterface.dismiss());
//            }
//        }
//    }
//
//    private void signIn() {
//        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
//        mStartForResult.launch(signInIntent);
//    }
//
//    ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
//        if (result.getResultCode() == Activity.RESULT_OK) {
//            Intent data = result.getData();
//            // Handle the Intent
//            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
//            handleSignInResult(task);
//        }
//        if (result.getResultCode() == Activity.RESULT_CANCELED) {
//            Intent data = result.getData();
//            // Handle the Intent
//            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
//            handleSignInResult(task);
//        }
//    });
//
//    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
//        try {
//            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
//            // Save user's email logged in
//            SharedPrefsHelper.setStringPrefs(activity, SharedPrefsHelper.Key.USER_LOGIN_EMAIL.name(), account.getEmail());
//            SharedPrefsHelper.setBooleanPrefs(activity, SharedPrefsHelper.Key.SHOWED_LOGIN_DIALOG.name(), true);
//            // Signed in successfully, show authenticated UI.
//            updateUI(account);
//        } catch (ApiException e) {
//            // The ApiException status code indicates the detailed failure reason.
//            // Please refer to the GoogleSignInStatusCodes class reference for more information.
//            updateUI(null);
//        }
//    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trending, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    protected void initViews(View rootView, Bundle savedInstanceState) {
        super.initViews(rootView, savedInstanceState);
        AppUtils.setStatusBarHeight(activity, statusBarView);
        initAdapter();
    }

    private void initAdapter() {
        // set adapter to viewPager
        viewPager.setAdapter(adapter);
        // setup tabLayout with viewPager
        tabLayout.setupWithViewPager(viewPager);
    }

    @OnClick(R.id.action_search)
    void onSearch() {
        // open search
        NavigationHelper.openSearchFragment(getFM(), ServiceHelper.getSelectedServiceId(activity), "");
    }

    @OnClick(R.id.action_settings)
    void onSettings() {
        // open Settings
        NavigationHelper.openSettings(activity);
    }
}
