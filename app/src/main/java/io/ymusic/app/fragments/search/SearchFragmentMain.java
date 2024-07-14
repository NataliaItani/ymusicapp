package io.ymusic.app.fragments.search;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.annimon.stream.Stream;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipDrawable;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.ymusic.app.R;
import io.ymusic.app.base.BaseFragment;
import io.ymusic.app.util.AppUtils;
import io.ymusic.app.util.NavigationHelper;
import io.ymusic.app.util.ServiceHelper;

public class SearchFragmentMain extends BaseFragment {
	
	public static SearchFragmentMain getInstance() {
		return new SearchFragmentMain();
	}

	@BindView(R.id.status_bar) View statusBarView;
	@BindView(R.id.chip_group) ChipGroup chipGroup;

	private DatabaseReference mDatabase;

//	@BindView(R.id.user_avatar)
//	CircleImageView userAvatar;
//	private GoogleSignInClient mGoogleSignInClient;
//	private FirebaseAuth mAuth;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDatabase = FirebaseDatabase.getInstance().getReference();
//
//		// Configure sign-in to request the user's ID, email address, and basic
//		// profile. ID and basic profile are included in DEFAULT_SIGN_IN.
//		GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//				.requestIdToken(getString(R.string.client_id))
//				.requestEmail()
//				.build();
//
//		// Build a GoogleSignInClient with the options specified by gso.
//		mGoogleSignInClient = GoogleSignIn.getClient(activity, gso);
//
//		// Initialize Firebase Auth
//		mAuth = FirebaseAuth.getInstance();
	}

	@Override
	public void onStart() {
		super.onStart();
		// Check for existing Google Sign In account, if the user is already signed in
		// the GoogleSignInAccount will be non-null.
//		GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(activity);
//		updateUI(account);
	}

