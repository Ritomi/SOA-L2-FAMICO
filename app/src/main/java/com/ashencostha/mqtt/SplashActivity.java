package com.ashencostha.mqtt;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private View btnComenzar;     // View invisible sobre la imagen
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

        // Asegurarnos de que el botón esté visualmente por encima (Z)
        // esto ayuda cuando overlays o ImageView interfieren con toques
        btnComenzar.bringToFront();
        btnComenzar.setClickable(true);
        btnComenzar.setFocusable(true);

        // Listener botón comenzar (con toast de debug)
        btnComenzar.setOnClickListener(v -> {
            Toast.makeText(SplashActivity.this, "Comenzar pulsado", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        });

        // Listener botón ayuda
        btnAyuda.setOnClickListener(v ->
                startActivity(new Intent(SplashActivity.this, HelpActivity.class))
        );
    }
}
