package io.ymusic.app.info_list.holder;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.channel.ChannelInfoItem;

import de.hdodenhof.circleimageview.CircleImageView;
import io.ymusic.app.App;
import io.ymusic.app.R;
import io.ymusic.app.info_list.InfoItemBuilder;
import io.ymusic.app.util.GlideUtils;
import io.ymusic.app.util.Localization;
import io.ymusic.app.util.views.SwipeRevealLayout;

public class ChannelMiniInfoItemHolder extends InfoItemHolder {

    public final CircleImageView itemThumbnailView;
    public final TextView itemTitleView;
    public final TextView itemAdditionalDetailView;
    public final TextView itemChannelDescriptionView;
    public final View itemRoot;
    public final SwipeRevealLayout swipeLayout;
    public final View unsubscribeLayout;

    ChannelMiniInfoItemHolder(InfoItemBuilder infoItemBuilder, int layoutId, ViewGroup parent) {
        super(infoItemBuilder, layoutId, parent);

        itemThumbnailView = itemView.findViewById(R.id.itemThumbnailView);
        itemTitleView = itemView.findViewById(R.id.itemTitleView);
        itemAdditionalDetailView = itemView.findViewById(R.id.itemAdditionalDetails);
        itemChannelDescriptionView = itemView.findViewById(R.id.itemChannelDescriptionView);
        itemRoot = itemView.findViewById(R.id.itemRoot);
        swipeLayout = itemView.findViewById(R.id.swipe_layout);
        unsubscribeLayout = itemView.findViewById(R.id.unsubscribe_layout);
    }

    public ChannelMiniInfoItemHolder(InfoItemBuilder infoItemBuilder, ViewGroup parent) {
        this(infoItemBuilder, R.layout.list_channel_mini_item, parent);
    }

    @Override
    public void updateFromItem(final InfoItem infoItem) {

        if (!(infoItem instanceof ChannelInfoItem)) return;
        final ChannelInfoItem item = (ChannelInfoItem) infoItem;

        itemTitleView.setText(item.getName());
        itemAdditionalDetailView.setText(getDetailLine(item));
        itemChannelDescriptionView.setText(item.getDescription());
        GlideUtils.loadThumbnail(App.getAppContext(), itemThumbnailView, item.getThumbnailUrl());

        itemRoot.setOnClickListener(view -> {
            if (itemBuilder.getOnChannelSelectedListener() != null) {
                itemBuilder.getOnChannelSelectedListener().selected(item);
            }
        });

        unsubscribeLayout.setOnClickListener(view -> {
            if (itemBuilder.getOnChannelSelectedListener() != null) {
                itemBuilder.getOnChannelSelectedListener().swipe(item);
            }
        });
    }

    protected String getDetailLine(final ChannelInfoItem item) {
        String details = "";
        if (item.getSubscriberCount() >= 0) {
            details += Localization.shortSubscriberCount(itemBuilder.getContext(), item.getSubscriberCount());
        }
        return details;
    }
}
