package io.ymusic.app.updates;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.fragment.app.DialogFragment;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.ymusic.app.BuildConfig;
import io.ymusic.app.R;
import io.ymusic.app.util.NavigationHelper;

public class UpdateDialogFrag extends DialogFragment {

    @BindView(R.id.current_version)
    TextView currentVersion;
    @BindView(R.id.new_version)
    TextView newVersion;
    @BindView(R.id.btn_later)
    LinearLayoutCompat btnLater;

    private Context context;
    private Version version;
    private boolean playstoreUpdated;
    private String appLink;
    private boolean cancelable;

    public static UpdateDialogFrag getInstance(Context context, Version version, boolean playstoreUpdated, String appLink, boolean cancelable) {
        UpdateDialogFrag dialog = new UpdateDialogFrag();
        dialog.setContext(context);
        dialog.setVersion(version);
        dialog.setPlaystoreUpdated(playstoreUpdated);
        dialog.setAppLink(appLink);
        dialog.setCancelable(cancelable);
        return dialog;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public void setPlaystoreUpdated(boolean playstoreUpdated) {
        this.playstoreUpdated = playstoreUpdated;
    }

    public void setAppLink(String appLink) {
        this.appLink = appLink;
    }

    public void setCancelable(boolean cancelable) {
        this.cancelable = cancelable;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_check_updates, container);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        currentVersion.setText(String.format(Locale.getDefault(), context.getString(R.string.current_version), BuildConfig.VERSION_NAME));
        newVersion.setText(String.format(Locale.getDefault(), context.getString(R.string.new_version), version.getVersion()));
        btnLater.setVisibility(cancelable ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.setCancelable(cancelable);
            Window window = dialog.getWindow();
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    @OnClick(R.id.btn_update_now)
    void onUpdateNow() {
        dismiss();
        UpdateSPManager.updateTime(context);
        if (playstoreUpdated) {
            NavigationHelper.openGooglePlayStore(context, BuildConfig.APPLICATION_ID);
        } else {
            UpdateApp updateApp = new UpdateApp();
            updateApp.setContext((Activity) context);
            updateApp.execute(appLink);
        }
    }

    @OnClick(R.id.btn_later)
    void onMaybeLater() {
        dismiss();
        UpdateSPManager.updateTime(context);
    }
}
