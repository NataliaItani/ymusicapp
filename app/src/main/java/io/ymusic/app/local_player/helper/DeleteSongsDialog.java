package io.ymusic.app.local_player.helper;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Html;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import io.ymusic.app.R;
import io.ymusic.app.local_player.model.Song;
import io.ymusic.app.util.MusicUtil;

public class DeleteSongsDialog extends DialogFragment {

    @NonNull
    public static DeleteSongsDialog create(Song song) {
        List<Song> list = new ArrayList<>();
        list.add(song);
        return create(list);
    }

    @NonNull
    public static DeleteSongsDialog create(List<Song> songs) {
        DeleteSongsDialog dialog = new DeleteSongsDialog();
        Bundle args = new Bundle();
        args.putParcelableArrayList("songs", new ArrayList<>(songs));
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final List<Song> songs = getArguments().getParcelableArrayList("songs");
        CharSequence content = Html.fromHtml(getString(R.string.delete_song_x, songs.get(0).title));
        return new MaterialAlertDialogBuilder(getActivity())
                .setTitle(R.string.delete_song_title)
                .setMessage(content)
                .setPositiveButton(R.string.delete_action, (dialog, which) -> MusicUtil.deleteTracks(getActivity(), songs))
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .create();
    }
}
