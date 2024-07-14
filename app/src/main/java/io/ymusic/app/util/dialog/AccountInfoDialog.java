package io.ymusic.app.util.dialog;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;
import io.ymusic.app.R;

public class AccountInfoDialog extends DialogFragment {

    @BindView(R.id.avatar)
    CircleImageView avatar;
    @BindView(R.id.account_name)
    TextView accountName;
    @BindView(R.id.account_email)
    TextView accountEmail;

    private GoogleSignInAccount account;
    private GoogleSignInClient mGoogleSignInClient;
    private Callback signOutCallback;

    public static AccountInfoDialog getInstance(GoogleSignInAccount account,
                                                GoogleSignInClient mGoogleSignInClient,
                                                Callback signOutCallback) {
        AccountInfoDialog dialog = new AccountInfoDialog();
        dialog.setGoogleSignInAccount(account);
        dialog.setGoogleSignInClient(mGoogleSignInClient);
        dialog.setSignOutCallback(signOutCallback);
        dialog.setCancelable(true);
        return dialog;
    }

    private void setGoogleSignInAccount(GoogleSignInAccount account) {
        this.account = account;
    }

    private void setGoogleSignInClient(GoogleSignInClient mGoogleSignInClient) {
        this.mGoogleSignInClient = mGoogleSignInClient;
    }

    private void setSignOutCallback(Callback signOutCallback) {
        this.signOutCallback = signOutCallback;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_account_info, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (account != null) {
            accountName.setText(account.getDisplayName());
            accountEmail.setText(account.getEmail());
            Glide.with(getContext()).load(account.getPhotoUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_account_circle_white_24dp)
                    .error(R.drawable.ic_account_circle_white_24dp)
                    .fallback(R.drawable.ic_account_circle_white_24dp)
                    .into(avatar);
        }
    }

    @OnClick(R.id.sign_out)
    void onSignOut() {
        mGoogleSignInClient.signOut().addOnCompleteListener(getActivity(), task -> {
            signOutCallback.onSignOutCallback();
            dismiss();
        });
    }

    @OnClick(R.id.close)
    void onClose() {
        dismiss();
    }

    @Override
    public void onResume() {
        super.onResume();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        }
    }

    public interface Callback {
        void onSignOutCallback();
    }
}