package io.ymusic.app.fragments.songs.adapters;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.afollestad.materialcab.MaterialCab;
import com.bumptech.glide.Glide;
import com.kabouzeid.appthemehelper.util.ColorUtil;
import com.kabouzeid.appthemehelper.util.MaterialValueHelper;

import java.util.List;

import io.ymusic.app.R;
import io.ymusic.app.fragments.songs.SongsFragment.SONG_TYPE;
import io.ymusic.app.local_player.base.AbsMultiSelectAdapter;
import io.ymusic.app.local_player.base.MediaEntryViewHolder;
import io.ymusic.app.local_player.interfaces.CabHolder;
import io.ymusic.app.local_player.model.Song;
import io.ymusic.app.util.NavigationHelper;
import io.ymusic.app.util.glide.PhonographColoredTarget;
import io.ymusic.app.util.glide.SongGlideRequest;

public class ArtistAdapter extends AbsMultiSelectAdapter<ArtistAdapter.ViewHolder, Song> implements MaterialCab.Callback {

    protected final AppCompatActivity activity;
    protected List<Song> dataSet;
    protected int itemLayoutRes;

    // this is to differentiate the AlbumFragment from the ArtistFragment
    protected SONG_TYPE type;

    public ArtistAdapter(AppCompatActivity activity, List<Song> dataSet, @LayoutRes int itemLayoutRes, SONG_TYPE type, @Nullable CabHolder cabHolder) {
        super(activity, cabHolder, R.menu.menu_media_selection);
        this.activity = activity;
        this.dataSet = dataSet;
        this.itemLayoutRes = itemLayoutRes;
        this.type = type;
        setHasStableIds(true);
    }

    public void swapDataSet(List<Song> dataSet) {
        this.dataSet = dataSet;
        notifyDataSetChanged();
    }

    public List<Song> getDataSet() {
        return dataSet;
    }

    @Override
    public long getItemId(int position) {
        return dataSet.get(position).id;
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(activity).inflate(itemLayoutRes, parent, false);
        return createViewHolder(view);
    }

    protected ViewHolder createViewHolder(View view) {
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final Song song = dataSet.get(position);

        boolean isChecked = isChecked(song);
        holder.itemView.setActivated(isChecked);

        if (holder.title != null) {
            holder.title.setText(getSongTitle(song));
        }
        if (holder.text != null) {
            holder.text.setText(getSongText(song));
        }

        loadAlbumCover(song, holder);
    }

    private void setColors(int color, ViewHolder holder) {
        if (holder.title != null) {
            holder.title.setTextColor(MaterialValueHelper.getPrimaryTextColor(activity, ColorUtil.isColorLight(color)));
        }
        if (holder.text != null) {
            holder.text.setTextColor(MaterialValueHelper.getSecondaryTextColor(activity, ColorUtil.isColorLight(color)));
        }
    }

    protected void loadAlbumCover(Song song, final ViewHolder holder) {
        if (holder.image == null) return;

        SongGlideRequest.Builder.from(Glide.with(activity), song)
                .generatePalette(activity).build()
                .into(new PhonographColoredTarget(holder.image) {
                    @Override
                    public void onLoadCleared(Drawable placeholder) {
                        super.onLoadCleared(placeholder);
                        setColors(getDefaultFooterColor(), holder);
                    }

                    @Override
                    public void onColorReady(int color) {
                        setColors(getDefaultFooterColor(), holder);
                    }
                });
    }

    protected String getSongTitle(Song song) {
        if (type == SONG_TYPE.ARTIST) {    // Artist Fragment
            return song.artistName;
        } else  {  // Album Fragment
            return song.albumName;
        }

    }

    protected String getSongText(Song song) {
        if (type == SONG_TYPE.ARTIST) {
            return song.trackNumber+ (song.trackNumber > 1 ? " songs" : " song");
        } else {
            return song.artistName;
        }
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    @Override
    protected Song getIdentifier(int position) {
        return dataSet.get(position);
    }

    @Override
    protected String getName(Song song) {
        return song.title;
    }

    @Override
    protected void onMultipleItemAction(@NonNull MenuItem menuItem, @NonNull List<Song> selection) {

    }

    public class ViewHolder extends MediaEntryViewHolder {

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            setImageTransitionName(activity.getString(R.string.transition_album_art));
        }

        @Override
        public void onClick(View v) {
            // Open the artist songs
            Song song = dataSet.get(getBindingAdapterPosition());
            Log.d("TAG", " id: "+song.artistId+" aId: "+song.albumId);
            if (type == SONG_TYPE.ARTIST) {    // Artist Fragment
                NavigationHelper.openSongs(activity.getSupportFragmentManager(), type, song.artistId,  song.artistName);
            } else {
                NavigationHelper.openSongs(activity.getSupportFragmentManager(), type, song.albumId,  song.albumName);
            }
        }
    }
}
