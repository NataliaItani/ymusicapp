package io.ymusic.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;
import androidx.preference.PreferenceManager;

import org.schabi.newpipe.extractor.localization.ContentCountry;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import io.ymusic.app.R;

public class Localization {

    public final static String DOT_SEPARATOR = " • ";

    private Localization() {
    }

    @NonNull
    public static String concatenateStrings(final String... strings) {
        return concatenateStrings(Arrays.asList(strings));
    }

    @NonNull
    public static String concatenateStrings(final List<String> strings) {
        if (strings.isEmpty()) return "";

        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(strings.get(0));

        for (int i = 1; i < strings.size(); i++) {
            final String string = strings.get(i);
            if (!TextUtils.isEmpty(string)) {
                stringBuilder.append(DOT_SEPARATOR).append(strings.get(i));
            }
        }

        return stringBuilder.toString();
    }

    public static org.schabi.newpipe.extractor.localization.Localization getAppLocalization(Context context) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String languageCode = sharedPreferences.getString(Constants.LANGUAGE_CODE, Locale.getDefault().getLanguage());

        return org.schabi.newpipe.extractor.localization.Localization.fromLocalizationCode(languageCode);
    }

    public static ContentCountry getAppCountry(final Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String countryCode = sharedPreferences.getString(Constants.COUNTRY_CODE, Locale.getDefault().getCountry());
        return new ContentCountry(countryCode);
    }

    public static Locale getAppLocale(Context context) {

        if (context != null) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            String languageCode = sharedPreferences.getString(Constants.LANGUAGE_CODE, Locale.getDefault().getLanguage());
            return new Locale(languageCode);
        }
        return new Locale(Locale.getDefault().getLanguage());
    }

    public static String localizeSubscribersCount(Context context, long subscriberCount) {
        return getQuantity(context, R.plurals.subscribers, R.string.no_subscribers, subscriberCount, localizeNumber(context, subscriberCount));
    }

    public static String shortCount(Context context, long count) {
        if (count >= 1000000000) {
            return count / 1000000000 + context.getString(R.string.short_billion);
        } else if (count >= 1000000) {
            return count / 1000000 + context.getString(R.string.short_million);
        } else if (count >= 1000) {
            return count / 1000 + context.getString(R.string.short_thousand);
        } else {
            return Long.toString(count);
        }
    }

    public static String shortSubscriberCount(Context context, long subscriberCount) {
        return getQuantity(context, R.plurals.subscribers, R.string.no_subscribers, subscriberCount, shortCount(context, subscriberCount));
    }

    private static String getQuantity(Context context, @PluralsRes int pluralId, @StringRes int zeroCaseStringId, long count, String formattedCount) {
        if (count == 0) return context.getString(zeroCaseStringId);

        // As we use the already formatted count, is not the responsibility of this method handle long numbers
        // (it probably will fall in the "other" category, or some language have some specific rule... then we have to change it)
        int safeCount = count > Integer.MAX_VALUE ? Integer.MAX_VALUE : count < Integer.MIN_VALUE ? Integer.MIN_VALUE : (int) count;
        return context.getResources().getQuantityString(pluralId, safeCount, formattedCount);
    }

    public static String localizeStreamCount(Context context, long streamCount) {
        return getQuantity(context, R.plurals.tracks, R.string.no_tracks, streamCount, localizeNumber(context, streamCount));
    }

    public static String localizeNumber(Context context, long number) {
        Locale locale = new Locale("en");
        NumberFormat nf = NumberFormat.getInstance(locale);
        return nf.format(number);
    }
}
