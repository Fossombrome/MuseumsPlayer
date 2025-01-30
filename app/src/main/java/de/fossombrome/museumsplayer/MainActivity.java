package de.fossombrome.museumsplayer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private Button btnPausePlay;
    private List<File> audioFiles = new ArrayList<>();
    private static final int REQUEST_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        btnPausePlay = findViewById(R.id.btnPausePlay);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
        } else {
            loadMusicFiles();
        }

        btnPausePlay.setOnClickListener(v -> togglePausePlay());
    }

    private void loadMusicFiles() {
        File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        if (musicDir.exists() && musicDir.isDirectory()) {
            File[] files = musicDir.listFiles((dir, name) -> name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".ogg"));

            if (files != null) {
                for (File file : files) {
                    audioFiles.add(file);
                }
            }
        }

        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setAdapter(new MusicAdapter(this, audioFiles));
    }

    private void togglePausePlay() {
        // Implementiere Pause/Play für aktuell laufendes Lied
    }
}