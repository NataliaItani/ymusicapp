package io.ymusic.app.util.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import io.ymusic.app.R;

public class FullScreenLoadingDialog {

    private final Dialog dialog;

    public FullScreenLoadingDialog(Context context) {
        dialog = new Dialog(context);
        dialog.setContentView(R.layout.layout_dialog_fullscreen);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setCancelable(false);
    }

    public void show() {
        dialog.show();
    }

    public void cancel() {
        dialog.cancel();
    }

    public boolean isShowing() {
        return dialog.isShowing();
    }
}