//	public void updateUI(GoogleSignInAccount account) {
//		if (account != null) {
//			Glide.with(activity).load(account.getPhotoUrl())
//					.diskCacheStrategy(DiskCacheStrategy.ALL)
//					.placeholder(R.drawable.ic_account_circle_white_24dp)
//					.error(R.drawable.ic_account_circle_white_24dp)
//					.fallback(R.drawable.ic_account_circle_white_24dp)
//					.into(userAvatar);
//
//			AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
//			mAuth.signInWithCredential(credential)
//					.addOnCompleteListener(activity, task -> {
//					});
//		} else {
//			Glide.with(activity).load(R.drawable.ic_account_circle_white_24dp)
//					.diskCacheStrategy(DiskCacheStrategy.ALL)
//					.placeholder(R.drawable.ic_account_circle_white_24dp)
//					.error(R.drawable.ic_account_circle_white_24dp)
//					.fallback(R.drawable.ic_account_circle_white_24dp)
//					.into(userAvatar);
//		}
//	}
//
//	@OnClick(R.id.user_avatar)
//	void onUserAvatarClicked() {
//		GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(activity);
//		if (account != null) {
//			AccountInfoDialog accountInfoDialog = AccountInfoDialog.getInstance(account, mGoogleSignInClient, () -> {
//				updateUI(null);
//				SharedPrefsHelper.removePrefs(activity, SharedPrefsHelper.Key.USER_LOGIN_EMAIL.name());
//			});
//			accountInfoDialog.show(getFM(), AccountInfoDialog.class.getSimpleName());
//		} else {
//			boolean showedLoginDialog = SharedPrefsHelper.getBooleanPrefs(activity, SharedPrefsHelper.Key.SHOWED_LOGIN_DIALOG.name());
//			if (showedLoginDialog) {
//				signIn();
//			} else {
//				DialogUtils.showLoginDialog(activity, (dialogInterface, i) -> signIn(), (dialogInterface, i) -> dialogInterface.dismiss());
//			}
//		}
//	}
//
//	private void signIn() {
//		Intent signInIntent = mGoogleSignInClient.getSignInIntent();
//		mStartForResult.launch(signInIntent);
//	}
//
//	ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
//		if (result.getResultCode() == Activity.RESULT_OK) {
//			Intent data = result.getData();
//			// Handle the Intent
//			Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
//			handleSignInResult(task);
//		}
//		if (result.getResultCode() == Activity.RESULT_CANCELED) {
//			Intent data = result.getData();
//			// Handle the Intent
//			Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
//			handleSignInResult(task);
//		}
//	});
//
//	private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
//		try {
//			GoogleSignInAccount account = completedTask.getResult(ApiException.class);
//			// Save user's email logged in
//			SharedPrefsHelper.setStringPrefs(activity, SharedPrefsHelper.Key.USER_LOGIN_EMAIL.name(), account.getEmail());
//			SharedPrefsHelper.setBooleanPrefs(activity, SharedPrefsHelper.Key.SHOWED_LOGIN_DIALOG.name(), true);
//			// Signed in successfully, show authenticated UI.
//			updateUI(account);
//		} catch (ApiException e) {
//			// The ApiException status code indicates the detailed failure reason.
//			// Please refer to the GoogleSignInStatusCodes class reference for more information.
//			updateUI(null);
//		}
//	}
	
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_search_main, container, false);
		ButterKnife.bind(this, view);
		return view;
	}
	
	@Override
	protected void initViews(View rootView, Bundle savedInstanceState) {
		super.initViews(rootView, savedInstanceState);
		initSuggestionKeywords();
		AppUtils.setStatusBarHeight(activity, statusBarView);
	}
	
	private void initSuggestionKeywords() {
		DatabaseReference dataRef = mDatabase.child("data");
		Query query = dataRef.orderByKey();
		query.addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				chipGroup.removeAllViews();
				List<String> keywords = new ArrayList<>();
				List<DataSnapshot> dataSnapshots = Stream.of(snapshot.getChildren()).toList();
				if (!dataSnapshots.isEmpty()) {
					Stream.of(dataSnapshots).forEach(dataSnapshot -> keywords.add(dataSnapshot.getValue().toString()));
				}
				createChipList(keywords);
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {
				Log.d("onCancelled", error.getDetails());
			}
		});
	}

	private void createChipList(List<String> keywords) {
		Stream.of(keywords).forEach(tag -> {
			final Chip chip = new Chip(activity);
			// style
			ChipDrawable drawable = ChipDrawable.createFromAttributes(activity, null, 0, R.style.Widget_MaterialComponents_Chip_Action);
			chip.setChipDrawable(drawable);
			chip.setTextColor(ContextCompat.getColor(activity, R.color.white));
			chip.setChipBackgroundColorResource(R.color.youtube_primary_color);
			// name
			chip.setText(tag);
			// add child chip to group
			chipGroup.addView(chip);

			chip.setOnClickListener(view -> NavigationHelper.openSearchFragment(getFragmentManager(), ServiceHelper.getSelectedServiceId(activity), (String) chip.getText()));
		});
	}

	@Override
	public void onResume() {
		super.onResume();
		//checkAppUpdates();
	}

	/*private void checkAppUpdates() {
		// create FirebaseRemoteConfig
		FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();

		// init firebase remote config
		remoteConfig.setConfigSettingsAsync(new FirebaseRemoteConfigSettings.Builder()
				.setMinimumFetchIntervalInSeconds(0)
				.build());
		// fetch data from FirebaseRemoteConfig
		remoteConfig.fetchAndActivate().addOnSuccessListener(activity, success -> {
			// get app version
			String _version = remoteConfig.getString("app_version");
			// get playstore updated
			boolean playstoreUpdated = remoteConfig.getBoolean("playstore_updated");
			// get app link (APK file)
			String appLink = remoteConfig.getString("app_link");

			// Create a new version
			Version version = new Version(_version);

			// Check if can show update dialog
			if (UpdateSPManager.canShowDialog(activity, version)) {
				UpdateDialogFrag dialog = UpdateDialogFrag.getInstance(activity, version, playstoreUpdated, appLink, false);
				dialog.setCancelable(false);
				if (!dialog.isVisible()) {
					dialog.show(getFM(), UpdateDialogFrag.class.getSimpleName());
				}
			}
		});
	}*/

	@OnClick(R.id.search_view)
	void onSearch() {
		NavigationHelper.openSearchFragment(getFragmentManager(), ServiceHelper.getSelectedServiceId(activity), "");
	}

	@OnClick(R.id.action_search)
	void onSearch2() {
		// open search
		NavigationHelper.openSearchFragment(getFM(), ServiceHelper.getSelectedServiceId(activity), "");
	}

	@OnClick(R.id.action_settings)
	void onSettings() {
		// open Settings
		NavigationHelper.openSettings(activity);
	}
}
