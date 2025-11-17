package com.ashencostha.mqtt;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private View btnComenzar;     // <-- Este NO es un Button, es un View invisible
    private Button btnAyuda;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Ocultar action bar si existe
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Referencias
        btnComenzar = findViewById(R.id.btnComenzar);
        btnAyuda = findViewById(R.id.btnAyuda);

        // Listener botón comenzar
        btnComenzar.setOnClickListener(v -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        });

        // Listener botón ayuda
        btnAyuda.setOnClickListener(v ->
                startActivity(new Intent(SplashActivity.this, HelpActivity.class))
        );
    }
}
