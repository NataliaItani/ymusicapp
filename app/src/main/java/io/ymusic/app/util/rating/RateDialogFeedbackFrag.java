package io.ymusic.app.util.rating;

import android.app.Dialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.ymusic.app.BuildConfig;
import io.ymusic.app.R;

public class RateDialogFeedbackFrag extends RateDialogFrag implements View.OnClickListener {

    private static final String RATING_KEY = "rating";
    private EditText etFeedback;
    private float rating;

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
        View view = inflater.inflate(R.layout.dialog_feedback, container);
        etFeedback = view.findViewById(R.id.et_feedback);
        View bt = view.findViewById(R.id.bt_no);
        bt.setOnClickListener(this);
        bt = view.findViewById(R.id.bt_send);
        bt.setOnClickListener(this);

        if (savedInstanceState != null) {
            rating = savedInstanceState.getFloat(RATING_KEY);
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

    public void setRating(float rating) {
        this.rating = rating;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putFloat(RATING_KEY, rating);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onClick(View view) {
        String feedback = etFeedback.getText().toString();
        if (view.getId() == R.id.bt_send && feedback.length() > 0) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"info@passiatech.com"});
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " User Feedback");
            intent.putExtra(Intent.EXTRA_TEXT, "Write your message below: \n" + feedback +
                    "\n\nDiagnostic information:" +
                    "\nVersion: " + BuildConfig.VERSION_NAME +
                    "\nAndroid version: " + Build.VERSION.RELEASE +
                    "\nDevice type: " + Build.MODEL);
            if (getActivity() != null) {
                getActivity().startActivity(Intent.createChooser(intent, getString(R.string.send_email_title)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
//                RateSPManager.neverAskAgain(getContext());
            }
        } else if (view.getId() == R.id.bt_send) {
            Toast.makeText(getContext(), getString(R.string.please_enter_your_feedback), Toast.LENGTH_SHORT).show();
            return;
        } else if (view.getId() == R.id.bt_no) {
//            RateSPManager.updateTime(getContext());
        }
        dismiss();
    }
}