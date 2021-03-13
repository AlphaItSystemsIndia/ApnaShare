package com.cod3rboy.apnashare.activities;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cod3rboy.apnashare.R;

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Set status bar color
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorAccent));
        }else{
            getWindow().setStatusBarColor(getColor(R.color.white));
        }
        // Set Navigation bar color
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1){
            getWindow().setNavigationBarColor(getResources().getColor(R.color.colorAccent));
        }else{
            getWindow().setNavigationBarColor(getColor(R.color.primarySurfaceColor));
        }
        super.onCreate(savedInstanceState);
    }
}
