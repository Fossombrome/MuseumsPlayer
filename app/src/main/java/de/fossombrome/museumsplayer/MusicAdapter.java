package de.fossombrome.museumsplayer;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicViewHolder> {

    private final Context context;
    private final List<File> musicFiles;
    private MediaPlayer mediaPlayer;
    private File currentPlayingFile;

    public MusicAdapter(Context context, List<File> musicFiles) {
        this.context = context;
        this.musicFiles = musicFiles;
    }

    @NonNull
    @Override
    public MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.music_tile, parent, false);
        return new MusicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicViewHolder holder, int position) {
        File musicFile = musicFiles.get(position);

        String songName = loadTitleFromMetadata(musicFile);
        String lyrics = loadDescriptionFromMetadata(musicFile);

        Log.d("LyricsTest", "Lyrics geladen: '" + lyrics + "'");

        holder.songTitle.setText(songName);
        holder.songDescription.setText(lyrics);

        holder.playButton.setOnClickListener(v -> playMusic(musicFile));
    }

    @Override
    public int getItemCount() {
        return musicFiles.size();
    }

    static class MusicViewHolder extends RecyclerView.ViewHolder {
        TextView songTitle, songDescription;
        ImageButton playButton;

        public MusicViewHolder(@NonNull View itemView) {
            super(itemView);
            songTitle = itemView.findViewById(R.id.songTitle);
            songDescription = itemView.findViewById(R.id.songDescription);
            playButton = itemView.findViewById(R.id.playButton);
        }
    }

    private void playMusic(File musicFile) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(musicFile.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            currentPlayingFile = musicFile;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private String loadTitleFromMetadata(File musicFile) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(musicFile.getAbsolutePath());
            String title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            return (title != null && !title.isEmpty())
                    ? title
                    : musicFile.getName().replaceAll("\\.(mp3|wav|ogg)$", "");
        } catch (Exception e) {
            e.printStackTrace();
            return musicFile.getName().replaceAll("\\.(mp3|wav|ogg)$", "");
        } finally {
            try {
                mmr.release();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String loadDescriptionFromMetadata(File musicFile) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(musicFile.getAbsolutePath());

            String composer = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER);
            String year = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR);

            StringBuilder result = new StringBuilder();

            result.append("Komponist: ")
                    .append((composer != null && !composer.isEmpty()) ? composer : "<keine Angabe>")
                    .append("\n");

            result.append("Jahr: ")
                    .append((year != null && !year.isEmpty()) ? year : "<keine Angabe>");

            return result.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "<Fehler beim Auslesen>";
        } finally {
            try {
                mmr.release();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
