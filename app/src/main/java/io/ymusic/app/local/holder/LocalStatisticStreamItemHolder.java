package io.ymusic.app.local.holder;

import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;

import io.ymusic.app.App;
import io.ymusic.app.R;
import io.ymusic.app.database.LocalItem;
import io.ymusic.app.database.stream.StreamStatisticsEntry;
import io.ymusic.app.local.LocalItemBuilder;
import io.ymusic.app.util.GlideUtils;

public class LocalStatisticStreamItemHolder extends LocalItemHolder {

    public final ImageView itemThumbnailView;
    public final TextView itemVideoTitleView;
    public final TextView itemUploaderView;
    public final ImageView itemDeleteView;

    public LocalStatisticStreamItemHolder(LocalItemBuilder infoItemBuilder, ViewGroup parent) {
        super(infoItemBuilder, R.layout.list_download_item, parent);
        itemThumbnailView = itemView.findViewById(R.id.itemThumbnailView);
        itemVideoTitleView = itemView.findViewById(R.id.itemVideoTitleView);
        itemUploaderView = itemView.findViewById(R.id.itemUploaderView);
        itemDeleteView = itemView.findViewById(R.id.itemDeleteView);
    }

    @Override
    public void updateFromItem(final LocalItem localItem, final DateFormat dateFormat) {
        if (!(localItem instanceof StreamStatisticsEntry)) return;
        final StreamStatisticsEntry item = (StreamStatisticsEntry) localItem;

        itemVideoTitleView.setText(item.title);
        itemUploaderView.setText(item.uploader);
        GlideUtils.loadThumbnail(App.applicationContext, itemThumbnailView, item.thumbnailUrl);

        itemView.setOnClickListener(view -> {
            if (itemBuilder.getOnItemSelectedListener() != null) {
                itemBuilder.getOnItemSelectedListener().selected(item);
            }
        });

        itemDeleteView.setOnClickListener(view -> {
            if (itemBuilder.getOnItemSelectedListener() != null) {
                itemBuilder.getOnItemSelectedListener().delete(item);
            }
        });
    }
}
