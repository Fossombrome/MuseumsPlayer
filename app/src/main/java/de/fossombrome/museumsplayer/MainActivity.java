package de.fossombrome.museumsplayer;

import android.Manifest;
import android.content.Intent;
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
    private ImageButton btnPausePlay, btnRestart, btnStop, btnInfo;

    private List<File> audioFiles = new ArrayList<>();
    private static final int REQUEST_PERMISSION = 1;
    private static final long INACTIVITY_TIMEOUT_MS = 180_000L;

    private MediaPlayer mediaPlayer;
    private File currentPlayingFile;

    private final Handler inactivityHandler = new Handler(Looper.getMainLooper());
    private final Runnable inactivityRunnable = new Runnable() {
        @Override
        public void run() {
            openExplanationScreen();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        playerControls = findViewById(R.id.playerControls);
        btnPausePlay = findViewById(R.id.btnPausePlay);
        btnRestart = findViewById(R.id.btnRestart);
        btnStop = findViewById(R.id.btnStop);
        btnInfo = findViewById(R.id.btnInfo);

        btnPausePlay.setOnClickListener(v -> togglePausePlay());
        btnRestart.setOnClickListener(v -> restartCurrentSong());
        btnStop.setOnClickListener(v -> stopCurrentSong());
        btnInfo.setOnClickListener(v -> openExplanationScreen());

        String requiredPermission = getRequiredStoragePermission();
        if (ContextCompat.checkSelfPermission(this, requiredPermission)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{requiredPermission}, REQUEST_PERMISSION);
        } else {
            loadMusicFiles();
        }

        resetInactivityTimer();
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
        }
    }

    private void restartCurrentSong() {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(0);
            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
                btnPausePlay.setImageResource(R.drawable.ic_pause);
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
    }

    private void showPlayerControls() {
        playerControls.setVisibility(View.VISIBLE);
        btnPausePlay.setImageResource(R.drawable.ic_pause);
    }

    private void hidePlayerControls() {
        playerControls.setVisibility(View.GONE);
    }

    private void resetInactivityTimer() {
        inactivityHandler.removeCallbacks(inactivityRunnable);
        inactivityHandler.postDelayed(inactivityRunnable, INACTIVITY_TIMEOUT_MS);
    }

    private void openExplanationScreen() {
        startActivity(new Intent(this, ExplanationActivity.class));
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        resetInactivityTimer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resetInactivityTimer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        inactivityHandler.removeCallbacks(inactivityRunnable);
    }
}
