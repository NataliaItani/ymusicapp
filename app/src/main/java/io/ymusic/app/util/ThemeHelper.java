package io.ymusic.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;

import androidx.annotation.AttrRes;
import androidx.preference.PreferenceManager;

import io.ymusic.app.R;

public class ThemeHelper {

    public static boolean themeChanged = false;

    /**
     * Apply the selected theme (on NewPipe settings) in the context
     * with the default style (see {@link #setTheme(Context)}).
     *
     * @param context context that the theme will be applied
     */
    public static void setTheme(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int styleId;
        switch (preferences.getInt(context.getString(R.string.color_theme_key), 0)) {
            case 1:
                styleId = R.style.LightTheme_Palette1;
                break;
            case 2:
                styleId = R.style.LightTheme_Palette2;
                break;
            case 3:
                styleId = R.style.LightTheme_Palette3;
                break;
            case 4:
                styleId = R.style.LightTheme_Palette4;
                break;
            case 5:
                styleId = R.style.LightTheme_Palette5;
                break;
            case 6:
                styleId = R.style.LightTheme_Palette6;
                break;
            case 7:
                styleId = R.style.LightTheme_Palette7;
                break;
            case 8:
                styleId = R.style.LightTheme_Palette8;
                break;
            case 9:
                styleId = R.style.LightTheme_Palette9;
                break;
            case 10:
                styleId = R.style.LightTheme_Palette10;
                break;
            case 11:
                styleId = R.style.LightTheme_Palette11;
                break;
            default:
                styleId = R.style.LightTheme;
                break;
        }
        context.setTheme(styleId);
    }

    public static void setTheme(Context context, int styleId) {
        context.setTheme(styleId);
        themeChanged = true;
    }

    /**
     * Get a resource id from a resource styled according to the the context's theme.
     */
    public static int resolveResourceIdFromAttr(Context context, @AttrRes int attr) {

        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{attr});
        int attributeResourceId = a.getResourceId(0, 0);
        a.recycle();
        return attributeResourceId;
    }
}
