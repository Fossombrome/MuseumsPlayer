package de.fossombrome.museumsplayer;

import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class ExplanationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explanation);

        Button backButton = findViewById(R.id.btnBackToMain);
        backButton.setOnClickListener(v -> finish());
    }
}
