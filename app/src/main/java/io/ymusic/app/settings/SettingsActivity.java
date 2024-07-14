package io.ymusic.app.settings;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import butterknife.ButterKnife;
import io.ymusic.app.R;
import io.ymusic.app.local_player.base.AbsThemeActivity;
import io.ymusic.app.util.ThemeHelper;

public class SettingsActivity extends AbsThemeActivity implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    public enum Extra {LOGIN}

    /*private ProgressDialog mPDialog;*/

    @Override
    protected void onCreate(Bundle savedInstanceBundle) {
        ThemeHelper.setTheme(this);
        super.onCreate(savedInstanceBundle);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);

        hideSystemUi();

        Toolbar toolbar = findViewById(R.id.default_toolbar);
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
        setSupportActionBar(toolbar);

        if (savedInstanceBundle == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_holder, new MainSettingsFragment()).commit();
        }

        /*mPDialog = new ProgressDialog(this);
        mPDialog.setMessage(getString(R.string.checking_the_new_version));
        mPDialog.setIndeterminate(true);
        mPDialog.setCancelable(false);*/
    }

    private void hideSystemUi() {
        int visibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getWindow().getDecorView().setSystemUiVisibility(visibility);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        /*MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_settings, menu);*/

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(R.string.action_settings);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // end here
            if (ThemeHelper.themeChanged) {
                ThemeHelper.themeChanged = false;
                return super.onOptionsItemSelected(item);
            }
            finish();
        } /*else if (item.getItemId() == R.id.menu_item_check_updates) {
            // Show progress dialog
            mPDialog.show();

            // create FirebaseRemoteConfig
            FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();

            // init firebase remote config
            remoteConfig.setConfigSettingsAsync(new FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(0)
                    .build());
            // fetch data from FirebaseRemoteConfig
            remoteConfig.fetchAndActivate().addOnSuccessListener(this, success -> {
                // Dismiss progress dialog
                mPDialog.dismiss();
                // get app version
                String _version = remoteConfig.getString("app_version");
                // get playstore updated
                boolean playstoreUpdated = remoteConfig.getBoolean("playstore_updated");
                // get app link (APK file)
                String appLink = remoteConfig.getString("app_link");

                // Create a new version
                Version version = new Version(_version);

                // Check if can show update dialog
                if (UpdateSPManager.canShowDialog(this, version)) {
                    UpdateDialogFrag dialog = UpdateDialogFrag.getInstance(this, version, playstoreUpdated, appLink, true);
                    dialog.setCancelable(true);
                    dialog.show(getSupportFragmentManager(), UpdateDialogFrag.class.getSimpleName());
                } else {
                    Toast.makeText(this, getString(R.string.you_are_download_the_latest_version), Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(this, e -> {
                // Dismiss progress dialog
                mPDialog.dismiss();
                Toast.makeText(SettingsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }*/
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (ThemeHelper.themeChanged) {
            ThemeHelper.themeChanged = false;
            NavUtils.navigateUpFromSameTask(this);
        }
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference preference) {
        Fragment fragment = Fragment.instantiate(this, preference.getFragment(), preference.getExtras());
        getSupportFragmentManager().beginTransaction().setCustomAnimations(R.animator.custom_fade_in, R.animator.custom_fade_out, R.animator.custom_fade_in, R.animator.custom_fade_out)
                .replace(R.id.fragment_holder, fragment)
                .addToBackStack(null)
                .commit();
        return true;
    }
}
