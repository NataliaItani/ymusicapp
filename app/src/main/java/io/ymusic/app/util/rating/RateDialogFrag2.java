package io.ymusic.app.util.rating;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.RatingBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import io.ymusic.app.BuildConfig;
import io.ymusic.app.R;

public class RateDialogFrag2 extends DialogFragment implements RatingBar.OnRatingBarChangeListener {

    public static final String KEY = "fragment_rate";

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
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_rate2, container);
        RatingBar rbStars = view.findViewById(R.id.rb_stars);
        rbStars.setOnRatingBarChangeListener(this);

        Button btnCancel = view.findViewById(R.id.btn_cancel);

        btnCancel.setOnClickListener(v -> dismiss());

        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    @Override
    public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
        if (rating >= 4) {
            String packageName = BuildConfig.APPLICATION_ID;
            Intent intent;
            try {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
                startActivity(intent);
//                    RateSPManager.neverAskAgain(getContext());
            } catch (android.content.ActivityNotFoundException anfe) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
                startActivity(intent);
//                    RateSPManager.neverAskAgain(getContext());
            }
            dismiss();
        } else if (rating > 0) {
            RateDialogManager.showRateDialogFeedback(getActivity(), rating);
            dismiss();
        }
    }
}