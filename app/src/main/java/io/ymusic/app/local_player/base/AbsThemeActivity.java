package io.ymusic.app.local_player.base;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.ColorInt;

import com.kabouzeid.appthemehelper.ATH;
import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.appthemehelper.common.ATHToolbarActivity;
import com.kabouzeid.appthemehelper.util.MaterialDialogsUtil;

public abstract class AbsThemeActivity extends ATHToolbarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MaterialDialogsUtil.updateMaterialDialogsThemeSingleton(this);
    }

    public void setTaskDescriptionColor(@ColorInt int color) {
        ATH.setTaskDescriptionColor(this, color);
    }

    public void setNavigationbarColor(int color) {
        if (ThemeStore.coloredNavigationBar(this)) {
            ATH.setNavigationbarColor(this, color);
        } else {
            ATH.setNavigationbarColor(this, Color.BLACK);
        }
    }

    public void setLightStatusbar(boolean enabled) {
        ATH.setLightStatusbar(this, enabled);
    }
}
