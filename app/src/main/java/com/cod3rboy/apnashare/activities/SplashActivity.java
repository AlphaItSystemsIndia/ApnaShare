package com.cod3rboy.apnashare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private static final long SPLASH_TIME_MILLIS = 800;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new Handler().postDelayed(() -> {
            Intent i = new Intent(this, MainActivity.class);
            startActivity(i);
            finish();
        }, SPLASH_TIME_MILLIS);
    }
}
