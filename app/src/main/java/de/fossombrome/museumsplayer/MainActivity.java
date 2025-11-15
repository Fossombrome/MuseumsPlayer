package de.fossombrome.museumsplayer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
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
    private ImageButton btnPausePlay, btnRestart, btnStop;
    private SeekBar songProgress;

    private List<File> audioFiles = new ArrayList<>();
    private static final int REQUEST_PERMISSION = 1;

    private MediaPlayer mediaPlayer;
    private File currentPlayingFile;

    private Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;
    private boolean userSeeking = false;

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
        songProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                userSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                userSeeking = false;
                if (mediaPlayer != null) {
                    seekTo(seekBar.getProgress());
                    refreshSongProgress();
                }
            }
        });

        btnPausePlay.setOnClickListener(v -> togglePausePlay());
        btnRestart.setOnClickListener(v -> restartCurrentSong());
        btnStop.setOnClickListener(v -> stopCurrentSong());

        String requiredPermission = getRequiredStoragePermission();
        if (ContextCompat.checkSelfPermission(this, requiredPermission)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{requiredPermission}, REQUEST_PERMISSION);
        } else {
            loadMusicFiles();
        }
    }

    private String getRequiredStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Manifest.permission.READ_MEDIA_AUDIO;
        }
        return Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadMusicFiles();
            }
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
            songProgress.setProgress(0);
            songProgress.setMax(mediaPlayer.getDuration());
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
                btnPausePlay.setImageResource(R.drawable.ic_play);
            } else {
                mediaPlayer.start();
                btnPausePlay.setImageResource(R.drawable.ic_pause);
            }
            refreshSongProgress();
        }
    }

    private void restartCurrentSong() {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(0);
            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
                btnPausePlay.setImageResource(R.drawable.ic_pause);
            }
            refreshSongProgress();
        }
    }

    private void stopCurrentSong() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        currentPlayingFile = null;
        userSeeking = false;
        hidePlayerControls();
        stopProgressUpdater();
    }

    private void showPlayerControls() {
        playerControls.setVisibility(View.VISIBLE);
        btnPausePlay.setImageResource(R.drawable.ic_pause);
    }

    private void hidePlayerControls() {
        playerControls.setVisibility(View.GONE);
        songProgress.setProgress(0);
        songProgress.setMax(0);
    }

    private void startProgressUpdater() {
        stopProgressUpdater();
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null) {
                    refreshSongProgress();
                    progressHandler.postDelayed(this, 500);
                }
            }
        };
        progressHandler.post(progressRunnable);
    }

    private void stopProgressUpdater() {
        if (progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
        }
        songProgress.setProgress(0);
        songProgress.setMax(0);
    }

    private void refreshSongProgress() {
        if (mediaPlayer == null) {
            songProgress.setProgress(0);
            return;
        }

        int total = mediaPlayer.getDuration();
        if (total > 0) {
            int current = mediaPlayer.getCurrentPosition();
            if (!userSeeking) {
                songProgress.setMax(total);
                songProgress.setProgress(current);
            }
        } else {
            songProgress.setProgress(0);
        }
    }

    private void seekTo(int progress) {
        if (mediaPlayer != null) {
            boolean wasPlaying = mediaPlayer.isPlaying();
            mediaPlayer.seekTo(progress);
            if (wasPlaying) {
                mediaPlayer.start();
                btnPausePlay.setImageResource(R.drawable.ic_pause);
            }
        }
    }
}
