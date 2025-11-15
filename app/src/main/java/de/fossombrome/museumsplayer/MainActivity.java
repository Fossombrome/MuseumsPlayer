package de.fossombrome.museumsplayer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MusicAdapter.OnMusicPlayListener {

    private RecyclerView recyclerView;
    private LinearLayout playerControls;
    private Button btnPausePlay, btnRestart, btnStop;
    private ProgressBar songProgress;

    private List<File> audioFiles = new ArrayList<>();
    private static final int REQUEST_PERMISSION = 1;

    private MediaPlayer mediaPlayer;
    private File currentPlayingFile;

    private Handler progressHandler = new Handler();
    private Runnable progressRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        playerControls = findViewById(R.id.playerControls);
        btnPausePlay = findViewById(R.id.btnPausePlay);
        btnRestart = findViewById(R.id.btnRestart);
        btnStop = findViewById(R.id.btnStop);
        songProgress = findViewById(R.id.songProgress);

        btnPausePlay.setOnClickListener(v -> togglePausePlay());
        btnRestart.setOnClickListener(v -> restartCurrentSong());
        btnStop.setOnClickListener(v -> stopCurrentSong());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
        } else {
            loadMusicFiles();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    private void loadMusicFiles() {
        File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        if (musicDir.exists() && musicDir.isDirectory()) {
            File[] files = musicDir.listFiles((dir, name) ->
                    name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".ogg"));

            if (files != null) {
                for (File file : files) {
                    audioFiles.add(file);
                }
            }
        }

        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setAdapter(new MusicAdapter(this, audioFiles, this));
    }

    @Override
    public void onMusicPlay(File file) {
        playSong(file);
    }

    public void playSong(File musicFile) {
        stopCurrentSong();

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(musicFile.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();

            currentPlayingFile = musicFile;
            showPlayerControls();
            startProgressUpdater();

            mediaPlayer.setOnCompletionListener(mp -> stopCurrentSong());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void togglePausePlay() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                btnPausePlay.setText("▶️");
            } else {
                mediaPlayer.start();
                btnPausePlay.setText("⏸️");
            }
        }
    }

    private void restartCurrentSong() {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(0);
            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
                btnPausePlay.setText("⏸️");
            }
        }
    }

    private void stopCurrentSong() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        currentPlayingFile = null;
        hidePlayerControls();
        stopProgressUpdater();
    }

    private void showPlayerControls() {
        playerControls.setVisibility(View.VISIBLE);
        btnPausePlay.setText("⏸️");
    }

    private void hidePlayerControls() {
        playerControls.setVisibility(View.GONE);
        songProgress.setProgress(0);
    }

    private void startProgressUpdater() {
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    int current = mediaPlayer.getCurrentPosition();
                    int total = mediaPlayer.getDuration();
                    if (total > 0) {
                        int progress = (int) ((current / (float) total) * 100);
                        songProgress.setProgress(progress);
                    }
                    progressHandler.postDelayed(this, 500);
                }
            }
        };
        progressHandler.post(progressRunnable);
    }

    private void stopProgressUpdater() {
        progressHandler.removeCallbacks(progressRunnable);
        songProgress.setProgress(0);
    }
}
