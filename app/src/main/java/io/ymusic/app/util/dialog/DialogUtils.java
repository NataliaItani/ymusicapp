package io.ymusic.app.util.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import io.ymusic.app.R;

public class DialogUtils {
	
	public static void showDeleteDialog(Context context, String songTitle, DialogInterface.OnClickListener positiveListener) {
		CharSequence content = Html.fromHtml(context.getString(R.string.delete_song_x, songTitle));
		new MaterialAlertDialogBuilder(context)
				.setTitle(R.string.delete)
				.setMessage(content)
				.setCancelable(false)
				.setPositiveButton(R.string.ok, positiveListener)
				.setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss())
				.show();
	}
	
	public static void showDeleteAllDialog(Context context, DialogInterface.OnClickListener positiveListener) {
		new MaterialAlertDialogBuilder(context)
				.setTitle(R.string.delete)
				.setMessage(R.string.clear_all_msg)
				.setCancelable(false)
				.setPositiveButton(R.string.ok, positiveListener)
				.setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss())
				.show();
	}
	
	public static void showErrorDialog(Context context, @StringRes int messageId) {
		new MaterialAlertDialogBuilder(context)
				.setTitle(R.string.general_error)
				.setMessage(messageId)
				.setPositiveButton(R.string.ok, (dialogInterface, i) -> dialogInterface.dismiss()).show();
	}
	
	public static AlertDialog gettingLinkDialog(Context context) {
		LayoutInflater layoutInflater = LayoutInflater.from(context);
		@SuppressLint("InflateParams") View view = layoutInflater.inflate(R.layout.layout_dialog_getting_link, null);
		
		MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
		builder.setView(view);
		builder.setCancelable(false);
		return builder.create();
	}
	
	public static void showDialogURLNotSupported(Context context) {
		new MaterialAlertDialogBuilder(context)
				.setTitle(R.string.download_not_supported_url_title)
				.setMessage(R.string.download_not_supported_url_msg)
				.setPositiveButton(context.getString(R.string.ok), (dialogInterface, i) -> dialogInterface.dismiss())
				.create().show();
	}

	public static void showDialogURLNotSupported(Context context, DialogInterface.OnClickListener positiveListener) {
		new MaterialAlertDialogBuilder(context)
				.setTitle(R.string.download_not_supported_url_title)
				.setMessage(R.string.download_not_supported_url_msg)
				.setPositiveButton(context.getString(R.string.ok), positiveListener)
				.create().show();
	}

	public static void showNoLyrics(Context context, DialogInterface.OnClickListener positiveListener) {
		new MaterialAlertDialogBuilder(context)
				.setTitle(R.string.lyrics)
				.setMessage(R.string.no_lyrics_found)
				.setPositiveButton(context.getString(R.string.ok), positiveListener)
				.create().show();
	}

	public static void showLoginDialog(Context context, DialogInterface.OnClickListener positiveListener, DialogInterface.OnClickListener negativeListener) {
		new MaterialAlertDialogBuilder(context, R.style.Theme_MaterialComponents_Light_Dialog)
				.setTitle(R.string.special_features_title)
				.setMessage(R.string.special_features_msg)
				.setCancelable(false)
				.setPositiveButton(context.getString(R.string.ok), positiveListener)
				.setNegativeButton(context.getString(R.string.cancel), negativeListener)
				.show();
	}

	public static void showSignupDialog(Context context, DialogInterface.OnClickListener positiveListener, DialogInterface.OnClickListener negativeListener) {
		new MaterialAlertDialogBuilder(context)
				.setTitle(R.string.special_features_title)
				.setMessage(R.string.sign_up_msg)
				.setCancelable(false)
				.setPositiveButton(R.string.ok, positiveListener)
				.setNegativeButton(R.string.cancel, negativeListener)
				.show();
	}
}
