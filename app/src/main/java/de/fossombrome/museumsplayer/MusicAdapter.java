package de.fossombrome.museumsplayer;

import android.content.Context;
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
        String songName = musicFile.getName().replaceAll("\\.(mp3|wav|ogg)$", "");
        String description = loadMarkdownDescription(musicFile);

        holder.songTitle.setText(songName);
        holder.songDescription.setText(description);

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

    private String loadMarkdownDescription(File musicFile) {
        String mdFilePath = musicFile.getAbsolutePath().replaceAll("\\.(mp3|wav|ogg)$", ".md");
        File mdFile = new File(mdFilePath);
        if (mdFile.exists()) {
            try {
                return new String(Files.readAllBytes(Paths.get(mdFilePath)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "<leer>";
    }
}
