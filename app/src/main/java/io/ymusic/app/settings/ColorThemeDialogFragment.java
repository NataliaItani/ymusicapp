package io.ymusic.app.settings;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import butterknife.ButterKnife;
import butterknife.OnClick;
import io.ymusic.app.R;
import io.ymusic.app.util.ThemeHelper;

public class ColorThemeDialogFragment extends DialogFragment implements View.OnClickListener {

    public static final String KEY = "fragment_color_theme";

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_color_themes, container);
        ButterKnife.bind(this, view);
        setUpOnClickListeners(view);
        return view;
    }

    private void setUpOnClickListeners(View view) {
        view.findViewById(R.id.palette).setOnClickListener(this);
        view.findViewById(R.id.palette_1).setOnClickListener(this);
        view.findViewById(R.id.palette_2).setOnClickListener(this);
        view.findViewById(R.id.palette_3).setOnClickListener(this);
        view.findViewById(R.id.palette_4).setOnClickListener(this);
        view.findViewById(R.id.palette_5).setOnClickListener(this);
        view.findViewById(R.id.palette_6).setOnClickListener(this);
        view.findViewById(R.id.palette_7).setOnClickListener(this);
        view.findViewById(R.id.palette_8).setOnClickListener(this);
        view.findViewById(R.id.palette_9).setOnClickListener(this);
        view.findViewById(R.id.palette_10).setOnClickListener(this);
        view.findViewById(R.id.palette_11).setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.setCancelable(true);
            Window window = dialog.getWindow();
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    @OnClick(R.id.background)
    void onClose() {
        dismiss();
    }

    @Override
    public void onClick(View view) {

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(requireContext()).edit();
        int pos;
        switch (view.getId()) {
            case R.id.palette_1:
                pos = 1;
                ThemeHelper.setTheme(requireContext(), R.style.LightTheme_Palette1);
                break;
            case R.id.palette_2:
                pos = 2;
                ThemeHelper.setTheme(requireContext(), R.style.LightTheme_Palette2);
                break;
            case R.id.palette_3:
                pos = 3;
                ThemeHelper.setTheme(requireContext(), R.style.LightTheme_Palette3);
                break;
            case R.id.palette_4:
                pos = 4;
                ThemeHelper.setTheme(requireContext(), R.style.LightTheme_Palette4);
                break;
            case R.id.palette_5:
                pos = 5;
                ThemeHelper.setTheme(requireContext(), R.style.LightTheme_Palette5);
                break;
            case R.id.palette_6:
                pos = 6;
                ThemeHelper.setTheme(requireContext(), R.style.LightTheme_Palette6);
                break;
            case R.id.palette_7:
                pos = 7;
                ThemeHelper.setTheme(requireContext(), R.style.LightTheme_Palette7);
                break;
            case R.id.palette_8:
                pos = 8;
                ThemeHelper.setTheme(requireContext(), R.style.LightTheme_Palette8);
                break;
            case R.id.palette_9:
                pos = 9;
                ThemeHelper.setTheme(requireContext(), R.style.LightTheme_Palette9);
                break;
            case R.id.palette_10:
                pos = 10;
                ThemeHelper.setTheme(requireContext(), R.style.LightTheme_Palette10);
                break;
            case R.id.palette_11:
                pos = 11;
                ThemeHelper.setTheme(requireContext(), R.style.LightTheme_Palette11);
                break;
            default:
                pos = 0;
                ThemeHelper.setTheme(requireContext(), R.style.LightTheme);
        }
        editor.putInt(getString(R.string.color_theme_key), pos);
        editor.apply();
        dismiss();
        // This is will recreate the activity, so the changes can be visible to the user
        requireActivity().recreate();
    }
}
