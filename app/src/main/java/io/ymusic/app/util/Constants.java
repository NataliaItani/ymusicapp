package io.ymusic.app.util;

import okhttp3.OkHttpClient;

public class Constants {
	
	public static final String KEY_SERVICE_ID = "key_service_id";
	public static final String KEY_URL = "key_url";
	public static final String KEY_TITLE = "key_title";
	public static final String KEY_LINK_TYPE = "key_link_type";
	public static final String KEY_OPEN_SEARCH = "key_open_search";
	public static final String KEY_SEARCH_STRING = "key_search_string";

	public static final String COUNTRY_CODE = "country_code";
	public static final String LANGUAGE_CODE = "language_code";

	public final static String PLAYBACK_TIME_DEFAULT = "00:00";
	
	// shared connection pool
	private final OkHttpClient okHttpClient;
	
	// singleton
	private static Constants myself;
	
	public synchronized static Constants getInstance() {
		
		if (myself == null) {
			myself = new Constants();
		}
		return myself;
	}
	
	private Constants() {
		
		// connection pool
		okHttpClient = new OkHttpClient();
	}
	
	public OkHttpClient.Builder getOkHttpBuilder() {
		return okHttpClient.newBuilder();
	}
}
