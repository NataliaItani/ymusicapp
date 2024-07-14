package io.ymusic.app.util;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import io.ymusic.app.R;

public class GlideUtils {

	public static void loadAvatar(Context context, ImageView imageView, String imageUrl) {
		Glide.with(context).load(imageUrl)
				.diskCacheStrategy(DiskCacheStrategy.ALL)
				.placeholder(R.drawable.user_default)
				.error(R.drawable.user_default)
				.fallback(R.drawable.user_default)
				.centerCrop()
				.into(imageView);
	}

	public static void loadBanner(Context context, ImageView imageView, String imageUrl) {
		Glide.with(context).load(imageUrl)
				.diskCacheStrategy(DiskCacheStrategy.ALL)
				.placeholder(R.drawable.user_default)
				.error(R.drawable.user_default)
				.fallback(R.drawable.user_default)
				.into(imageView);
	}

	public static void loadThumbnail(Context context, ImageView imageView, String imageUrl) {
		Glide.with(context).load(imageUrl)
				.diskCacheStrategy(DiskCacheStrategy.ALL)
				.placeholder(R.drawable.default_image)
				.error(R.drawable.default_image)
				.fallback(R.drawable.default_image)
				.centerCrop()
				.into(imageView);
	}

	public static void loadThumbnailCircleCrop(Context context, ImageView imageView, String imageUrl) {
		Glide.with(context).load(imageUrl)
				.diskCacheStrategy(DiskCacheStrategy.ALL)
				.placeholder(R.drawable.default_image)
				.error(R.drawable.default_image)
				.fallback(R.drawable.default_image)
				.centerCrop()
				.into(imageView);
	}

}
