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

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // muestra back arrow
        }

        // Referencias
        btnComenzar = findViewById(R.id.btnComenzar);
        btnAyuda = findViewById(R.id.btnAyuda);

        btnComenzar.bringToFront();
        btnComenzar.setClickable(true);
        btnComenzar.setFocusable(true);

        // Listener botón comenzar (con toast de debug)
        btnComenzar.setOnClickListener(v -> {
            Toast.makeText(SplashActivity.this, "Vamos!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        });

        // Listener botón ayuda
        btnAyuda.setOnClickListener(v ->
                startActivity(new Intent(SplashActivity.this, HelpActivity.class))
        );

        Button cmdExit = findViewById(R.id.cmdExit);
        cmdExit.setOnClickListener(v -> {
            finishAffinity();     // Cierra todas las actividades
            System.exit(0);       // Mata el proceso (opcional, pero efectivo)
        });
    }
}
