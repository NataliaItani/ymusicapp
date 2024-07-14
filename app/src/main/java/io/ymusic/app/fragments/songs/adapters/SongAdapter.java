package io.ymusic.app.fragments.songs.adapters;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.afollestad.materialcab.MaterialCab;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.kabouzeid.appthemehelper.util.ColorUtil;
import com.kabouzeid.appthemehelper.util.MaterialValueHelper;

import java.util.ArrayList;
import java.util.List;

import io.ymusic.app.R;
import io.ymusic.app.activities.MainActivity;
import io.ymusic.app.fragments.songs.SongsFragment.SONG_TYPE;
import io.ymusic.app.local_player.base.AbsMultiSelectAdapter;
import io.ymusic.app.local_player.base.MediaEntryViewHolder;
import io.ymusic.app.local_player.helper.MusicPlayerRemote;
import io.ymusic.app.local_player.helper.PlaylistSongHelper;
import io.ymusic.app.local_player.helper.SongMenuHelper;
import io.ymusic.app.local_player.interfaces.CabHolder;
import io.ymusic.app.local_player.model.Song;
import io.ymusic.app.util.MusicUtil;
import io.ymusic.app.util.NavigationHelper;
import io.ymusic.app.util.glide.PhonographColoredTarget;
import io.ymusic.app.util.glide.SongGlideRequest;

public class SongAdapter extends AbsMultiSelectAdapter<SongAdapter.ViewHolder, Song> implements MaterialCab.Callback, Filterable {

    protected final AppCompatActivity activity;
    protected List<Song> dataSet;
    protected List<Song> dataSetFiltered;
    protected int itemLayoutRes;

    private int playlistId;
    private SONG_TYPE type;

    public SongAdapter(AppCompatActivity activity, List<Song> dataSet, @LayoutRes int itemLayoutRes, @Nullable CabHolder cabHolder) {
        super(activity, cabHolder, R.menu.menu_media_selection);
        this.activity = activity;
        this.dataSet = dataSet;
        this.dataSetFiltered = dataSet;
        this.itemLayoutRes = itemLayoutRes;
        setHasStableIds(true);
    }

    public void swapDataSet(List<Song> dataSet) {
        this.dataSet = dataSet;
        this.dataSetFiltered = dataSet;
        notifyDataSetChanged();
    }

    public void setType(SONG_TYPE type) {
        this.type = type;
    }

    public void setPlaylistId(int playlistId) {
        this.playlistId = playlistId;
    }


    public List<Song> getDataSet() {
        return dataSetFiltered;
    }

    @Override
    public long getItemId(int position) {
        return dataSetFiltered.get(position).id;
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
        final Song song = dataSetFiltered.get(position);

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
        return song.title;
    }

    protected String getSongText(Song song) {
        return MusicUtil.getSongInfoString(song);
    }

    @Override
    public int getItemCount() {
        return dataSetFiltered.size();
    }

    @Override
    protected Song getIdentifier(int position) {
        return dataSetFiltered.get(position);
    }

    @Override
    protected String getName(Song song) {
        return song.title;
    }

    @Override
    protected void onMultipleItemAction(@NonNull MenuItem menuItem, @NonNull List<Song> selection) {

    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                if (charString.isEmpty() || TextUtils.isEmpty(charString)) {
                    dataSetFiltered = dataSet;
                } else {
                    List<Song> filteredList = new ArrayList<>();
                    for (Song song : dataSet) {
                        if (song.title.toLowerCase().contains(charString.toLowerCase())
                                || song.artistName.toLowerCase().contains(charString.toLowerCase())
                                || song.albumName.toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(song);
                        }
                    }

                    dataSetFiltered = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = dataSetFiltered;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                dataSetFiltered = (ArrayList<Song>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    public class ViewHolder extends MediaEntryViewHolder {
        protected int DEFAULT_MENU_RES = SongMenuHelper.MENU_RES;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            setImageTransitionName(activity.getString(R.string.transition_album_art));

            if (menu == null) {
                return;
            }
            menu.setVisibility(View.VISIBLE);
            menu.setOnClickListener(new SongMenuHelper.OnClickSongMenu(activity) {
                @Override
                public Song getSong() {
                    return ViewHolder.this.getSong();
                }

                @Override
                public SONG_TYPE getType() {
                    return type;
                }

                @Override
                public int getMenuRes() {
                    return getSongMenuRes();
                }

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return onSongMenuItemClick(item) || super.onMenuItemClick(item);
                }
            });
        }

        protected Song getSong() {
            return dataSetFiltered.get(getBindingAdapterPosition());
        }

        protected int getSongMenuRes() {
            return DEFAULT_MENU_RES;
        }

        protected boolean onSongMenuItemClick(MenuItem item) {
            if(item.getItemId() == R.id.action_add_playlist) {
                if (type==SONG_TYPE.PLAYLIST) {
                    PlaylistSongHelper.remove(activity, playlistId, getSong(), new PlaylistSongHelper.OnRemove() {
                        @Override
                        protected void execute() {
                            // Remove playlist song
                            dataSetFiltered.remove(getBindingAdapterPosition());
                            notifyItemRemoved(getBindingAdapterPosition());
                        }
                    });
                    return true;
                }
                PlaylistSongHelper.add(activity, getSong());
                return true;
            }
            return false;
        }

        @Override
        public void onClick(View v) {
            // stop remote player
            final FrameLayout bottomSheetLayout = activity.findViewById(R.id.fragment_player_holder);
            final BottomSheetBehavior<FrameLayout> bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            ((MainActivity) activity).setBottomNavigationVisibility(View.VISIBLE);

            // start local player
            MusicPlayerRemote.openQueue(getDataSet(), getBindingAdapterPosition(), true);

            if (type == SONG_TYPE.SEARCH) {
                NavigationHelper.goBack(activity.getSupportFragmentManager());
            }
        }
    }
}
