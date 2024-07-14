package io.ymusic.app.info_list.holder;

import android.annotation.SuppressLint;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import io.ymusic.app.App;
import io.ymusic.app.R;
import io.ymusic.app.info_list.InfoItemBuilder;
import io.ymusic.app.util.GlideUtils;

public class StreamMiniInfoItemHolder extends InfoItemHolder {
	
	public final ImageView itemThumbnailView;
	public final TextView itemVideoTitleView;
	public final TextView itemUploaderView;
	public final ImageView itemDownloadView;
	
	StreamMiniInfoItemHolder(InfoItemBuilder infoItemBuilder, int layoutId, ViewGroup parent) {
		super(infoItemBuilder, layoutId, parent);
		
		itemThumbnailView = itemView.findViewById(R.id.itemThumbnailView);
		itemVideoTitleView = itemView.findViewById(R.id.itemVideoTitleView);
		itemUploaderView = itemView.findViewById(R.id.itemUploaderView);
		itemDownloadView = itemView.findViewById(R.id.itemDownloadView);
	}
	
	public StreamMiniInfoItemHolder(InfoItemBuilder infoItemBuilder, ViewGroup parent) {
		this(infoItemBuilder, R.layout.list_stream_item, parent);
	}
	
	@SuppressLint("CheckResult")
	@Override
	public void updateFromItem(final InfoItem infoItem) {
		if (!(infoItem instanceof StreamInfoItem)) return;
		final StreamInfoItem item = (StreamInfoItem) infoItem;
		
		itemVideoTitleView.setText(item.getName());
		itemUploaderView.setText(item.getUploaderName());
		// Default thumbnail is shown on error, while loading and if the url is empty
		GlideUtils.loadThumbnail(App.applicationContext, itemThumbnailView, item.getThumbnailUrl());
		
		itemView.setOnClickListener(view -> {
			if (itemBuilder.getOnStreamSelectedListener() != null) {
				itemBuilder.getOnStreamSelectedListener().selected(item);
			}
		});
		
		itemDownloadView.setOnClickListener(view -> {
			if (itemBuilder.getOnStreamSelectedListener() != null) {
				itemBuilder.getOnStreamSelectedListener().download(item);
			}
		});
	}
}
