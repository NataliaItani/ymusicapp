package io.ymusic.app.info_list.holder;

import android.view.ViewGroup;

import org.schabi.newpipe.extractor.InfoItem;

import io.ymusic.app.R;
import io.ymusic.app.info_list.InfoItemBuilder;

public class StreamInfoItemHolder extends StreamMiniInfoItemHolder {
	
	public StreamInfoItemHolder(InfoItemBuilder infoItemBuilder, ViewGroup parent) {
		super(infoItemBuilder, R.layout.list_stream_item, parent);
	}
	
	@Override
	public void updateFromItem(final InfoItem infoItem) {
		super.updateFromItem(infoItem);
	}
}
