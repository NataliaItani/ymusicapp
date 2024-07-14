package io.ymusic.app.player.playqueue;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import io.ymusic.app.R;

public class PlayQueueItemHolder extends RecyclerView.ViewHolder {

    public final TextView itemVideoTitleView, itemAdditionalDetailsView;
    public final ImageView itemSelected, itemThumbnailView, itemHandle;
    public final View itemRoot;

    public PlayQueueItemHolder(View rootView) {
        super(rootView);
        itemRoot = rootView.findViewById(R.id.itemRoot);
        itemVideoTitleView = rootView.findViewById(R.id.itemVideoTitleView);
        itemAdditionalDetailsView = rootView.findViewById(R.id.itemAdditionalDetails);
        itemSelected = rootView.findViewById(R.id.itemSelected);
        itemThumbnailView = rootView.findViewById(R.id.itemThumbnailView);
        itemHandle = rootView.findViewById(R.id.itemHandle);
    }
}
