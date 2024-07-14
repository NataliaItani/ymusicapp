package io.ymusic.app.settings;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceFragmentCompat;

import butterknife.ButterKnife;
import io.ymusic.app.BuildConfig;
import io.ymusic.app.R;
import io.ymusic.app.util.NavigationHelper;
import io.ymusic.app.util.SharedUtils;
import io.ymusic.app.util.rating.RateDialogFrag2;

public class MainSettingsFragment extends PreferenceFragmentCompat {

//    private GoogleSignInClient mGoogleSignInClient;
//    private FirebaseAuth mAuth;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        // Configure sign-in to request the user's ID, email address, and basic
//        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
//        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                .requestIdToken(getString(R.string.client_id))
//                .requestEmail()
//                .build();
//
//        // Build a GoogleSignInClient with the options specified by gso.
//        mGoogleSignInClient = GoogleSignIn.getClient(getActivity(), gso);
//
//        // Initialize Firebase Auth
//        mAuth = FirebaseAuth.getInstance();
//
//        updateUI(GoogleSignIn.getLastSignedInAccount(getActivity()));

//        // sign up
//        findPreference(getString(R.string.sign_up_key)).setOnPreferenceClickListener(preference -> {
//            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getActivity());
//            if (account != null) {
//                AccountInfoDialog accountInfoDialog = AccountInfoDialog.getInstance(account, mGoogleSignInClient, () -> {
//                    updateUI(null);
//                    SharedPrefsHelper.removePrefs(getActivity(), SharedPrefsHelper.Key.USER_LOGIN_EMAIL.name());
//                });
//                accountInfoDialog.show(getChildFragmentManager(), AccountInfoDialog.class.getSimpleName());
//            } else {
//                boolean showedLoginDialog = SharedPrefsHelper.getBooleanPrefs(getActivity(), SharedPrefsHelper.Key.SHOWED_LOGIN_DIALOG.name());
//                if (showedLoginDialog) {
//                    signIn();
//                } else {
//                    DialogUtils.showLoginDialog(getActivity(), (dialogInterface, i) -> signIn(), (dialogInterface, i) -> dialogInterface.dismiss());
//                }
//            }
//            return true;
//        });

        // dark theme
        findPreference(getString(R.string.dark_theme_key)).setOnPreferenceChangeListener((preference, newValue) -> {
            if ((boolean) newValue) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            return true;
        });

        // rate app
        findPreference(getString(R.string.color_theme_key)).setOnPreferenceClickListener(preference -> {
            ColorThemeDialogFragment dialog = new ColorThemeDialogFragment();
            dialog.setCancelable(true);
            dialog.show(getActivity().getSupportFragmentManager(), ColorThemeDialogFragment.KEY);
            return true;
        });

        // rate app
        findPreference(getString(R.string.rate_me_now)).setOnPreferenceClickListener(preference -> {
            RateDialogFrag2 dialog = new RateDialogFrag2();
            dialog.setCancelable(false);
            dialog.show(getActivity().getSupportFragmentManager(), RateDialogFrag2.KEY);
            return true;
        });

        // share
        findPreference(getString(R.string.tell_your_friend)).setOnPreferenceClickListener(preference -> {
            SharedUtils.shareUrl(getContext());
            return true;
        });

        // feedback
        findPreference(getString(R.string.feedback)).setOnPreferenceClickListener(preference -> {
            NavigationHelper.composeEmail(getContext(), getString(R.string.app_name) + " Android Feedback");
            return true;
        });

        // telegram
        findPreference(getString(R.string.telegram))
                .setOnPreferenceClickListener(preference -> {
            requireContext().startActivity(
                    new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/videmaktech"))
            );
            return true;
        });
    }

//    private void signIn() {
//        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
//        mStartForResult.launch(signInIntent);
//    }

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

//    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
//        try {
//            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
//            // Save user's email logged in
//            SharedPrefsHelper.setStringPrefs(getActivity(), SharedPrefsHelper.Key.USER_LOGIN_EMAIL.name(), account.getEmail());
//            SharedPrefsHelper.setBooleanPrefs(getActivity(), SharedPrefsHelper.Key.SHOWED_LOGIN_DIALOG.name(), true);
//            // Signed in successfully, show authenticated UI.
//            updateUI(account);
//        } catch (ApiException e) {
//            // The ApiException status code indicates the detailed failure reason.
//            // Please refer to the GoogleSignInStatusCodes class reference for more information.
//            updateUI(null);
//        }
//    }

//    private void updateUI(GoogleSignInAccount account) {
//        if (account != null) {
//            AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
//            mAuth.signInWithCredential(credential)
//                    .addOnCompleteListener(getActivity(), task -> {
//                    });
//
//            findPreference(getString(R.string.sign_up_key)).setTitle(account.getDisplayName());
//            findPreference(getString(R.string.sign_up_key)).setSummary(account.getEmail());
//        } else {
//            findPreference(getString(R.string.sign_up_key)).setTitle(R.string.sign_up);
//            findPreference(getString(R.string.sign_up_key)).setSummary(null);
//        }
//    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.app_settings);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        // remove divider
        setDivider(null);
        // versionName
        initVersion(getActivity());
    }

    private void initVersion(Activity activity) {
        boolean debug = (0 != (activity.getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE));
        String versionName = BuildConfig.VERSION_NAME + "-" + (debug ? "DEBUG" : "PRODUCTION");
        findPreference(getString(R.string.version_name)).setSummary(versionName);
    }
}
