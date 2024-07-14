package io.ymusic.app.api;

import android.util.Log;

import com.google.common.base.Joiner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Response;

public class Lyrics {

    public static Single<String> getLyrics(String artist, String title) {
        return Single.fromCallable(() -> {
            // scrap Lyrics
            String lyrics = getMusixMatchLyrics(artist, title);
            if (lyrics.equals("")) {
                lyrics = getGoogleLyrics(artist, title);
            }
            return lyrics;
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private static String getGoogleLyrics(String artist, String title) throws IOException {
        String _url = "https://www.google.com/search?client=safari&rls=en&ie=UTF-8&oe=UTF-8&q=";
        String delimiter1 = "</div></div></div></div><div class=\"hwc\"><div class=\"BNeawe tAd8D AP7Wnd\"><div><div class=\"BNeawe tAd8D AP7Wnd\">";
        String delimiter2 = "</div></div></div></div></div><div><span class=\"hwc\"><div class=\"BNeawe uEec3 AP7Wnd\">";
        String lyrics = "";
        Response response;
        String url = getProperUrl(_url + title + " by " + artist + " lyrics");
        for (int i = 0; i <= 3; i++) {
            response = OKHttp.getInstance().newCall(OKHttp.buildRequest(url)).execute();
            if (response.isSuccessful()) {
                String[] arrLyrics = Objects.requireNonNull(response.body()).string().split(delimiter1);
                lyrics = arrLyrics[arrLyrics.length - 1];
                lyrics = lyrics.split(delimiter2)[0];
                if (lyrics.contains("<meta charset=\"UTF-8\">")) {
                    if (i == 0) {
                        url = getProperUrl(_url + title + " by " + artist + " song lyrics");
                    } else if (i == 1) {
                        url = getProperUrl(_url + title.split("-")[0] + " by " + artist + " lyrics");
                    } else {
                        lyrics = "";
                        break;
                    }
                    continue;
                }
                break;
            }
        }
        return lyrics.trim();
    }


    private static String getMusixMatchLyrics(String artist, String title) {
        try {
            String link = getLyricsLink(title, artist);
            return scrapLink(link);
        } catch (IOException e) {
            return "";
        }
    }


    private static String getLyricsLink(String song, String artist) throws IOException {
        String authority = "https://www.musixmatch.com";
        String unencodedPath = "/search/" + song + " " + artist;
        Response response = OKHttp.getInstance().newCall(OKHttp.buildRequest(getProperUrl(authority + unencodedPath))).execute();
        if (response.isSuccessful()) {
            Matcher matcher = Pattern.compile("href=\"(/lyrics/.*?)\"").matcher(Objects.requireNonNull(response.body()).string());
            if (matcher.find()) {
                Log.d("TAG", "getMusicMatch: Found something " + matcher.group(1));
                return matcher.group(1);
            }
        }
        return "";
    }


    private static String scrapLink(String unencodedPath) throws IOException {
        String authority = "https://www.musixmatch.com";
        Response response = OKHttp.getInstance().newCall(OKHttp.buildRequest(getProperUrl(authority + unencodedPath))).execute();
        if (response.isSuccessful()) {
            List<String> lyrics = new ArrayList<>();
            Matcher matcher = Pattern.compile("<span class=\"lyrics__content__ok\">(.*?)</span>",
                    Pattern.CASE_INSENSITIVE).matcher(Objects.requireNonNull(response.body()).string());
            while (matcher.find()) {
                lyrics.add(matcher.group(1));
            }
            return lyrics.isEmpty() ? "" : Joiner.on("\n").join(lyrics);
        }
        return "";
    }

    private static String getProperUrl(String url) {
        return url.trim().replace(" ", "%20");
    }

}
